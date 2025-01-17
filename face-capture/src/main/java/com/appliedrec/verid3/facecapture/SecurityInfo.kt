package com.appliedrec.verid3.facecapture

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

object SecurityInfo {

    @JvmStatic
    fun isDeviceRooted(context: Context): Boolean {
        return isRooted() || hasRootApps(context) || isInsecureBuild() || canExecuteRootCommands()
    }

    @JvmStatic
    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.MODEL.contains("Emulator")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.PRODUCT.contains("sdk"))
    }

    @JvmStatic
    fun isDebuggable(context: Context): Boolean {
        return (0 != context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)
    }

    private fun isRooted(): Boolean {
        val paths = arrayOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/xbin/su",
            "/data/local/bin/su", "/system/su", "/system/bin/.ext/.su"
        )
        for (path in paths) {
            if (File(path).exists()) {
                return true
            }
        }
        return false
    }

    private fun hasRootApps(context: Context): Boolean {
        val dangerousPackages = listOf(
            "com.koushikdutta.superuser", "com.thirdparty.superuser",
            "eu.chainfire.supersu", "com.noshufou.android.su",
            "com.devadvance.rootcloak"
        )
        val pm = context.packageManager
        return dangerousPackages.any { packageName ->
            try { pm.getPackageInfo(packageName, 0); true }
            catch (e: PackageManager.NameNotFoundException) { false }
        }
    }

    private fun isInsecureBuild(): Boolean {
        return (Build.TAGS?.contains("test-keys") == true
                || Build.FINGERPRINT.startsWith("generic"))
    }

    private fun canExecuteRootCommands(): Boolean {
        return try {
            Runtime.getRuntime().exec("su").exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }

}