package com.tari.android.wallet.ui.extension

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.core.content.ContextCompat
import com.orhanobut.logger.Printer
import com.tari.android.wallet.ui.common.CommonFragment


object PermissionExtensions {

    private val logger: Printer
        get() = com.orhanobut.logger.Logger.t("permission")

    fun CommonFragment<*, *>.runWithPermission(permission: String, openSettings: Boolean = false, callback: () -> Unit) {
        logger.d("runWithPermissions: start")

        if (this.isDetached) return

        if (requireContext().isPermissionGranted(permission)) {
            logger.d("permission granted: $permission")
            callback()
        } else {
            if (!shouldShowRequestPermissionRationale(permission)) {
                grantedAction = callback
                launcher.launch(permission)
            } else {
                if (openSettings) {
                    requireContext().openSettings()
                }
            }
        }
    }

    fun Context.isPermissionGranted(permission: String): Boolean = isPermissionNotGranted(permission).not()

    fun Context.isPermissionNotGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED

    fun Context.openSettings() {
        Intent(ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            ContextCompat.startActivity(this@openSettings, this, Bundle())
        }
    }
}