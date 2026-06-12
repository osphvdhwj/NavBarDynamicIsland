package com.example.navbardynamicisland

import com.example.navbardynamicisland.hooks.NavigationBarHook
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class XposedInit : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName != "com.android.systemui") return

        XposedBridge.log("[NavbarDynamicIsland] Loading hooks in SystemUI...")
        try {
            NavigationBarHook.init(lpparam)
        } catch (t: Throwable) {
            XposedBridge.log("[NavbarDynamicIsland] Error loading hooks: " + t.message)
        }
    }
}
