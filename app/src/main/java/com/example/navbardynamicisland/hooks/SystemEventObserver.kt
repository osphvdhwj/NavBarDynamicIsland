package com.example.navbardynamicisland.hooks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.example.navbardynamicisland.DynamicIslandView
import de.robv.android.xposed.XposedBridge

object SystemEventObserver {
    private var isInitialized = false

    fun init(context: Context, view: DynamicIslandView) {
        if (isInitialized) return

        // 1. Battery Receiver
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

                val batteryPct = (level / scale.toFloat() * 100).toInt()
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                view.updateBatteryState(batteryPct, isCharging)
            }
        }
        context.registerReceiver(batteryReceiver, batteryFilter)

        // 2. Volume Receiver (Music stream 3)
        val volumeFilter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        val volumeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                if (streamType == 3) { // STREAM_MUSIC
                    val volumeVal = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", 0)
                    val maxVolume = 15 // Standard Android max music volume
                    val volPct = (volumeVal / maxVolume.toFloat() * 100).toInt()
                    view.updateVolumeState(volPct)
                }
            }
        }
        context.registerReceiver(volumeReceiver, volumeFilter)

        // 3. Brightness Observer
        val brightnessUri = Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS)
        val brightnessObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                try {
                    val brightness = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                    val maxBrightness = 255
                    val brightPct = (brightness / maxBrightness.toFloat() * 100).toInt()
                    view.updateBrightnessState(brightPct)
                } catch (e: Exception) {
                    XposedBridge.log("[NavbarDynamicIsland] Error reading brightness setting: " + e.message)
                }
            }
        }
        context.contentResolver.registerContentObserver(brightnessUri, false, brightnessObserver)

        isInitialized = true
        XposedBridge.log("[NavbarDynamicIsland] System event observers successfully bound.")
    }
}
