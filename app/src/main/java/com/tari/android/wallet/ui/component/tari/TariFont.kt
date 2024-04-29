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
package com.tari.android.wallet.ui.component.tari

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet

/**
 * Custom font enumeration - used in layout files.
 *
 * @author The Tari Development Team
 */
enum class TariFont(private val fileName: String) {

    // font files
    AVENIR_LT_STD_BLACK("fonts/AvenirLTStd-Black.otf"),
    AVENIR_LT_STD_HEAVY("fonts/AvenirLTStd-Heavy.otf"),
    AVENIR_LT_STD_MEDIUM("fonts/AvenirLTStd-Medium.otf"),
    AVENIR_LT_STD_ROMAN("fonts/AvenirLTStd-Roman.otf"),
    AVENIR_NEXT_LT_PRO_REGULAR("fonts/AvenirNextLTPro-Regular.otf"),
    AVENIR_LT_STD_LIGHT("fonts/AvenirLTStd-Light.otf");

    fun asTypeface(context: Context): Typeface {
        return Typeface.createFromAsset(context.assets, fileName)
    }

    companion object {
        private const val sScheme = "http://schemas.android.com/apk/res-auto"
        private const val sAttribute = "customFont"

        /**
         * Get font from attribute set. The default font is Medium.
         */
        fun getFromAttributeSet(context: Context, attr: AttributeSet): Typeface {
            val fontName = attr.getAttributeValue(sScheme, sAttribute)?.toInt()
            val uiFont = fontName?.let { UIFont.entries[it] } ?: UIFont.Medium
            return uiFont.toTariFont().asTypeface(context)
        }
    }
}