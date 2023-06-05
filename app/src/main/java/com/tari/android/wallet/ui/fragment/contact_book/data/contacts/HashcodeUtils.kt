package com.tari.android.wallet.ui.fragment.contact_book.data.contacts

object HashcodeUtils {
    fun generate(vararg args: Any?): Int {
        var result = 1
        for (element in args) {
            result = 31 * result + element.hashCode()
        }
        return result
    }
}