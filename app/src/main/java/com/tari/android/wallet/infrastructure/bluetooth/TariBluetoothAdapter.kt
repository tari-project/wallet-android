package com.tari.android.wallet.infrastructure.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.tari.android.wallet.ui.common.CommonViewModel

abstract class TariBluetoothAdapter() : CommonViewModel() {
    protected var fragment: Fragment? = null
    protected var bluetoothAdapter: BluetoothAdapter? = null

    protected val bluetoothConnectPermission = when {
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.R -> Manifest.permission.BLUETOOTH
        else -> Manifest.permission.BLUETOOTH_CONNECT
    }

    protected val bluetoothScanPermission = when {
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.R -> Manifest.permission.BLUETOOTH
        else -> Manifest.permission.BLUETOOTH_SCAN
    }

    protected val bluetoothAdvertisePermission = when {
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.R -> Manifest.permission.BLUETOOTH
        else -> Manifest.permission.BLUETOOTH_ADVERTISE
    }

    protected val bluetoothAdminPermission = when {
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.R -> Manifest.permission.BLUETOOTH
        else -> Manifest.permission.BLUETOOTH_ADMIN
    }

    val bluetoothPermissions = mutableListOf(
        bluetoothConnectPermission, bluetoothScanPermission, bluetoothAdminPermission, bluetoothAdvertisePermission
    ).apply {
    }.distinct()

    val locationPermission = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).distinct()


    private var onBluetoothEnabled: () -> Unit = {}

    private var onBluetoothNotEnabled: () -> Unit = {}

    var doOnRequiredPermissions: (permissions: List<String>, continueAction: () -> Unit) -> Unit = { _, _ -> }


    fun init(fragment: Fragment) {
        this.fragment = fragment

        val bluetoothManager: BluetoothManager = fragment.requireContext().getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == FragmentActivity.RESULT_OK) {
                onBluetoothEnabled.invoke()
            } else {
                onBluetoothNotEnabled.invoke()
            }
        }
    }

    protected fun runWithPermissions(permission: String, action: () -> Unit) {
        try {
            if (ActivityCompat.checkSelfPermission(fragment!!.requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                doOnRequiredPermissions.invoke(bluetoothPermissions, action)
            } else {
                action()
            }
        } catch (e: SecurityException) {
            doOnRequiredPermissions.invoke(bluetoothPermissions, action)
        }
    }

    protected fun ensureBluetoothIsEnabled(action: () -> Unit = {}) {
        if (bluetoothAdapter?.isEnabled == false) {
            onBluetoothEnabled = action
            onBluetoothNotEnabled = this::showDialogBluetoothRequired
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            fragment?.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            action.invoke()
        }
    }

    private fun showDialogBluetoothRequired() {
        //todo
    }

    companion object {
        const val REQUEST_ENABLE_BT = 15

        const val SERVICE_UUID = "0DABCA14-0688-458D-89D3-367A3D969537"
        const val CHARACTERISTIC_UUID = "999CB541-8D4C-4075-BFF3-43AB74DE8C9B"
    }
}