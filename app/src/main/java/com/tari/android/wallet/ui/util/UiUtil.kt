/**
 * Copyright 2019 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:

 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.

 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.

 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.

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

package com.tari.android.wallet.ui.util

import android.app.Activity
import android.content.Context
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.os.Handler

import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import androidx.annotation.NonNull
import java.lang.ref.WeakReference

/**
 * Contains UI utility functions.
 *
 * @author Kutsal Kaan Bilgin
 */
class UiUtil {

    // enabled view clickability after a disable
    private val clickEnablingHandler = Handler()

    fun setWidthAndHeight(
        @NonNull view: View,
        @NonNull newWidth: Int,
        @NonNull newHeight: Int
    ) {
        if (view.layoutParams is ViewGroup.MarginLayoutParams) {
            val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = newWidth
            layoutParams.height = newHeight
            view.layoutParams = layoutParams
        }
    }

    fun setHeight(
        @NonNull view: View,
        @NonNull newHeight: Int
    ) {
        if (view.layoutParams is ViewGroup.MarginLayoutParams) {
            val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.height = newHeight
            view.layoutParams = layoutParams
        }
    }

    fun setLeftMargin(
        @NonNull view: View,
        @NonNull newLeftMargin: Int
    ) {
        if (view.layoutParams is ViewGroup.MarginLayoutParams) {
            val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.leftMargin = newLeftMargin
            view.layoutParams = layoutParams
        }
    }

    fun setProgressBarColor(
        @NonNull progressBar: ProgressBar,
        @NonNull color: Int
    ) {
        val colorFilter = BlendModeColorFilter(color, BlendMode.SRC_ATOP)
        progressBar.indeterminateDrawable.mutate().colorFilter = colorFilter
    }

    fun hideKeyboard(
        @NonNull activity: Activity
    ) {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = activity.currentFocus
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * Makes the view unclickable for a second, then makes it
     * clickable again.
     */
    fun temporarilyDisableClick(
        @NonNull view: View
    ) {
        view.isClickable = false
        clickEnablingHandler.postDelayed(
            ClickEnablingRunnable(view),
            1000
        )
    }

    /**
     * Makes the given view clickable again.
     */
    private class ClickEnablingRunnable(@NonNull view: View) : Runnable {

        private val viewWR: WeakReference<View> = WeakReference(view)

        override fun run() {
            val view = viewWR.get()
            if (view != null) {
                try {
                    view.isClickable = true
                } catch (e: Throwable) {
                    // no-op
                }
            }
        }

    }

}