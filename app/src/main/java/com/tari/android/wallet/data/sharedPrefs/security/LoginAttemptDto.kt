package com.tari.android.wallet.data.sharedPrefs.security

class LoginAttemptDto(val timeInMills: Long, val isSuccessful: Boolean)

class LoginAttemptList(loginAttempts: List<LoginAttemptDto>) : ArrayList<LoginAttemptDto>(loginAttempts) {
    constructor() : this(emptyList())
}