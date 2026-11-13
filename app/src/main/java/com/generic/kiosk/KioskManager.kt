package com.generic.kiosk

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build

class AdminReceiver : android.app.admin.DeviceAdminReceiver()

class KioskManager(private val context: Context) {
    private val dpm = context.getSystemService(DevicePolicyManager::class.java)
    private val admin = ComponentName(context, AdminReceiver::class.java)
    val isDeviceOwner: Boolean get() = dpm.isDeviceOwnerApp(context.packageName)
    val isLockTaskPermitted: Boolean get() = dpm.isLockTaskPermitted(context.packageName)
    val isLockTaskActive: Boolean get() = (context as? Activity)?.isInLockTaskModeCompat() == true

    fun configureAndStart(activity: Activity, enabled: Boolean) {
        if (!isDeviceOwner) return
        if (enabled) {
            dpm.setLockTaskPackages(admin, arrayOf(context.packageName))
            if (Build.VERSION.SDK_INT >= 28) dpm.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
            if (!activity.isInLockTaskModeCompat()) activity.startLockTask()
        } else {
            if (activity.isInLockTaskModeCompat()) activity.stopLockTask()
            dpm.setLockTaskPackages(admin, emptyArray())
        }
    }

    fun stop(activity: Activity) { if (isDeviceOwner && activity.isInLockTaskModeCompat()) { activity.stopLockTask(); dpm.setLockTaskPackages(admin, emptyArray()) } }
}

private fun Activity.isInLockTaskModeCompat(): Boolean = if (Build.VERSION.SDK_INT >= 23) getSystemService(android.app.ActivityManager::class.java).lockTaskModeState != android.app.ActivityManager.LOCK_TASK_MODE_NONE else false
