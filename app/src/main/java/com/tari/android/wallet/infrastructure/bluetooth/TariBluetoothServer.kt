package com.tari.android.wallet.infrastructure.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
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
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.welie.blessed.BluetoothCentral
import com.welie.blessed.BluetoothPeripheralManager
import com.welie.blessed.BluetoothPeripheralManagerCallback
import com.welie.blessed.GattStatus
import com.welie.blessed.ReadResponse
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class TariBluetoothServer @Inject constructor(
    private val shareSettingsRepository: ShareSettingsRepository,
    val deeplinkHandler: DeeplinkHandler,
) : TariBluetoothAdapter() {

    private var bluetoothGattServer: BluetoothGattServer? = null

    override fun onContextSet() {
        super.onContextSet()
        compositeDisposable.dispose()
        compositeDisposable = CompositeDisposable()
        stopReceiving()
        handleReceiving()
        shareSettingsRepository.updateNotifier.subscribe {
            try {
                handleReceiving()
            } catch (e: Throwable) {
                logger.i("Error handling receiving", e)
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
        val permissions = mutableListOf<String>()
        permissions.addAll(locationPermission)
        permissions.addAll(bluetoothPermissions)
        permissionManager.runWithPermission(permissions, true) {
            ensureBluetoothIsEnabled {
                doReceiving2()
            }
        }
    }

    var onReceived: (List<DeepLink.Contacts.DeeplinkContact>) -> Unit = {}

    private fun doReceiving2() {
        stopReceiving()

        val callback = object : BluetoothPeripheralManagerCallback() {

            var wholeData = byteArrayOf()

            var shareChunkedData = mutableListOf<ByteArray>()

            var throttle: Disposable? = null

            override fun onCharacteristicWrite(
                bluetoothCentral: BluetoothCentral,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray?
            ): GattStatus {
                if (characteristic.uuid.toString().lowercase() == CHARACTERISTIC_UUID.lowercase()) {
                    wholeData += value?.dropLast(1)?.toByteArray() ?: byteArrayOf()

                    if ((value?.size ?: 0) < chunkSize && value?.lastOrNull() == 0.toByte()) {
                        logger.i("share: read: wrong chunk size: ${value.size}")
                    }
                    logger.i("share: read: chunk size: ${value?.size ?: 0}")
                    logger.i("share: read: chunk: ${String(value ?: byteArrayOf(), Charsets.UTF_8)}")
                    logger.i("share: read: whole data: ${String(wholeData, Charsets.UTF_8)}")

                    throttle?.dispose()
                    throttle = io.reactivex.Observable.timer(1000, TimeUnit.MILLISECONDS)
                        .subscribe { doHandling(String(wholeData, Charsets.UTF_8)) }
                }

                return GattStatus.SUCCESS
            }

            private fun doHandling(string: String): GattStatus {
                logger.i("share: handle: url: $string")

                val handled = runCatching { deeplinkHandler.handle(string) }.getOrNull()

                logger.i("share: handle: handled: $handled")

                if (handled != null && handled is DeepLink.Contacts) {
                    onReceived.invoke(handled.contacts)
                }
                wholeData = byteArrayOf()
                return if (handled != null) GattStatus.SUCCESS else GattStatus.INVALID_HANDLE
            }

            override fun onCharacteristicRead(bluetoothCentral: BluetoothCentral, characteristic: BluetoothGattCharacteristic): ReadResponse {
                return if (characteristic.uuid.toString().lowercase() == TRANSACTION_DATA_UUID.lowercase()) {
                    initiateReading()
                    return doRead()
                } else super.onCharacteristicRead(bluetoothCentral, characteristic)
            }

            fun initiateReading() {
                if (shareChunkedData.isNotEmpty()) return
                val myWalletAddress = TariWalletAddress().apply {
                    hexString = sharedPrefsRepository.publicKeyHexString.orEmpty()
                    emojiId = sharedPrefsRepository.emojiId.orEmpty()
                }
                val data = deeplinkHandler.getDeeplink(
                    DeepLink.UserProfile(
                        sharedPrefsRepository.publicKeyHexString.orEmpty(),
                        ContactDto.normalizeAlias(sharedPrefsRepository.name.orEmpty(), myWalletAddress),
                    )
                )
                logger.i("contactlessPayment: read: whole data: $data")
                val chunked = data.toByteArray(Charsets.UTF_8).toList().chunked(chunkSize)
                shareChunkedData =
                    chunked.mapIndexed { index, items -> (items + if (index == chunked.size - 1) 0 else 1).toByteArray() }.toMutableList()
            }

            fun doRead(): ReadResponse {
                val data = shareChunkedData.first()
                shareChunkedData = shareChunkedData.drop(1).toMutableList()
                logger.i("contactlessPayment: read: whole data: ${String(data, Charsets.UTF_8)}")
                return ReadResponse(GattStatus.SUCCESS, data)
            }
        }

        val myService = BluetoothGattService(UUID.fromString(SERVICE_UUID), BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val myCharacteristic = BluetoothGattCharacteristic(
            UUID.fromString(CHARACTERISTIC_UUID),
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        val myProfileCharacteristic = BluetoothGattCharacteristic(
            UUID.fromString(TRANSACTION_DATA_UUID),
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        myService.addCharacteristic(myCharacteristic)
        myService.addCharacteristic(myProfileCharacteristic)

        val manager = BluetoothPeripheralManager(fragappCompatActivity!!, bluetoothManager!!, callback)
        try {
            manager.add(myService)
        } catch (e: Throwable) {
            logger.i("share: add service: failed to add service: $e")
        }

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
        manager.startAdvertising(settings, data, data)
    }

    private fun stopReceiving() {
        bluetoothAdapter ?: return
        val bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser

        @Suppress("MissingPermission")
        bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)

        bluetoothGattServer?.let {
            @Suppress("MissingPermission")
            it.clearServices()
            @Suppress("MissingPermission")
            it.close()
        }
        bluetoothGattServer = null
    }
}