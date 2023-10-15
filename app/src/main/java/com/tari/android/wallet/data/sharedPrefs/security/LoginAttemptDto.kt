package com.tari.android.wallet.data.sharedPrefs.security

class LoginAttemptDto(val time: Long, val isSuccessful: Boolean)

class LoginAttemptList : ArrayList<LoginAttemptDto>()

fun LoginAttemptList?.orEmpty() : LoginAttemptList = this ?: LoginAttemptList()