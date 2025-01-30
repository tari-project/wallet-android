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
package com.tari.android.wallet.util.extension

import android.content.Context
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AlignmentSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import com.tari.android.wallet.ui.component.tari.TariFont
import com.tari.android.wallet.ui.component.tari.TariLetterSpacingSpan
import com.tari.android.wallet.ui.component.tari.TariTypefaceSpan

/**
 * Used to apply partial font styles to a string.
 *
 * @param defaultFont the font to be applied to the whole string
 * @param search the substring to whose occurrences the custom font should be applied
 * @param tariFont font to be applied to the occurrences of the search string
 * @param applyToOnlyFirstOccurrence whether customFont should be applied only to the first occurrence
 * @return spannable string
 */
fun String.applyFontStyle(
    context: Context,
    defaultFont: TariFont,
    search: List<String>,
    tariFont: TariFont,
    applyToOnlyFirstOccurrence: Boolean = false
): SpannableString {
    val defaultTypeface = defaultFont.asTypeface(context)
    val spannableString = SpannableString(this)
    spannableString.setSpan(TariTypefaceSpan("", defaultTypeface), 0, length, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
    search.forEach { spannableString.applyTypefaceStyle(it, tariFont.asTypeface(context), applyToOnlyFirstOccurrence) }
    return spannableString
}

/**
 * Similar to applyFontStyle above, but applied to color instead.
 */
fun String.applyColorStyle(defaultColor: Int, search: List<String>, styleColor: Int, applyToOnlyFirstOccurrence: Boolean = false): SpannableString {
    val spannableString = SpannableString(this)
    spannableString.setSpan(ForegroundColorSpan(defaultColor), 0, length, Spanned.SPAN_INTERMEDIATE)
    search.forEach { spannableString.applyColorStyle(it, styleColor, applyToOnlyFirstOccurrence) }
    return spannableString
}

/**
 * Helper function to apply color style to a spannable string.
 */
fun SpannableString.applyColorStyle(search: String, color: Int, applyToOnlyFirstOccurrence: Boolean = false) {
    var index = this.indexOf(search)
    while (index >= 0) {
        setSpan(ForegroundColorSpan(color), index, index + search.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (applyToOnlyFirstOccurrence) {
            break
        }
        index = this.indexOf(search, index + 1)
    }
}

/**
 * Helper function to apply typeface style to a spannable string.
 */
fun SpannableString.applyTypefaceStyle(search: String, typeface: Typeface, applyToOnlyFirstOccurrence: Boolean = false) {
    var index = this.indexOf(search)
    while (index >= 0) {
        setSpan(TariTypefaceSpan("", typeface), index, index + search.length, Spanned.SPAN_INTERMEDIATE)
        if (applyToOnlyFirstOccurrence) {
            break
        }
        index = this.indexOf(search, index + 1)
    }
}

/**
 * Helper function to apply relative text size style to a spannable string.
 */
fun SpannableString.applyRelativeTextSizeStyle(search: String, relativeTextSize: Float, applyToOnlyFirstOccurrence: Boolean = false) {
    var index = this.indexOf(search)
    while (index >= 0) {
        setSpan(RelativeSizeSpan(relativeTextSize), index, index + search.length, Spanned.SPAN_INTERMEDIATE)
        if (applyToOnlyFirstOccurrence) {
            break
        }
        index = this.indexOf(search, index + 1)
    }
}

/**
 * Helper function to apply letter spacing to a spannable string.
 */
fun SpannableString.applyLetterSpacingStyle(search: String, letterSpacing: Float, applyToOnlyFirstOccurrence: Boolean = false) {
    var index = this.indexOf(search)
    while (index >= 0) {
        setSpan(TariLetterSpacingSpan(letterSpacing), index, index + search.length, Spanned.SPAN_INTERMEDIATE)
        if (applyToOnlyFirstOccurrence) {
            break
        }
        index = this.indexOf(search, index + 1)
    }
}

fun SpannableString.applyCenterAlignment() {
    setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, length, Spanned.SPAN_INTERMEDIATE)
}

/**
 * Helper function to apply bold style to a spannable string.
 * If no words are provided, the whole string will be made bold.
 */
fun String.makeTextBold(context: Context, vararg wordsToBeBold: String): SpannableString = this.applyFontStyle(
    context = context,
    defaultFont = TariFont.LIGHT,
    search = if (wordsToBeBold.isEmpty()) listOf(this) else wordsToBeBold.toList(),
    tariFont = TariFont.HEAVY,
)
