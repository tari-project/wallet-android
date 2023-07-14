package com.tari.android.wallet.infrastructure.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.tari.android.wallet.ui.common.CommonViewModel

abstract class TariBluetoothAdapter() : CommonViewModel() {
    protected var fragappCompatActivity: AppCompatActivity? = null
        private set(value) {
            field = value
            onContextSet()
        }
    protected val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter
    protected val bluetoothManager: BluetoothManager?
        get() = fragappCompatActivity!!.getSystemService(BluetoothManager::class.java)

    open fun onContextSet() { }

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
        else ->  Manifest.permission.BLUETOOTH
    }

    val bluetoothPermissions = mutableListOf(
        bluetoothConnectPermission, bluetoothScanPermission, bluetoothAdminPermission, bluetoothAdvertisePermission
    ).apply {
    }.distinct()

    val locationPermission = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).distinct()


    private var onBluetoothEnabled: () -> Unit = {}

    private var onBluetoothNotEnabled: () -> Unit = {}

    var doOnRequiredPermissions: (permissions: List<String>, continueAction: () -> Unit) -> Unit = { _, _ -> }


    fun init(fragment: AppCompatActivity) {
        this.fragappCompatActivity = fragment
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

    protected fun runWithPermissions(permission: String, silently: Boolean = false, action: () -> Unit) {
        try {
            if (ActivityCompat.checkSelfPermission(fragappCompatActivity!!, permission) != PackageManager.PERMISSION_GRANTED) {
                if (!silently) doOnRequiredPermissions.invoke(listOf(permission), action)
            } else {
                action()
            }
        } catch (e: SecurityException) {
            if (!silently) doOnRequiredPermissions.invoke(listOf(permission), action)
        }
    }

    protected fun ensureBluetoothIsEnabled(action: () -> Unit = {}) {
        if (bluetoothAdapter?.isEnabled == false) {
            onBluetoothEnabled = action
            onBluetoothNotEnabled = this::showDialogBluetoothRequired
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            @Suppress("MissingPermission")
            fragappCompatActivity?.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
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
        const val TRANSACTION_DATA_UUID = "4567F76F-2577-4EA4-9220-AFCCCAA89B59"

        const val chunkSize = 150
    }
}