/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.ffi

/**
 * @author The Tari Development Team
 */
internal class NetAddressString constructor() {

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
            throw FFIException(message = "String is not valid Address")
        }
        if (port >= 0) {
            addressPort = port
        } else {
            throw FFIException(message = "Port is not valid Port")
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