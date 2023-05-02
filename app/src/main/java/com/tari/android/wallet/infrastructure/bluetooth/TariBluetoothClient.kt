package com.tari.android.wallet.infrastructure.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TariBluetoothClient @Inject constructor() : TariBluetoothAdapter() {
    var onSuccessSharing: () -> Unit = {}

    var onFailedSharing: (String) -> Unit = {}

    var shareData: String? = null

    val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
            result?.device?.let {
                doPairingOrShare(it)
            }
        }
    }

    fun startSharing(data: String) {
        ensureBluetoothIsEnabled {
            doScanning(data)
        }
    }

    fun stopSharing() {
        @Suppress("MissingPermission")
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(callback)
    }

    private fun doScanning(data: String) {
        shareData = data

        val scanFilter = ScanFilter.Builder().setServiceUuid(ParcelUuid(UUID.fromString(SERVICE_UUID))).build()
        val scanSetting = ScanSettings.Builder().build()

        runWithPermissions(bluetoothScanPermission) {
            @Suppress("MissingPermission")
            bluetoothAdapter?.bluetoothLeScanner?.startScan(listOf(scanFilter), scanSetting, callback)
        }
    }

    private fun doPairingOrShare(device: BluetoothDevice) {
        val gattCallback = object : BluetoothGattCallback() {

            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    runWithPermissions(bluetoothConnectPermission) {
                        @Suppress("MissingPermission")
                        gatt?.discoverServices()
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(gatt, status)
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt?.getService(UUID.fromString(SERVICE_UUID))
                    service?.characteristics?.forEach {
                        if (it.uuid == UUID.fromString(CHARACTERISTIC_UUID)) {
                            runWithPermissions(bluetoothConnectPermission) {
                                @Suppress("MissingPermission")
                                gatt.writeCharacteristic(it, shareData.orEmpty().toByteArray(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                            }
                        }
                    }
                }
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                super.onCharacteristicWrite(gatt, characteristic, status)
                runWithPermissions(bluetoothConnectPermission) {
                    @Suppress("MissingPermission")
                    gatt?.disconnect()
                    @Suppress("MissingPermission")
                    gatt?.close()
                    stopSharing()
                    onSuccessSharing.invoke()
                }
            }
        }

        runWithPermissions(bluetoothConnectPermission) {
            @Suppress("MissingPermission")
            device.connectGatt(fragment!!.requireContext(), false, gattCallback)
        }
    }
}