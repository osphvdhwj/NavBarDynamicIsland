package com.example.navbardynamicisland.hooks

import android.graphics.drawable.Icon
import com.example.navbardynamicisland.DynamicIslandView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

object MediaHook {
    fun init(lpparam: LoadPackageParam, view: DynamicIslandView) {
        val mediaDataManagerClass = try {
            XposedHelpers.findClass("com.android.systemui.media.controls.pipeline.MediaDataManager", lpparam.classLoader)
        } catch (e: Throwable) {
            try {
                XposedHelpers.findClass("com.android.systemui.media.MediaDataManager", lpparam.classLoader)
            } catch (e2: Throwable) {
                XposedBridge.log("[NavbarDynamicIsland] MediaDataManager class not found.")
                return
            }
        }

        val mediaDataClassName = try {
            XposedHelpers.findClass("com.android.systemui.media.controls.models.player.MediaData", lpparam.classLoader)
            "com.android.systemui.media.controls.models.player.MediaData"
        } catch (e: Throwable) {
            try {
                XposedHelpers.findClass("com.android.systemui.media.MediaData", lpparam.classLoader)
                "com.android.systemui.media.MediaData"
            } catch (e2: Throwable) {
                XposedBridge.log("[NavbarDynamicIsland] MediaData class not found.")
                return
            }
        }

        XposedHelpers.findAndHookMethod(
            mediaDataManagerClass,
            "onMediaDataLoaded",
            String::class.java,
            String::class.java,
            mediaDataClassName,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val mediaData = param.args[2] ?: return
                    try {
                        val song = XposedHelpers.getObjectField(mediaData, "song") as? String ?: "Unknown"
                        val artist = XposedHelpers.getObjectField(mediaData, "artist") as? String ?: "Unknown"

                        val isPlaying = try {
                            XposedHelpers.getBooleanField(mediaData, "isPlaying")
                        } catch (t: Throwable) {
                            try {
                                val playbackInfo = XposedHelpers.getObjectField(mediaData, "playbackInfo")
                                XposedHelpers.getBooleanField(playbackInfo, "isPlaying")
                            } catch (t2: Throwable) {
                                true
                            }
                        }

                        val artwork = try {
                            XposedHelpers.getObjectField(mediaData, "artwork") as? Icon
                        } catch (t: Throwable) {
                            null
                        }

                        val appName = try {
                            XposedHelpers.getObjectField(mediaData, "appName") as? String
                        } catch (t: Throwable) {
                            "Music"
                        }

                        view.updateMediaState(song, artist, isPlaying, appName, artwork)
                    } catch (t: Throwable) {
                        XposedBridge.log("[NavbarDynamicIsland] Error parsing MediaData: " + t.message)
                    }
                }
            }
        )
    }
}
