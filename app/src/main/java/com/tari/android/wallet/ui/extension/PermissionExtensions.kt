package com.tari.android.wallet.ui.extension

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.orhanobut.logger.Printer
import com.tari.android.wallet.ui.common.CommonFragment

object PermissionExtensions {

    private val logger: Printer
        get() = com.orhanobut.logger.Logger.t("permission")

    fun CommonFragment<*,*>.runWithPermission(permission: String, callback: () -> Unit) {
        logger.d("runWithPermissions: start")

        if (requireContext().isPermissionGranted(permission)) {
            logger.d("permission granted: $permission")
            callback()
        } else {
            grantedAction = callback
            launcher.launch(permission)
        }
    }

    fun Context.isPermissionGranted(permission: String): Boolean = isPermissionNotGranted(permission).not()

    fun Context.isPermissionNotGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED
}