package com.tari.android.wallet.infrastructure.nfc

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkHandler
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.home.HomeActivity
import java.nio.charset.Charset
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class TariNFCAdapter @Inject constructor(
    val deeplinkHandler: DeeplinkHandler,
    val sharedPrefsRepository: SharedPrefsRepository
) : CommonViewModel() {

    val MIME_TEXT_PLAIN = "text/plain"

    var context: AppCompatActivity? = null

    var onSuccessSharing: () -> Unit = {}
    var onFailedSharing: (String) -> Unit = {}

    var onReceived: (List<DeepLink.Contacts.DeeplinkContact>) -> Unit = {}

    fun onNewIntent(intent: Intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.also { rawMessages ->
                val messages: List<NdefMessage> = rawMessages.map { it as NdefMessage }
                val first = messages.firstOrNull() ?: return
                val actualMessage = first.records.firstOrNull()?.payload ?: return
                val payload = String(actualMessage, Charsets.UTF_16)
                doHandling(payload)
            }
        }
    }

    private fun doHandling(string: String) {
        val handled = runCatching { deeplinkHandler.handle(string) }.getOrNull()

        if (handled != null && handled is DeepLink.Contacts) {
            onReceived.invoke(handled.contacts)
        }
    }

    fun stopSharing() {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(context!!)
        nfcAdapter.setNdefPushMessageCallback(null, context)
        nfcAdapter.setOnNdefPushCompleteCallback(null, context)
    }

    fun startSharing(data: String) {
        val adapter = NfcAdapter.getDefaultAdapter(context!!) ?: return
        if (adapter.isEnabled.not()) {
            showNFCSettings()
            return
        }
        createTag(data)
    }

    private fun createTag(data: String) {
        val outBytes: ByteArray = data.toByteArray(Charsets.UTF_16)
        val outRecord = NdefRecord.createMime(MIME_TEXT_PLAIN, outBytes)

        val nfcAdapter = NfcAdapter.getDefaultAdapter(context!!)
        nfcAdapter.enableForegroundNdefPush(context, NdefMessage(outRecord))
        nfcAdapter.setOnNdefPushCompleteCallback({ onSuccessSharing() }, context)
        nfcAdapter.setNdefPushMessageCallback({ NdefMessage(outRecord) }, context)
    }

    fun enableForegroundDispatch(activity: AppCompatActivity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return

        val intent = Intent(activity.applicationContext, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(activity.applicationContext, 0, intent, FLAG_MUTABLE)
        val filters = arrayOfNulls<IntentFilter>(1)
        val techList = arrayOf<Array<String>>()
        filters[0] = IntentFilter()
        filters[0]!!.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED)
        filters[0]!!.addCategory(Intent.CATEGORY_DEFAULT)
        try {
            filters[0]!!.addDataType(MIME_TEXT_PLAIN)
        } catch (ex: IntentFilter.MalformedMimeTypeException) {
            throw RuntimeException("Check your MIME type")
        }
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList)
    }

    fun createTextRecord(payload: String, locale: Locale, encodeInUtf8: Boolean): NdefRecord {
        val langBytes = locale.language.toByteArray(Charset.forName("US-ASCII"))
        val utfEncoding = if (encodeInUtf8) Charset.forName("UTF-8") else Charset.forName("UTF-16")
        val textBytes = payload.toByteArray(utfEncoding)
        val utfBit: Int = if (encodeInUtf8) 0 else 1 shl 7
        val status = (utfBit + langBytes.size).toChar()
        val data = ByteArray(1 + langBytes.size + textBytes.size)
        data[0] = status.code.toByte()
        System.arraycopy(langBytes, 0, data, 1, langBytes.size)
        System.arraycopy(textBytes, 0, data, 1 + langBytes.size, textBytes.size)
        return NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), data)
    }

    fun disableForegroundDispatch(activity: AppCompatActivity?) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        adapter.disableForegroundDispatch(activity)
    }

    fun isNFCAvailable(): Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(context!!)
        return adapter != null && adapter.isEnabled
    }

    fun isNFCSupported(): Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(context!!)
        val sdk = Build.VERSION.SDK_INT
        return adapter != null && sdk < Build.VERSION_CODES.Q
    }

    fun showNFCSettings() {
        val intent = Intent(android.provider.Settings.ACTION_NFC_SETTINGS)
        context?.startActivity(intent)
    }
}