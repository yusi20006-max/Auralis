package com.auralis.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

fun Activity.requestNotificationPermissionIfNeeded(requestCode: Int = 1001) {
    if (Build.VERSION.SDK_INT < 33) return
    val granted = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
    if (!granted) {
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), requestCode)
    }
}
