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
import androidx.annotation.FontRes
import com.tari.android.wallet.R

/**
 * Custom font enumeration - used in layout files.
 *
 * @author The Tari Development Team
 */

enum class TariFont(@param:FontRes private val fontRes: Int) {

    // font files
    BLACK(R.font.poppins_semibold),
    HEAVY(R.font.poppins_semibold),
    MEDIUM(R.font.poppins_medium),
    ROMAN(R.font.poppins_regular),
    REGULAR(R.font.poppins_regular),
    LIGHT(R.font.poppins_light);

    fun asTypeface(context: Context): Typeface {
        return context.resources.getFont(fontRes)
    }

    companion object {
        private const val SCHEME = "http://schemas.android.com/apk/res-auto"
        private const val ATTRIBUTE = "customFont"

        /**
         * Get font from attribute set. The default font is Medium.
         */
        fun getFromAttributeSet(context: Context, attr: AttributeSet): Typeface {
            val fontName = attr.getAttributeValue(SCHEME, ATTRIBUTE)?.toInt()
            val uiFont = fontName?.let { UIFont.entries[it] } ?: UIFont.Medium
            return uiFont.toTariFont().asTypeface(context)
        }
    }
}