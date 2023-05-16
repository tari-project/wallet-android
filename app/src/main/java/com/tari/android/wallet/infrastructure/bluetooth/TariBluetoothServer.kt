package com.tari.android.wallet.infrastructure.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.os.ParcelUuid
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkHandler
import com.tari.android.wallet.data.sharedPrefs.bluetooth.BluetoothServerState
import com.tari.android.wallet.data.sharedPrefs.bluetooth.ShareSettingsRepository
import com.tari.android.wallet.extension.addTo
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class TariBluetoothServer @Inject constructor(private val shareSettingsRepository: ShareSettingsRepository, val deeplinkHandler: DeeplinkHandler) :
    TariBluetoothAdapter() {

    private var bluetoothGattServer: BluetoothGattServer? = null

    init {
        shareSettingsRepository.updateNotifier.subscribe {
            try {
                handleReceiving()
            } catch (e: Throwable) {
                logger.e("Error handling receiving", e)
            }
        }.addTo(compositeDisposable)
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            logger.i("onStartSuccess: $settingsInEffect")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            logger.i("onStartFailure: $errorCode")
        }
    }

    private fun handleReceiving() {
        when (shareSettingsRepository.bluetoothSettingsState) {
            BluetoothServerState.DISABLED -> stopReceiving()
            BluetoothServerState.WHILE_UP -> startReceiving()
            BluetoothServerState.ENABLED -> startReceiving()
            null -> Unit
        }
    }

    private fun startReceiving() {
        ensureBluetoothIsEnabled {
            runWithPermissions(bluetoothAdvertisePermission) {
                runWithPermissions(bluetoothConnectPermission) {
                    doReceiving()
                }
            }
        }
    }

    var onReceived: (List<DeepLink.Contacts.DeeplinkContact>) -> Unit = {}

    private fun doReceiving() {
        logger.e("doReceiving")

        val callback = object : BluetoothGattServerCallback() {

            val receivedString = StringBuilder()

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

                logger.e(preparedWrite.toString() + " " + responseNeeded.toString() + " " + offset.toString() + " " + value.toString())

                //todo some delay here
                if (characteristic?.uuid.toString().lowercase() == CHARACTERISTIC_UUID.lowercase()) {
                    if (receivedString.isEmpty()) {
                        receivedString.append(String(value!!))
                        doHandling()
                    } else {
                        receivedString.append(String(value!!))
                    }
                }

                if (responseNeeded) {
                    runWithPermissions(bluetoothConnectPermission) {
                        @Suppress("MissingPermission")
                        bluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                    }
                }
            }

            override fun onExecuteWrite(device: BluetoothDevice?, requestId: Int, execute: Boolean) {
                super.onExecuteWrite(device, requestId, execute)

                if (execute) {
                    doHandling()
                }
            }

            private fun doHandling() {
                val handled = runCatching { deeplinkHandler.handle(receivedString.toString()) }.getOrNull()

                if (handled != null && handled is DeepLink.Contacts) {
                    receivedString.clear()
                    onReceived.invoke(handled.contacts)
                    receivedString.clear()
                }
            }
        }

        @Suppress("MissingPermission")
        bluetoothGattServer = bluetoothManager?.openGattServer(fragappCompatActivity!!, callback) ?: return

        val myService = BluetoothGattService(UUID.fromString(SERVICE_UUID), BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val myCharacteristic = BluetoothGattCharacteristic(
            UUID.fromString(CHARACTERISTIC_UUID),
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        myService.addCharacteristic(myCharacteristic)

        @Suppress("MissingPermission")
        bluetoothGattServer?.addService(myService)

        val bluetoothLeAdvertiser = bluetoothAdapter!!.bluetoothLeAdvertiser
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(UUID.fromString(SERVICE_UUID)))
            .build()

        @Suppress("MissingPermission")
        bluetoothLeAdvertiser.startAdvertising(settings, data, advertiseCallback)

        logger.i("startAdvertising")
    }

    private fun stopReceiving() {
        val bluetoothLeAdvertiser = bluetoothAdapter!!.bluetoothLeAdvertiser

        @Suppress("MissingPermission")
        bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)

        bluetoothGattServer?.let {
            @Suppress("MissingPermission")
            it.clearServices()
            @Suppress("MissingPermission")
            it.close()
        }
        bluetoothGattServer = null
    }
}