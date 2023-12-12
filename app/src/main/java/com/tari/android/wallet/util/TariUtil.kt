package com.tari.android.wallet.util

import java.math.BigInteger

fun String?.parseToBigInteger(): BigInteger = runCatching { BigInteger(this) }.getOrNull() ?: BigInteger.ZERO