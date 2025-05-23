package com.tari.android.wallet.util

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.orhanobut.logger.Logger
import java.util.EnumMap

object QrUtil {
    private val logger
        get() = Logger.t(QrUtil::class.simpleName)

    @Throws(Exception::class)
    fun getQrEncodedBitmap(content: String, size: Int): Bitmap = try {
        val hints: MutableMap<EncodeHintType, String> = EnumMap(EncodeHintType::class.java)
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        val barcodeEncoder = BarcodeEncoder()
        val map = barcodeEncoder.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        barcodeEncoder.createBitmap(map)
    } catch (e: Exception) {
        error("Failed to encode QR code with value: $content \nError: ${e.message}")
    }

    fun getQrEncodedBitmapOrNull(content: String, size: Int): Bitmap? =
        runCatching {
            getQrEncodedBitmap(content, size)
        }.getOrElse {
            logger.e("Failed to encode QR code with value: $content \nError: ${it.message}")
            null
        }
}