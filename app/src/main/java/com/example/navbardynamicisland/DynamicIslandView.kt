package com.example.navbardynamicisland

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import de.robv.android.xposed.XposedBridge

class DynamicIslandView(
    context: Context,
    private val navigationHandle: View
) : FrameLayout(context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val collapseRunnable = Runnable { collapseToIdle() }

    // Child Views
    private lateinit var mainPill: FrameLayout
    private lateinit var contentLayout: LinearLayout
    private lateinit var iconView: ImageView
    private lateinit var textLayout: LinearLayout
    private lateinit var titleText: TextView
    private lateinit var subText: TextView
    private lateinit var sliderBar: ProgressBar

    // Layout configuration values
    private val dp = context.resources.displayMetrics.density
    private var isVisible = false

    private enum class IslandState { IDLE, BATTERY, MEDIA, VOLUME, BRIGHTNESS }
    private var currentState = IslandState.IDLE

    // Active media state caching
    private var currentSongTitle = ""
    private var currentArtistName = ""
    private var currentAppName = ""
    private var isMusicPlaying = false

    init {
        setupLayout()
    }

    private fun setupLayout() {
        // Main rounded container (the Island capsule)
        mainPill = FrameLayout(context).apply {
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#E0121212")) // Glossy dark background
                setStroke((1.5f * dp).toInt(), Color.parseColor("#25FFFFFF")) // Subtle glowing outline
                cornerRadius = 10f * dp
            }
            background = bg
        }

        // Horizontal layout inside the container
        contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((12 * dp).toInt(), 0, (12 * dp).toInt(), 0)
        }

        // Left Icon View
        iconView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams((24 * dp).toInt(), (24 * dp).toInt()).apply {
                marginEnd = (10 * dp).toInt()
            }
            visibility = View.GONE
        }

        // Vertical Text stack (Title and Subtitle)
        textLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
            visibility = View.GONE
        }

        titleText = TextView(context).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            maxLines = 1
        }

        subText = TextView(context).apply {
            setTextColor(Color.parseColor("#B3FFFFFF")) // 70% opacity white
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            maxLines = 1
            visibility = View.GONE
        }

        textLayout.addView(titleText)
        textLayout.addView(subText)

        // Progress Slider for Volume/Brightness levels
        sliderBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams((120 * dp).toInt(), (4 * dp).toInt()).apply {
                marginStart = (10 * dp).toInt()
            }
            visibility = View.GONE
            max = 100
        }

        contentLayout.addView(iconView)
        contentLayout.addView(textLayout)
        contentLayout.addView(sliderBar)

        mainPill.addView(contentLayout)
        addView(mainPill)

        // Set up custom gestures
        setupTouchListener()
        
        // Start in collapsed state
        setIdleDimensions()
    }

    private fun setupTouchListener() {
        mainPill.setOnTouchListener(object : OnTouchListener {
            private var startX = 0f
            private var startY = 0f
            private var startTime = 0L

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.x
                        startY = event.y
                        startTime = System.currentTimeMillis()
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val duration = System.currentTimeMillis() - startTime
                        val deltaY = event.y - startY
                        
                        if (duration < 300 && Math.abs(event.x - startX) < 15 && Math.abs(deltaY) < 15) {
                            // Double Tap or Single Tap detection
                            handlePillTap()
                        } else if (deltaY < -40) {
                            // Swipe up
                            expandDynamicIsland()
                        } else if (deltaY > 40) {
                            // Swipe down
                            collapseToIdle()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun handlePillTap() {
        if (currentState == IslandState.IDLE && isMusicPlaying) {
            // Expand to show media player controller
            showMediaExpanded()
        } else {
            // Otherwise collapse
            collapseToIdle()
        }
    }

    private fun setIdleDimensions() {
        val params = mainPill.layoutParams as LayoutParams
        params.width = (140 * dp).toInt()  // Wrap system gesture bar width
        params.height = (12 * dp).toInt()  // Wrap system gesture bar height
        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        
        val bg = mainPill.background as GradientDrawable
        bg.cornerRadius = 6f * dp
        
        iconView.visibility = View.GONE
        textLayout.visibility = View.GONE
        sliderBar.visibility = View.GONE
        
        mainPill.layoutParams = params
    }

    // Floating Window Mount Settings
    fun showOverlay() {
        if (isVisible) return
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // Overlay above active app layouts
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (16 * dp).toInt() // Positions slightly above the screen bottom edge
        }

        try {
            windowManager.addView(this, params)
            isVisible = true
            XposedBridge.log("[NavbarDynamicIsland] Overlay attached to WindowManager.")
        } catch (e: Exception) {
            XposedBridge.log("[NavbarDynamicIsland] WindowManager attach failed: " + e.message)
        }
    }

    fun removeOverlay() {
        if (!isVisible) return
        try {
            windowManager.removeView(this)
            isVisible = false
        } catch (e: Exception) {
            // Ignored
        }
    }

    // Smooth Spring transition animator
    private fun animatePillSize(targetWidth: Int, targetHeight: Int, targetRadius: Float, onEnd: (() -> Unit)? = null) {
        val startWidth = mainPill.width
        val startHeight = mainPill.height

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 320
            interpolator = OvershootInterpolator(0.75f)
        }

        animator.addUpdateListener { valAnim ->
            val fraction = valAnim.animatedValue as Float
            val currentWidth = (startWidth + (targetWidth - startWidth) * fraction).toInt()
            val currentHeight = (startHeight + (targetHeight - startHeight) * fraction).toInt()

            mainPill.layoutParams.width = currentWidth
            mainPill.layoutParams.height = currentHeight

            val bg = mainPill.background as GradientDrawable
            bg.cornerRadius = targetRadius

            mainPill.requestLayout()
        }

        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd?.invoke()
            }
        })
        animator.start()
    }

    private fun resetAutoCollapseTimer(delayMs: Long = 3000L) {
        mainHandler.removeCallbacks(collapseRunnable)
        mainHandler.postDelayed(collapseRunnable, delayMs)
    }

    private fun expandDynamicIsland() {
        if (currentState == IslandState.IDLE) return
        // Grows upward to show details
        animatePillSize((300 * dp).toInt(), (64 * dp).toInt(), 16f * dp) {
            iconView.visibility = View.VISIBLE
            textLayout.visibility = View.VISIBLE
            if (currentState == IslandState.VOLUME || currentState == IslandState.BRIGHTNESS) {
                sliderBar.visibility = View.VISIBLE
            }
        }
        resetAutoCollapseTimer(5000L)
    }

    fun collapseToIdle() {
        mainHandler.removeCallbacks(collapseRunnable)
        if (currentState == IslandState.IDLE) return

        iconView.visibility = View.GONE
        textLayout.visibility = View.GONE
        sliderBar.visibility = View.GONE

        currentState = IslandState.IDLE
        animatePillSize((140 * dp).toInt(), (12 * dp).toInt(), 6f * dp) {
            // If music is still playing, keep checking it
            if (isMusicPlaying) {
                resetAutoCollapseTimer(3000L)
            }
        }
    }

    // --- State update receivers ---

    fun updateBatteryState(pct: Int, isCharging: Boolean) {
        if (!isCharging) {
            if (currentState == IslandState.BATTERY) collapseToIdle()
            return
        }

        currentState = IslandState.BATTERY
        titleText.text = "Charging $pct%"
        subText.text = "Fast Charging"
        subText.visibility = View.VISIBLE
        iconView.setImageResource(android.R.drawable.ic_lock_idle_charging)
        
        // Show expanding charging capsule
        iconView.visibility = View.VISIBLE
        textLayout.visibility = View.VISIBLE
        sliderBar.visibility = View.GONE
        
        animatePillSize((200 * dp).toInt(), (48 * dp).toInt(), 12f * dp)
        resetAutoCollapseTimer(3500L)
    }

    fun updateVolumeState(pct: Int) {
        currentState = IslandState.VOLUME
        titleText.text = "Volume"
        subText.visibility = View.GONE
        iconView.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
        sliderBar.progress = pct

        iconView.visibility = View.VISIBLE
        textLayout.visibility = View.VISIBLE
        sliderBar.visibility = View.VISIBLE

        animatePillSize((280 * dp).toInt(), (48 * dp).toInt(), 12f * dp)
        resetAutoCollapseTimer(2000L)
    }

    fun updateBrightnessState(pct: Int) {
        currentState = IslandState.BRIGHTNESS
        titleText.text = "Brightness"
        subText.visibility = View.GONE
        iconView.setImageResource(android.R.drawable.ic_menu_compass) // Represents glowing status
        sliderBar.progress = pct

        iconView.visibility = View.VISIBLE
        textLayout.visibility = View.VISIBLE
        sliderBar.visibility = View.VISIBLE

        animatePillSize((280 * dp).toInt(), (48 * dp).toInt(), 12f * dp)
        resetAutoCollapseTimer(2000L)
    }

    fun updateMediaState(song: String, artist: String, isPlaying: Boolean, app: String?, artwork: Icon?) {
        this.currentSongTitle = song
        this.currentArtistName = artist
        this.currentAppName = app ?: "Music"
        this.isMusicPlaying = isPlaying

        if (!isPlaying) {
            // When paused, collapse after 4 seconds
            resetAutoCollapseTimer(4000L)
            return
        }

        // Do not interrupt system popups like battery, volume, or brightness
        if (currentState != IslandState.IDLE && currentState != IslandState.MEDIA) {
            return
        }

        currentState = IslandState.MEDIA
        showMediaExpanded()
    }

    private fun showMediaExpanded() {
        titleText.text = currentSongTitle
        subText.text = "$currentArtistName • $currentAppName"
        subText.visibility = View.VISIBLE
        sliderBar.visibility = View.GONE

        if (artwork != null) {
            iconView.setImageIcon(artwork)
        } else {
            iconView.setImageResource(android.R.drawable.ic_media_play)
        }

        iconView.visibility = View.VISIBLE
        textLayout.visibility = View.VISIBLE

        animatePillSize((300 * dp).toInt(), (60 * dp).toInt(), 14f * dp)
        mainHandler.removeCallbacks(collapseRunnable) // Keep expanded while playing music
    }
}
