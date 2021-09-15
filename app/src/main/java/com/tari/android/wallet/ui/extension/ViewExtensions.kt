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
package com.tari.android.wallet.ui.extension

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.*
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.animation.addListener
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.dialog.BottomSlideDialog
import com.tari.android.wallet.util.Constants
import java.lang.ref.WeakReference
import android.animation.Animator as LegacyAnimator
import android.animation.Animator.AnimatorListener as LegacyAnimatorListener

internal fun RecyclerView.isScrolledToTop(): Boolean {
    val layoutManager = (layoutManager as? LinearLayoutManager) ?: return false
    if (layoutManager.childCount == 0) return true
    return (layoutManager.findFirstVisibleItemPosition() == 0
            && layoutManager.findViewByPosition(0)?.top == 0)
}

internal fun View.visible() {
    this.visibility = View.VISIBLE
}

internal fun View.invisible() {
    this.visibility = View.INVISIBLE
}

internal fun View.gone() {
    this.visibility = View.GONE
}

internal fun View.setVisible(visible: Boolean, hideState: Int = View.GONE) {
    visibility = if (visible) View.VISIBLE else hideState
}

/**
 * Given the context, displays the standard "no internet connection" dialog.
 */
internal fun showInternetConnectionErrorDialog(context: Context) {
    BottomSlideDialog(
        context = context,
        layoutId = R.layout.internet_connection_error_dialog,
        dismissViewId = R.id.internet_connection_error_dialog_txt_close
    ).show()
}

/**
 * Sets the width of a TextView to the measured width of its contents
 * taking into account the text size and the typeface.
 */
internal fun TextView.setWidthToMeasured() {
    measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
    setLayoutWidth(measuredWidth)
}

/**
 * Sets the size of a TextView to the measured size of its contents
 * taking into account the text size and the typeface.
 */
internal fun TextView.setWidthAndHeightToMeasured() {
    measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
    setLayoutWidth(measuredWidth)
    setLayoutHeight(measuredHeight)
}

/**
 * Sets text size in pixel units.
 */
internal fun TextView.setTextSizePx(sizePx: Float) {
    setTextSize(
        TypedValue.COMPLEX_UNIT_PX,
        sizePx
    )
}

/**
 * @return first child of the view group, null if no children
 */
internal fun ViewGroup.getFirstChild(): View? {
    return if (childCount > 0) {
        this.getChildAt(0)
    } else {
        null
    }
}

/**
 * @return last child of the view group, null if no children
 */
internal fun ViewGroup.getLastChild(): View? {
    return if (childCount > 0) {
        this.getChildAt(childCount - 1)
    } else {
        null
    }
}

/**
 * Scroll to the top of the scroll view.
 */
internal fun ScrollView.scrollToTop() {
    scrollTo(0, 0)
}

/**
 * Scroll to the bottom of the scroll view.
 */
internal fun ScrollView.scrollToBottom() {
    val lastChild = getChildAt(childCount - 1)
    val bottom = lastChild.bottom + paddingBottom
    val delta = bottom - (scrollY + height)
    smoothScrollBy(0, delta)
}

internal fun LottieAnimationView.addAnimatorListener(
    onStart: (LegacyAnimator?) -> Unit = {},
    onEnd: (LegacyAnimator?) -> Unit = {},
    onCancel: (LegacyAnimator?) -> Unit = {},
    onRepeat: (LegacyAnimator?) -> Unit = {}
) {
    addAnimatorListener(object : LegacyAnimatorListener {
        override fun onAnimationRepeat(animation: LegacyAnimator?) = onRepeat(animation)
        override fun onAnimationEnd(animation: LegacyAnimator?) = onEnd(animation)
        override fun onAnimationCancel(animation: LegacyAnimator?) = onCancel(animation)
        override fun onAnimationStart(animation: LegacyAnimator?) = onStart(animation)
    })
}

internal fun View.doOnGlobalLayout(block: () -> Unit) {
    this.viewTreeObserver.addOnGlobalLayoutListener(
        object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                this@doOnGlobalLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                block()
            }
        })
}

internal fun View.setTopMargin(margin: Int) {
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        val layoutParams = layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.topMargin = margin
        this.layoutParams = layoutParams
    }
}

internal fun View.setBottomMargin(margin: Int) {
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        val layoutParams = layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.bottomMargin = margin
        this.layoutParams = layoutParams
    }
}

internal fun View.postDelayed(timeMillis: Long, action: () -> Unit) =
    this.postDelayed(action, timeMillis)

internal fun View.string(@StringRes id: Int): String = context.string(id)

