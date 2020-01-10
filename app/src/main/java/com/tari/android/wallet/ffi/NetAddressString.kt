package com.tari.android.wallet.ffi

import java.util.*

class NetAddressString constructor() {

    private val pattern = StringBuilder()
        .append("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.")
        .append("([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.")
        .append("([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.")
        .append("([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")
        .toString()
        .toRegex()
    private var address: String
    private var addressPort: Int

    init {
        address = "0.0.0.0"
        addressPort = 0

    }

    constructor(string: String, port: Int) : this() {
        if (pattern.matches(string)) {
            address = string
        } else {
            throw InvalidPropertiesFormatException("String is not valid Address")
        }
        if (port >= 0) {
            addressPort = port
        } else {
            throw InvalidPropertiesFormatException("Port is not valid Port")
        }
    }

    override fun toString(): String {
        val result = StringBuilder()
            .append("/ip4/")
            .append(address)
            .append("/tcp/")
            .append(addressPort)
        return result.toString()
    }


}