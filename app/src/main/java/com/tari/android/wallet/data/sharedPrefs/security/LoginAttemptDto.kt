package com.tari.android.wallet.data.sharedPrefs.security

class LoginAttemptDto(val timeInMills: Long, val isSuccessful: Boolean)

class LoginAttemptList : ArrayList<LoginAttemptDto>()

fun LoginAttemptList?.orEmpty() : LoginAttemptList = this ?: LoginAttemptList()