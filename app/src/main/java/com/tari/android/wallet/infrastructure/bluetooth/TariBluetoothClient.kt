package com.tari.android.wallet.infrastructure.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkHandler
import com.welie.blessed.BluetoothCentralManager
import com.welie.blessed.BluetoothCentralManagerCallback
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.BluetoothPeripheralCallback
import com.welie.blessed.GattStatus
import com.welie.blessed.ScanMode
import com.welie.blessed.WriteType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TariBluetoothClient @Inject constructor(val deeplinkHandler: DeeplinkHandler) : TariBluetoothAdapter() {

    var onSuccessSharing: () -> Unit = {}
    var onFailedSharing: (String) -> Unit = {}

    var shareData: String? = null
    var scanningCallback: ((DeepLink.UserProfile) -> Unit)? = null
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
            shareData = data
            doScanning()
        }
    }

    fun startDeviceScanning(callback: (DeepLink.UserProfile) -> Unit) {
        ensureBluetoothIsEnabled {
            runCatching { closeAll() }
            this.scanningCallback = callback
            doScanning()
        }
    }

    fun stopSharing() {
        shareData = null
        scanningCallback = null
        runCatching { closeAll() }
        stopScanning()
    }

    fun stopScanning() {
        @Suppress("MissingPermission")
        manager.stopScan()
    }

    private fun doScanning() {
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(UUID.fromString(SERVICE_UUID.lowercase())))
            .build()

        manager.setScanMode(ScanMode.LOW_LATENCY)
        manager.scanForPeripheralsUsingFilters(listOf(scanFilter))
    }

    private fun doPairingOrShare(device: BluetoothPeripheral) {

        val contactlessPaymentCallback = object : BluetoothPeripheralCallback() {
            override fun onServicesDiscovered(peripheral: BluetoothPeripheral) {
                super.onServicesDiscovered(peripheral)
                val service = peripheral.getService(UUID.fromString(SERVICE_UUID))
                service?.characteristics?.forEach {
                    if (it.uuid.toString().lowercase() == TRANSACTION_DATA_UUID.lowercase()) {
                        peripheral.readCharacteristic(it)
                        logger.e("contactlessPayment: read:")
                    }
                }
            }

            var wholeData = byteArrayOf()

            override fun onCharacteristicUpdate(
                peripheral: BluetoothPeripheral,
                value: ByteArray?,
                characteristic: BluetoothGattCharacteristic,
                status: GattStatus
            ) {
                super.onCharacteristicUpdate(peripheral, value, characteristic, status)

                if (characteristic.uuid.toString().lowercase() == TRANSACTION_DATA_UUID.lowercase()) {
                    val lastByte = value?.last() ?: 0
                    wholeData += value?.dropLast(1)?.toByteArray() ?: byteArrayOf()

                    if ((value?.size ?: 0) < chunkSize && value?.lastOrNull() == 0.toByte()) {
                        logger.e("share: read: wrong chunk size: ${value.size}")
                    }

                    logger.e("contactlessPayment: read: chunk size: ${value?.size ?: 0}")
                    logger.e("contactlessPayment: read: chunk: ${String(value ?: byteArrayOf(), Charsets.UTF_8)}")
                    logger.e("contactlessPayment: read: whole data: ${String(wholeData, Charsets.UTF_8)}")

                    if (lastByte == 1.toByte()) {
                        peripheral.readCharacteristic(characteristic)
                    } else {
                        doHandling(String(wholeData, Charsets.UTF_8))
                    }
                }
            }

            private fun doHandling(string: String): GattStatus {
                logger.e("contactlessPayment: handle: url: $string")

                val handled = runCatching { deeplinkHandler.handle(string) }.getOrNull()

                logger.e("contactlessPayment: handle: handled: $handled")

                scanningCallback?.let {
                    if (handled != null && handled is DeepLink.UserProfile) {
                        scanningCallback?.invoke(handled)
                    } else {
                        onFailedSharing("Something went wrong during contactless payment. Please try again.\n\nError: $string")
                    }
                }
                wholeData = byteArrayOf()
                return if (handled != null) GattStatus.SUCCESS else GattStatus.INVALID_HANDLE
            }
        }


        val callback = object : BluetoothPeripheralCallback() {

            var chunks = listOf<ByteArray>()
            var chunkDevice: BluetoothPeripheral? = null
            var characteristic: BluetoothGattCharacteristic? = null

            override fun onServicesDiscovered(peripheral: BluetoothPeripheral) {
                super.onServicesDiscovered(peripheral)
                val service = peripheral.getService(UUID.fromString(SERVICE_UUID))
                service?.characteristics?.forEach {
                    if (it.uuid.toString().lowercase() == CHARACTERISTIC_UUID.lowercase() && shareData != null) {
                        val shareData = shareData.orEmpty().toByteArray(Charsets.UTF_8)
                        logger.e("shareCharacteristic: write: whole data: ${String(shareData, Charsets.UTF_8)}")
                        runWithPermissions(bluetoothConnectPermission) {
                            viewModelScope.launch(Dispatchers.IO) {
                                val chunked = shareData.toList().chunked(chunkSize)
                                chunks =
                                    chunked.mapIndexed { index, items -> (items + if (index == chunked.size - 1) 0 else 1).toByteArray() }.toList()
                                chunkDevice = peripheral
                                characteristic = it
                                doChunkWrite()
                            }
                        }
                    }

                    if (it.uuid.toString().lowercase() == TRANSACTION_DATA_UUID.lowercase()) {
                        peripheral.readCharacteristic(it)
                        logger.e("contactlessPayment: read:")
                    }
                }
            }

            private fun doChunkWrite() {
                if (chunks.isEmpty()) {
                    logger.e("shareCharacteristic: write: done")
                    return
                }
                val newChunk = chunks.first()
                chunks = chunks.drop(1)
                chunkDevice?.writeCharacteristic(characteristic!!, newChunk, WriteType.WITH_RESPONSE)
                logger.e("shareCharacteristic: write: chunk: ${newChunk.joinToString("")}")
                logger.e("shareCharacteristic: write: chunk: ${String(newChunk, Charsets.UTF_8)}")
            }

            override fun onCharacteristicWrite(
                peripheral: BluetoothPeripheral,
                value: ByteArray?,
                characteristic: BluetoothGattCharacteristic,
                status: GattStatus
            ) {
                super.onCharacteristicWrite(peripheral, value, characteristic, status)

                if (chunks.isEmpty()) {
                    runWithPermissions(bluetoothConnectPermission) {
                        peripheral.cancelConnection()
                        stopScanning()
                        closeAll()
                    }

                    when (status) {
                        GattStatus.SUCCESS -> onSuccessSharing()
                        GattStatus.INVALID_HANDLE -> onFailedSharing.invoke("Failed to share data during BLE transfer. Please try again.")
                        else -> Unit
                    }
                }

                doChunkWrite()
            }
        }

        if (shareData != null) {
            manager.connectPeripheral(device, callback)
        } else {
            manager.connectPeripheral(device, contactlessPaymentCallback)
        }
    }

    private fun closeAll() {
        foundDevice?.let { manager.cancelConnection(it) }
        foundDevice = null
        shareData = null
        scanningCallback = null
        closeGatt()
        runCatching { manager.close() }
    }

    private fun closeGatt() {
        @Suppress("MissingPermission")
        myGatt?.disconnect()
        @Suppress("MissingPermission")
        myGatt?.close()
        myGatt = null
    }

    companion object {
        const val RSSI_Threshold = -55
    }
}