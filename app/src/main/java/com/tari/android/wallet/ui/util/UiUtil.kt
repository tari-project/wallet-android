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
package com.tari.android.wallet.ui.util

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ProgressBar
import androidx.annotation.NonNull
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.tari.android.wallet.util.Constants
import java.lang.ref.WeakReference

/**
 * Contains UI utility functions.
 *
 * @author The Tari Development Team
 */
internal object UiUtil {

    // enabled view clickability after a disable
    private val clickEnablingHandler = Handler()

    fun setWidth(
        @NonNull view: View,
        @NonNull newWidth: Int
    ) {
        if (view.layoutParams is ViewGroup.MarginLayoutParams) {
            val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = newWidth
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

    fun getHeight(
        @NonNull view: View
    ): Int {
        val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
        return layoutParams.height
    }

    fun setTopMargin(
        @NonNull view: View,
        @NonNull newTopMargin: Int
    ) {
        if (view.layoutParams is ViewGroup.MarginLayoutParams) {
            val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.topMargin = newTopMargin
            view.layoutParams = layoutParams
        }
    }

    fun setBottomMargin(
        @NonNull view: View,
        @NonNull newBottomMargin: Int
    ) {
        if (view.layoutParams is ViewGroup.MarginLayoutParams) {
            val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.bottomMargin = newBottomMargin
            view.layoutParams = layoutParams
        }
    }

    fun getBottomMargin(
        @NonNull view: View
    ): Int {
        val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
        return layoutParams.bottomMargin
    }

    fun getStartMargin(
        @NonNull view: View
    ): Int {
        val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
        return layoutParams.marginStart
    }

    fun setStartMargin(
        @NonNull view: View,
        @NonNull newMargin: Int
    ) {
        if (view.layoutParams is ViewGroup.MarginLayoutParams) {
            val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.marginStart = newMargin
            view.layoutParams = layoutParams
        }
    }

    fun setProgressBarColor(
        progressBar: ProgressBar,
        color: Int
    ) {
        progressBar.indeterminateDrawable
            .mutate().colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            color,
            BlendModeCompat.SRC_IN
        )
    }

    @Suppress("unused")
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

    fun showKeyboard(@NonNull activity: Activity) {
        var view = activity.currentFocus
        if (view == null) {
            view = View(activity)
        }
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
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
            Constants.UI.xLongDurationMs
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

    /**
     * @param resourceId identifies an application resource
     * @return the Uri by which the application resource is accessed
     */
    fun Context.getResourceUri(resourceId: Int): Uri = Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(packageName)
        .path(resourceId.toString())
        .build()

    /*
    * Animation for button click
    * */
    fun animateButtonClick(button: Button): AnimatorSet {
        val scaleDownBtnAnim = ValueAnimator.ofFloat(
            Constants.UI.Button.clickScaleAnimFullScale,
            Constants.UI.Button.clickScaleAnimSmallScale
        )
        scaleDownBtnAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val scale = valueAnimator.animatedValue as Float
            button.scaleX = scale
            button.scaleY = scale
        }
        scaleDownBtnAnim.duration = Constants.UI.Button.clickScaleAnimDurationMs
        scaleDownBtnAnim.startDelay = Constants.UI.Button.clickScaleAnimStartOffset
        scaleDownBtnAnim.interpolator = DecelerateInterpolator()

        val scaleUpBtnAnim = ValueAnimator.ofFloat(
            Constants.UI.Button.clickScaleAnimSmallScale,
            Constants.UI.Button.clickScaleAnimFullScale
        )
        scaleUpBtnAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val scale = valueAnimator.animatedValue as Float
            button.scaleX = scale
            button.scaleY = scale
        }
        scaleUpBtnAnim.duration = Constants.UI.Button.clickScaleAnimReturnDurationMs
        scaleUpBtnAnim.startDelay = Constants.UI.Button.clickScaleAnimReturnStartOffset
        scaleUpBtnAnim.interpolator = AccelerateInterpolator()

        val animSet = AnimatorSet()
        animSet.playSequentially(scaleDownBtnAnim, scaleUpBtnAnim)
        animSet.start()
        return animSet
    }

    /**
     * @param content content to encode
     * @param size bitmap size
     * @return the encoded bitmap
     **/
    fun getQREncodedBitmap(content: String, size: Int): Bitmap? {
        try {
            val barcodeEncoder = BarcodeEncoder()
            val hints: HashMap<EncodeHintType, String> = HashMap()
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8";

            val map = barcodeEncoder.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            return barcodeEncoder.createBitmap(map)
        } catch (e: Exception) {
        }
        return null
    }

}