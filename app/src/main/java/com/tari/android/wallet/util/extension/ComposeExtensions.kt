package com.tari.android.wallet.util.extension

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue


fun TextFieldValue.newValueIfChanged(newValue: String?) =
    if (newValue != null && newValue != text) copy(text = newValue, selection = TextRange(newValue.length)) else this