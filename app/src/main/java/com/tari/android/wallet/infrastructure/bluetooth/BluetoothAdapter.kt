package com.tari.android.wallet.infrastructure.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.tari.android.wallet.R
import com.tari.android.wallet.infrastructure.bluetooth.devicesModule.DevicesModule
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import java.util.UUID
import javax.inject.Inject
import android.bluetooth.BluetoothAdapter as AndroidBluetoothAdapter


class BluetoothAdapter @Inject constructor() : CommonViewModel() {

    private var fragment: Fragment? = null
    private var bluetoothAdapter: AndroidBluetoothAdapter? = null

    private val bluetoothPermission = when {
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.R -> Manifest.permission.BLUETOOTH
        else -> Manifest.permission.BLUETOOTH_CONNECT
    }

    val bluetoothPermissions = mutableListOf(bluetoothPermission).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
            add(Manifest.permission.BLUETOOTH_ADMIN)
        }
    }.distinct()

    val locationPermission = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).distinct()

    var onReceived: (String) -> Unit = {}

    var onSuccessSharing: () -> Unit = {}

    var onBluetoothEnabled: () -> Unit = {}

    var onBluetoothNotEnabled: () -> Unit = {}

    fun init(fragment: Fragment) {
        this.fragment = fragment

        val bluetoothManager: BluetoothManager = fragment.requireContext().getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
    }

    fun isPermissionsGranted(context: Context, permissions: List<String>) =
        permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    fun startSharing(data: String) {
        ensureBluetoothIsEnabled {
            doScanning(data)
        }
    }

    fun stopSharing() {
//todo
    }

    fun startReceiving() {
        ensureBluetoothIsEnabled {
            doReceiving()
        }
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

    private fun ensureBluetoothIsEnabled(action: () -> Unit = {}) {
        if (bluetoothAdapter?.isEnabled == false) {
            onBluetoothEnabled = action
            val enableBtIntent = Intent(AndroidBluetoothAdapter.ACTION_REQUEST_ENABLE)
            fragment?.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            action.invoke()
        }
    }

    @SuppressLint("MissingPermission")
    private fun doScanning(data: String) {
        val devicesModule = DevicesModule(mutableListOf())

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
                result?.device?.let {
                    devicesModule.devices.add(it)
                    devicesModule.listUpdatedAction()
                }
            }
        }
        val scanFilter = ScanFilter.Builder().setServiceUuid(ParcelUuid(UUID.fromString(SERVICE_UUID))).build()
        val scanSetting = ScanSettings.Builder().build()

        bluetoothAdapter?.bluetoothLeScanner?.startScan(listOf(scanFilter), scanSetting, callback)

        val args = ModularDialogArgs(
            DialogArgs(canceledOnTouchOutside = false) {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(callback)
            }, listOf(
                HeadModule(resourceManager.getString(R.string.share_via_bluetooth_scanning_title)),
                BodyModule(resourceManager.getString(R.string.share_via_bluetooth_scanning_body)),
                devicesModule,
                ButtonModule(resourceManager.getString(R.string.common_share), ButtonStyle.Normal) {
                    doPairingOrShare(data, devicesModule.checkedList)
                },
                ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
            )
        )
        _modularDialog.postValue(args)
    }

    @SuppressLint("MissingPermission")
    private fun doPairingOrShare(data: String, devices: List<BluetoothDevice>) {
        for (device in devices) {

            val gattCallback = object : BluetoothGattCallback() {

                @SuppressLint("MissingPermission")
                override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                    super.onConnectionStateChange(gatt, status, newState)
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        gatt?.discoverServices()
                    }
                }

                @SuppressLint("MissingPermission")
                override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                    super.onServicesDiscovered(gatt, status)
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        val service = gatt?.getService(UUID.fromString(SERVICE_UUID))
                        service?.characteristics?.forEach {
                            if (it.uuid == UUID.fromString(CHARACTERISTIC_UUID)) {
                                gatt.writeCharacteristic(it, data.toByteArray(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                            }
                        }
                    }
                }

                override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                    super.onCharacteristicWrite(gatt, characteristic, status)
                    onSuccessSharing.invoke()
                }
            }


            device.connectGatt(fragment!!.requireContext(), false, gattCallback)
        }
    }

    @SuppressLint("MissingPermission")
    private fun doReceiving() {

        logger.e("doReceiving")

        val callback = object : BluetoothGattServerCallback() {
            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice?,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic?,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray?
            ) {
                super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)
                if (characteristic?.uuid == UUID.fromString(CHARACTERISTIC_UUID)) {
                    onReceived.invoke(String(value!!))
                }
            }
        }

        val advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
                logger.e("onStartSuccess: $settingsInEffect")
            }

            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
                logger.e("onStartFailure: $errorCode")
            }
        }

        val bluetoothManager = fragment?.requireContext()?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager? ?: return
        val bluetoothGattServer = bluetoothManager.openGattServer(fragment!!.requireContext(), callback) ?: return

        val myService = BluetoothGattService(UUID.fromString(SERVICE_UUID), BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val myCharacteristic = BluetoothGattCharacteristic(
            UUID.fromString(CHARACTERISTIC_UUID),
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        myService.addCharacteristic(myCharacteristic)

        bluetoothGattServer.addService(myService)

        val bluetoothLeAdvertiser = bluetoothAdapter!!.bluetoothLeAdvertiser
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(UUID.fromString(SERVICE_UUID)))
            .build()

        bluetoothLeAdvertiser.startAdvertising(settings, data, advertiseCallback)

        logger.e("startAdvertising")
    }

    companion object {
        const val REQUEST_ENABLE_BT = 15
        const val REQUEST_CODE_RECOVERABLE_ENABLE = 16

        const val SERVICE_UUID = "0DABCA14-0688-458D-89D3-367A3D969537"
        const val CHARACTERISTIC_UUID = "999CB541-8D4C-4075-BFF3-43AB74DE8C9B"
    }
}