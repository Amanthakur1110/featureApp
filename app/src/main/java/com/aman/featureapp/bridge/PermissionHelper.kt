package com.aman.featureapp.bridge

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionHelper {
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasPermissions(context: Context, vararg permissions: String): Boolean {
        return permissions.all { hasPermission(context, it) }
    }
}
