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
package com.tari.android.wallet.extension

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.URLSpan
import com.tari.android.wallet.ui.component.CustomFont
import com.tari.android.wallet.ui.component.CustomTypefaceSpan
import com.tari.android.wallet.ui.component.LetterSpacingSpan

/**
 * Process the URLs in the given spannable string.
 *
 * @param search substring to be marked as a link
 * @param url locator
 * @param applyToOnlyFirstOccurence whether the span should be applied only to the first occurence
 */
internal fun SpannableString.applyURLStyle(
    search: String,
    url: String,
    applyToOnlyFirstOccurence: Boolean = false
): SpannableString {
    var index = this.indexOf(search)
    val span = URLSpan(url)
    while (index >= 0) {
        setSpan(
            span,
            index,
            index + search.length,
            Spanned.SPAN_EXCLUSIVE_INCLUSIVE
        )
        if (applyToOnlyFirstOccurence) {
            break
        }
        index = this.indexOf(search, index + 1)
    }
    return this
}

/**
 * Used to apply partial font styles to a string.
 *
 * @param defaultFont the font to be applied to the whole string
 * @param search the substring to whose occurences the custom font should be applied
 * @param customFont font to be applied to the occurences of the search string
 * @param applyToOnlyFirstOccurence whether customFont should be applied only to the first occurence
 * @return spannable string
 */
internal fun String.applyFontStyle(
    context: Context,
    defaultFont: CustomFont,
    search: String,
    customFont: CustomFont,
    applyToOnlyFirstOccurence: Boolean = false
): SpannableString {
    val defaultTypeface = defaultFont.asTypeface(context)
    val spannableString = SpannableString(this)
    spannableString.setSpan(
        CustomTypefaceSpan("", defaultTypeface),
        0,
        length,
        Spanned.SPAN_EXCLUSIVE_INCLUSIVE
    )
    spannableString.applyTypefaceStyle(
        search,
        customFont.asTypeface(context),
        applyToOnlyFirstOccurence
    )
    return spannableString
}

/**
 * Similar to applyFontStyle above, but applied to color instead.
 */
internal fun String.applyColorStyle(
    defaultColor: Int,
    search: String,
    styleColor: Int,
    applyToOnlyFirstOccurence: Boolean = false
): SpannableString {
    val spannableString = SpannableString(this)
    spannableString.setSpan(
        ForegroundColorSpan(defaultColor),
        0,
        length,
        Spanned.SPAN_INTERMEDIATE
    )
    spannableString.applyColorStyle(
        search,
        styleColor,
        applyToOnlyFirstOccurence
    )
    return spannableString
}

/**
 * Helper function to apply color style to a spannable string.
 */
private fun SpannableString.applyColorStyle(
    search: String,
    color: Int,
    applyToOnlyFirstOccurence: Boolean = false
) {
    var index = this.indexOf(search)
    while (index >= 0) {
        setSpan(
            ForegroundColorSpan(color),
            index,
            index + search.length,
            Spanned.SPAN_INTERMEDIATE
        )
        if (applyToOnlyFirstOccurence) {
            break
        }
        index = this.indexOf(search, index + 1)
    }
}

/**
 * Helper function to apply typeface style to a spannable string.
 */
private fun SpannableString.applyTypefaceStyle(
    search: String,
    typeface: Typeface,
    applyToOnlyFirstOccurence: Boolean = false
) {
    var index = this.indexOf(search)
    while (index >= 0) {
        setSpan(
            CustomTypefaceSpan("", typeface),
            index,
            index + search.length,
            Spanned.SPAN_INTERMEDIATE
        )
        if (applyToOnlyFirstOccurence) {
            break
        }
        index = this.indexOf(search, index + 1)
    }
}

/**
 * Helper function to apply relative text size style to a spannable string.
 */
internal fun SpannableString.applyRelativeTextSizeStyle(
    search: String,
    relativeTextSize: Float,
    applyToOnlyFirstOccurence: Boolean = false
) {
    var index = this.indexOf(search)
    while (index >= 0) {
        setSpan(
            RelativeSizeSpan(relativeTextSize),
            index,
            index + search.length,
            Spanned.SPAN_INTERMEDIATE
        )
        if (applyToOnlyFirstOccurence) {
            break
        }
        index = this.indexOf(search, index + 1)
    }
}

/**
 * Helper function to apply letter spacing to a spannable string.
 */
internal fun SpannableString.applyLetterSpacingStyle(
    search: String,
    letterSpacing: Float,
    applyToOnlyFirstOccurence: Boolean = false
) {
    var index = this.indexOf(search)
    while (index >= 0) {
        setSpan(
            LetterSpacingSpan(letterSpacing),
            index,
            index + search.length,
            Spanned.SPAN_INTERMEDIATE
        )
        if (applyToOnlyFirstOccurence) {
            break
        }
        index = this.indexOf(search, index + 1)
    }
}