internal fun View.string(@StringRes id: Int, vararg formatArgs: Any): String =
    context.string(id, *formatArgs)

internal fun View.color(@ColorRes id: Int): Int = context.color(id)

internal fun View.dimen(@DimenRes id: Int): Float = context.dimen(id)

internal fun View.dimenPx(@DimenRes id: Int): Int = context.dimenPx(id)

internal fun View.drawable(@DrawableRes id: Int): Drawable? = context.drawable(id)

internal fun View.setOnThrottledClickListener(action: (View) -> Unit) {
    this.setOnClickListener(ThrottleClick(action))
}

internal class ThrottleClick(private val delegate: (View) -> Unit) : View.OnClickListener {
    override fun onClick(v: View?) {
        v?.temporarilyDisableClick()
        v?.let(delegate)
    }
}

// method name is this rather than "setWidth" because "setWidth" might conflict with View's
// subtypes intrinsic methods
fun View.setLayoutWidth(width: Int) {
    this.layoutParams?.let { this@setLayoutWidth.layoutParams = it.also { p -> p.width = width } }
}

// method name is this rather than "setHeight" because "setHeight" might conflict with View's
// subtypes intrinsic methods
fun View.setLayoutHeight(height: Int) {
    this.layoutParams?.let { this@setLayoutHeight.layoutParams = it.also { p -> p.height = height } }
}

fun View.setLayoutSize(width: Int, height: Int) {
    this.layoutParams?.let {
        this@setLayoutSize.layoutParams = it.also { p ->
            p.width = width
            p.height = height
        }
    }
}

fun View.layoutParamsHeight() = (layoutParams as ViewGroup.MarginLayoutParams).height

fun View.getBottomMargin() = (layoutParams as ViewGroup.MarginLayoutParams).bottomMargin

fun View.getStartMargin() = (layoutParams as ViewGroup.MarginLayoutParams).marginStart

fun View.setStartMargin(margin: Int) {
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        val layoutParams = this.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.marginStart = margin
        this.layoutParams = layoutParams
    }
}

fun ProgressBar.setColor(color: Int) {
    this.indeterminateDrawable
        .mutate()
        .colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
        color, BlendModeCompat.SRC_IN
    )
}

fun View.temporarilyDisableClick() {
    isClickable = false
    postDelayed(
        ClickEnablingRunnable(this),
        Constants.UI.xLongDurationMs
    )
}

fun AppCompatEditText.setTextSilently(newText: String) {
    if (text.toString() != newText) {
        try {
            var selectionStartPoint = selectionStart
            var selectionEndPoint = selectionEnd
            setText(newText)
            selectionEndPoint = Math.min(selectionEnd, newText.length)
            selectionStartPoint = Math.min(selectionStart, selectionEnd)
            setSelection(selectionStartPoint, selectionEndPoint)
        } catch (e: Throwable) {
            Logger.i(e.toString())
        }
    }
}

fun AppCompatEditText.setSelectionToEnd() {
    try {
        val selectionEndPoint = text.toString().length
        setSelection(selectionEndPoint, selectionEndPoint)
    } catch (e: Throwable) {
        Logger.i(e.toString())
    }
}

/**
 * Makes the given view clickable again.
 */
private class ClickEnablingRunnable(@NonNull view: View) : Runnable {

    private val viewWR: WeakReference<View> = WeakReference(view)

    override fun run() {
        viewWR.get()?.run {
            try {
                isClickable = true
            } catch (e: Throwable) {
                // no-op
            }
        }
    }
}

fun View.animateClick(onEnd: (android.animation.Animator) -> Unit = {}) {
    val scaleDownBtnAnim = ValueAnimator.ofFloat(
        Constants.UI.Button.clickScaleAnimFullScale,
        Constants.UI.Button.clickScaleAnimSmallScale
    )
    scaleDownBtnAnim.addUpdateListener { valueAnimator: ValueAnimator ->
        val scale = valueAnimator.animatedValue as Float
        scaleX = scale
        scaleY = scale
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
        scaleX = scale
        scaleY = scale
    }
    scaleUpBtnAnim.duration = Constants.UI.Button.clickScaleAnimReturnDurationMs
    scaleUpBtnAnim.startDelay = Constants.UI.Button.clickScaleAnimReturnStartOffset
    scaleUpBtnAnim.interpolator = AccelerateInterpolator()

    val animSet = AnimatorSet()
    animSet.addListener(onEnd = onEnd)
    animSet.playSequentially(scaleDownBtnAnim, scaleUpBtnAnim)
    animSet.start()
}
