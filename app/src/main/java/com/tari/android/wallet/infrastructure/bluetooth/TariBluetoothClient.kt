package com.tari.android.wallet.infrastructure.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import com.welie.blessed.BluetoothCentralManager
import com.welie.blessed.BluetoothCentralManagerCallback
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.BluetoothPeripheralCallback
import com.welie.blessed.GattStatus
import com.welie.blessed.ScanMode
import com.welie.blessed.WriteType
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TariBluetoothClient @Inject constructor() : TariBluetoothAdapter() {

    var onSuccessSharing: () -> Unit = {}
    var onFailedSharing: (String) -> Unit = {}

    var shareData: String? = null
    var foundDevice: BluetoothPeripheral? = null
    var myGatt: BluetoothGatt? = null

    val manager: BluetoothCentralManager by lazy { BluetoothCentralManager(fragappCompatActivity!!, callback, Handler(Looper.getMainLooper())) }

    val callback = object : BluetoothCentralManagerCallback() {
        override fun onDiscoveredPeripheral(peripheral: BluetoothPeripheral, scanResult: ScanResult) {
            super.onDiscoveredPeripheral(peripheral, scanResult)

            if (foundDevice == null && (scanResult.rssi > RSSI_Threshold)) {
                scanResult.device?.let {
                    foundDevice = peripheral
                    stopScanning()
                    doPairingOrShare(peripheral)
                }
            }
        }
    }

    fun startSharing(data: String) {
        ensureBluetoothIsEnabled {
            runCatching { closeAll() }
            doScanning(data)
        }
    }

    fun stopSharing() {
        runCatching { closeAll() }
        stopScanning()
    }

    fun stopScanning() {
        @Suppress("MissingPermission")
        manager.stopScan()
    }

    private fun doScanning(data: String) {
        shareData = data

        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(UUID.fromString(SERVICE_UUID.lowercase())))
            .build()

        manager.setScanMode(ScanMode.LOW_LATENCY)
        manager.scanForPeripheralsUsingFilters(listOf(scanFilter))
    }

    private fun doPairingOrShare(device: BluetoothPeripheral) {
        val shareData = shareData.orEmpty().toByteArray(Charsets.UTF_16)

        val callback = object : BluetoothPeripheralCallback() {

            override fun onServicesDiscovered(peripheral: BluetoothPeripheral) {
                super.onServicesDiscovered(peripheral)
                val service = peripheral.getService(UUID.fromString(SERVICE_UUID))
                service?.characteristics?.forEach {
                    if (it.uuid.toString().lowercase() == CHARACTERISTIC_UUID.lowercase()) {
                        runWithPermissions(bluetoothConnectPermission) {
                            @Suppress("MissingPermission")
                            val dataChunks = shareData.toList().chunked(512)
                            for (chunk in dataChunks) {
//                                https://stackoverflow.com/questions/38913743/maximum-packet-length-for-bluetooth-le/38914831#38914831
                                peripheral.writeCharacteristic(it, chunk.toByteArray(), WriteType.WITH_RESPONSE)
                            }
                        }
                    }
                }
            }

            override fun onCharacteristicWrite(
                peripheral: BluetoothPeripheral,
                value: ByteArray?,
                characteristic: BluetoothGattCharacteristic,
                status: GattStatus
            ) {
                super.onCharacteristicWrite(peripheral, value, characteristic, status)
                runWithPermissions(bluetoothConnectPermission) {
                    peripheral.cancelConnection()
                    stopScanning()
                    closeAll()
                }

                when (status) {
                    GattStatus.SUCCESS -> onSuccessSharing()
                    //todo str
                    GattStatus.INVALID_HANDLE -> onFailedSharing.invoke("Failed to share data during BLE transfer. Please try again.")
                    else -> Unit
                }
            }
        }

        manager.connectPeripheral(device, callback)
    }

    private fun closeAll() {
        foundDevice = null
        closeGatt()
    }

    private fun closeGatt() {
        @Suppress("MissingPermission")
        myGatt?.disconnect()
        @Suppress("MissingPermission")
        myGatt?.close()
        myGatt = null
    }

    companion object {
        const val RSSI_Threshold = -50
    }
}