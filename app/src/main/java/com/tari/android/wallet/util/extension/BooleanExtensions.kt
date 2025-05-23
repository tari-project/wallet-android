package com.tari.android.wallet.util.extension

fun Boolean?.isFalse(): Boolean = this == false

fun Boolean?.isNotFalse(): Boolean = this != false

fun Boolean?.isTrue(): Boolean = this == true

fun Boolean?.isNotTrue(): Boolean = this != true