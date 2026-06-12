package com.example.navbardynamicisland

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.navbardynamicisland.databinding.ActivityMainBinding
import com.google.android.material.color.DynamicColors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply Material You dynamic coloring (wallpaper-based themes)
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Preferences file name matching classic Xposed expectations
        prefs = getSharedPreferences("com.example.navbardynamicisland_preferences", Context.MODE_PRIVATE)

        loadSettings()
        setupListeners()
    }

    private fun loadSettings() {
        // Load configurations, default values set to true/recommended options
        binding.switchEnableModule.isChecked = prefs.getBoolean("enable_module", true)
        binding.switchMusic.isChecked = prefs.getBoolean("enable_music", true)
        binding.switchBattery.isChecked = prefs.getBoolean("enable_battery", true)
        binding.switchVolume.isChecked = prefs.getBoolean("enable_volume", true)
        binding.switchBrightness.isChecked = prefs.getBoolean("enable_brightness", true)
        
        // Load gestures configurations
        val gestureOption = prefs.getString("gesture_activation", "swipe_up")
        when (gestureOption) {
            "swipe_up" -> binding.radioSwipeUp.isChecked = true
            "double_tap" -> binding.radioDoubleTap.isChecked = true
            "long_press" -> binding.radioLongPress.isChecked = true
        }
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            val editor = prefs.edit()
            editor.putBoolean("enable_module", binding.switchEnableModule.isChecked)
            editor.putBoolean("enable_music", binding.switchMusic.isChecked)
            editor.putBoolean("enable_battery", binding.switchBattery.isChecked)
            editor.putBoolean("enable_volume", binding.switchVolume.isChecked)
            editor.putBoolean("enable_brightness", binding.switchBrightness.isChecked)

            val selectedGesture = when {
                binding.radioSwipeUp.isChecked -> "swipe_up"
                binding.radioDoubleTap.isChecked -> "double_tap"
                binding.radioLongPress.isChecked -> "long_press"
                else -> "swipe_up"
            }
            editor.putString("gesture_activation", selectedGesture)

            editor.apply()

            // World-readable permissions helper for root users
            fixPreferencesPermissions()

            Toast.makeText(this, "Settings Saved! Restart SystemUI via LSPosed manager to apply.", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Fixes file permissions on Android 10+ so SystemUI can read preferences.
     * Since this is a root-integrated app, it executes a simple shell command.
     */
    private fun fixPreferencesPermissions() {
        try {
            val prefsPath = "/data/data/$packageName/shared_prefs"
            val command = "chmod -R 777 $prefsPath"
            Runtime.getRuntime().exec(arrayOf("su", "-c", command))
        } catch (e: Exception) {
            // Log/ignore if root permission not granted yet
        }
    }
}
