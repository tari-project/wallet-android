package com.tari.android.wallet.ui.common.permission

import android.content.pm.PackageManager
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.icon.IconModule
import javax.inject.Inject

class PermissionManager @Inject constructor(val resourceManager: ResourceManager) {

    val checkForPermission: SingleLiveEvent<String> = SingleLiveEvent()

    val dialog = SingleLiveEvent<ModularDialogArgs>()

    var permissionAction: (() -> Unit)? = null

    fun runWithPermission(permissions: List<String>, action: () -> Unit) {
        permissionAction = action
        checkForPermission.postValue(permissions.first())
    }

    fun showPermissionRequiredDialog(permission: String) {
        val permissionName = kotlin.runCatching {
            val packageManager: PackageManager = resourceManager.context.packageManager
            val permissionInfo = packageManager.getPermissionInfo(permission, 0)
            permissionInfo.labelRes
            resourceManager.getString(permissionInfo.labelRes)
        }.getOrNull() ?: permission

        val args = ModularDialogArgs(
            DialogArgs(), listOf(
                IconModule(R.drawable.vector_sharing_failed),
                HeadModule(resourceManager.getString(R.string.common_error_title)),
                BodyModule(resourceManager.getString(R.string.common_permission_required_dialog_body, permissionName)),
                ButtonModule(resourceManager.getString(R.string.common_retry), ButtonStyle.Normal) {
                    checkForPermission.postValue(permission)
                },
                ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
            )
        )
        dialog.postValue(args)
    }
}