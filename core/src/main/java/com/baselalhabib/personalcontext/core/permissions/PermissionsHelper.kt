package com.baselalhabib.personalcontext.core.permissions

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import androidx.core.content.ContextCompat

/**
 * Helper utility for checking standard runtime permissions and special system permissions
 * (such as App Usage Access).
 */
object PermissionsHelper {

    /**
     * Returns true if all [permissions] are granted for the given [context].
     */
    fun hasPermissions(context: Context, permissions: List<String>): Boolean {
        return permissions.all { isPermissionGranted(context, it) }
    }

    /**
     * Checks if a single [permission] is granted. Handles special cases like App Usage Access.
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return when (permission) {
            android.Manifest.permission.PACKAGE_USAGE_STATS -> hasUsageAccessPermission(context)
            else -> ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Checks whether the user has granted App Usage Access permission (PACKAGE_USAGE_STATS).
     */
    fun hasUsageAccessPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
