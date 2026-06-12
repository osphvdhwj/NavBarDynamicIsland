package com.example.navbardynamicisland.hooks

import android.content.Context
import android.view.View
import com.example.navbardynamicisland.DynamicIslandView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

object NavigationBarHook {
    private var isIslandAttached = false
    private var dynamicIslandView: DynamicIslandView? = null

    fun init(lpparam: LoadPackageParam) {
        val navHandleClass = try {
            XposedHelpers.findClass(
                "com.android.systemui.navigationbar.gestural.NavigationHandle",
                lpparam.classLoader
            )
        } catch (e: Throwable) {
            // Fallback for older/modified Android builds (e.g. status bar namespace)
            XposedHelpers.findClass(
                "com.android.systemui.statusbar.phone.NavigationHandle",
                lpparam.classLoader
            )
        }

        XposedHelpers.findAndHookMethod(
            navHandleClass,
            "onAttachedToWindow",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val navigationHandle = param.thisObject as View
                    val context = navigationHandle.context

                    if (isIslandAttached) return

                    XposedBridge.log("[NavbarDynamicIsland] NavigationHandle attached. Initializing Dynamic Island...")
                    try {
                        dynamicIslandView = DynamicIslandView(context, navigationHandle)
                        dynamicIslandView?.showOverlay()
                        isIslandAttached = true

                        // Initialize media and system event controllers
                        SystemEventObserver.init(context, dynamicIslandView!!)
                        MediaHook.init(lpparam, dynamicIslandView!!)
                    } catch (t: Throwable) {
                        XposedBridge.log("[NavbarDynamicIsland] Failed to create overlay: " + t.message)
                    }
                }
            }
        )

        XposedHelpers.findAndHookMethod(
            navHandleClass,
            "onDetachedFromWindow",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!isIslandAttached) return
                    XposedBridge.log("[NavbarDynamicIsland] NavigationHandle detached. Removing Dynamic Island...")
                    dynamicIslandView?.removeOverlay()
                    dynamicIslandView = null
                    isIslandAttached = false
                }
            }
        )
    }
}
