package com.tari.android.wallet.infrastructure.permission

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.tari.android.wallet.R
import com.tari.android.wallet.application.TariNavigator
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.icon.IconModule
import org.joda.time.DateTime
import javax.inject.Inject

// TODO refactor this. It doesn't work reliably
class PermissionManager @Inject constructor(
    private val context: Context,
    private val resourceManager: ResourceManager,
    private val tariNavigator: TariNavigator,
) {

    val checkForPermission: SingleLiveEvent<List<String>> = SingleLiveEvent()

    val dialog = SingleLiveEvent<ModularDialogArgs>()

    var grantedAction: () -> Unit = {}

    val openSettings = SingleLiveEvent<Unit>()

    private var silently = false
    private val waitingPermissions = mutableListOf<String>()
    private var lastAskedTime: DateTime? = null

    fun runWithPermission(permissions: List<String>, silently: Boolean = false, action: () -> Unit) {
        this.silently = silently
        grantedAction = {
            action()
        }

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(
                context,
                it
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isEmpty()) {
            action()
        }

        val shouldShowRationale = notGranted.any {
            tariNavigator.currentActivity.shouldShowRequestPermissionRationale(it)
        }

        if (shouldShowRationale) {
            if (!silently) {
                showPermissionRequiredDialog(notGranted)
            }
        } else {
            val neededPermissions = notGranted.filter { !waitingPermissions.contains(it) }
            if (neededPermissions.isEmpty()) return
            checkForPermission.postValue(neededPermissions)
        }
    }

    fun showPermissionRequiredDialog(permissions: List<String>) {
        if (silently) return
        if (permissions.all { waitingPermissions.contains(it) } && lastAskedTime != null && lastAskedTime!!.plusMinutes(1).isAfterNow) return

        lastAskedTime = DateTime.now()
        waitingPermissions.addAll(permissions)

        val permissionNames = permissions.map {
            kotlin.runCatching {
                val packageManager: PackageManager = resourceManager.context.packageManager
                val permissionInfo = packageManager.getPermissionInfo(it, 0)
                permissionInfo.labelRes
                resourceManager.getString(permissionInfo.labelRes)
            }.getOrNull() ?: it
        }.toList().joinToString(", ")

        val args = ModularDialogArgs(
            DialogArgs {
                waitingPermissions.clear()
                lastAskedTime = null
            }, listOf(
                IconModule(R.drawable.vector_sharing_failed),
                HeadModule(resourceManager.getString(R.string.common_error_title)),
                BodyModule(resourceManager.getString(R.string.common_permission_required_dialog_body, permissionNames)),
                ButtonModule(resourceManager.getString(R.string.common_permission_required_button), ButtonStyle.Normal) {
                    waitingPermissions.clear()
                    lastAskedTime = null
                    openSettings.postValue(Unit)
                },
                ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
            )
        )
        dialog.postValue(args)
    }
}