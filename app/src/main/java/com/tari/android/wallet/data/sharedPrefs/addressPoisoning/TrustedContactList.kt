package com.tari.android.wallet.data.sharedPrefs.addressPoisoning

class TrustedContactList(trustedContacts: List<String>) : ArrayList<String>(trustedContacts) {
    constructor() : this(emptyList())
}