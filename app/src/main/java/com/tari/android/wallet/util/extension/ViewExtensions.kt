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

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.animation.addListener
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.FragmentTransaction
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.util.Constants
import java.lang.ref.WeakReference

private val logger
    get() = Logger.t("ViewExtensions")

fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun View.gone() {
    this.visibility = View.GONE
}

fun View.setVisible(visible: Boolean, hideState: Int = View.GONE) {
    visibility = if (visible) View.VISIBLE else hideState
}

/**
 * Sets the size of a TextView to the measured size of its contents
 * taking into account the text size and the typeface.
 */
fun TextView.setWidthAndHeightToMeasured() {
    measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
    setLayoutWidth(measuredWidth)
    setLayoutHeight(measuredHeight)
}

/**
 * Sets text size in pixel units.
 */
fun TextView.setTextSizePx(sizePx: Float) = setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx)

/**
 * @return first child of the view group, null if no children
 */
fun ViewGroup.getFirstChild(): View? = if (childCount > 0) this.getChildAt(0) else null

/**
 * @return last child of the view group, null if no children
 */
fun ViewGroup.getLastChild(): View? = if (childCount > 0) this.getChildAt(childCount - 1) else null

/**
 * Scroll to the top of the scroll view.
 */
fun ScrollView.scrollToTop() = scrollTo(0, 0)

/**
 * Scroll to the bottom of the scroll view.
 */
fun ScrollView.scrollToBottom() {
    val lastChild = getChildAt(childCount - 1)
    val bottom = lastChild.bottom + paddingBottom
    val delta = bottom - (scrollY + height)
    smoothScrollBy(0, delta)
}

fun View.doOnGlobalLayout(block: () -> Unit) {
    this.viewTreeObserver.addOnGlobalLayoutListener(
        object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                this@doOnGlobalLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                block()
            }
        })
}

fun View.setTopMargin(margin: Int) {
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        val layoutParams = layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.topMargin = margin
        this.layoutParams = layoutParams
    }
}

fun View.setBottomMargin(margin: Int) {
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        val layoutParams = layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.bottomMargin = margin
        this.layoutParams = layoutParams
    }
}

fun View.postDelayed(timeMillis: Long, action: () -> Unit) = this.postDelayed(action, timeMillis)

fun View.string(@StringRes id: Int): String = context.string(id)

fun View.string(@StringRes id: Int, vararg formatArgs: Any): String = context.string(id, *formatArgs)

fun View.color(@ColorRes id: Int): Int = context.color(id)

fun View.dimen(@DimenRes id: Int): Float = context.dimen(id)

fun View.dimenPx(@DimenRes id: Int): Int = context.dimenPx(id)

fun View.drawable(@DrawableRes id: Int): Drawable? = context.drawable(id)

fun View.setOnThrottledClickListener(action: (View) -> Unit) = this.setOnClickListener(ThrottleClick(action))

class ThrottleClick(private val delegate: (View) -> Unit) : View.OnClickListener {
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

fun View.getBottomMargin() = (layoutParams as ViewGroup.MarginLayoutParams).bottomMargin

fun View.setStartMargin(margin: Int) {
    withMargin {
        it.marginStart = margin
    }
}

fun View.setEndMargin(margin: Int) {
    withMargin {
        it.marginEnd = margin
    }
}

fun View.withMargin(action: (ViewGroup.MarginLayoutParams) -> Unit) {
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        val layoutParams = this.layoutParams as ViewGroup.MarginLayoutParams
        action(layoutParams)
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

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
fun AppCompatEditText.setTextSilently(newText: String) {
    if (text.toString() != newText) {
        runCatching {
            var selectionStartPoint = selectionStart
            var selectionEndPoint = selectionEnd
            setText(newText)
            selectionEndPoint = selectionEnd.coerceAtMost(newText.length)
            selectionStartPoint = selectionStart.coerceAtMost(selectionEnd)
            setSelection(selectionStartPoint, selectionEndPoint)
        }
    }
}

fun AppCompatEditText.setSelectionToEnd() {
    runCatching {
        val selectionEndPoint = text.toString().length
        setSelection(selectionEndPoint, selectionEndPoint)
    }
}

/**
 * Makes the given view clickable again.
 */
private class ClickEnablingRunnable(view: View) : Runnable {

    private val viewWR: WeakReference<View> = WeakReference(view)

    override fun run() {
        viewWR.get()?.run {
            runCatching {
                isClickable = true
            }
        }
    }
}

// TODO we don't stop the animation when the view is detached from window. May cause memory leaks.
fun View.animateClick(onEnd: (Animator) -> Unit = {}) {
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

fun FragmentTransaction.addEnterLeftAnimation(): FragmentTransaction {
    this.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
    return this
}

fun View.obtain(attrs: AttributeSet, styleable: IntArray): TypedArray = context.theme.obtainStyledAttributes(attrs, styleable, 0, 0)

fun TypedArray.runRecycle(action: TypedArray.() -> Unit) {
    try {
        action.invoke(this)
    } catch (e: Throwable) {
        logger.e(e, "Error while running action on TypedArray")
    } finally {
        recycle()
    }
}

fun Context.colorFromAttribute(attribute: Int): Int {
    val attributes = obtainStyledAttributes(intArrayOf(attribute))
    val dimension = attributes.getColor(0, 0)
    attributes.recycle()
    return dimension
}

fun Animator.removeListenersAndCancel() {
    this.safeCastTo<ValueAnimator>()?.removeAllUpdateListeners()
    removeAllListeners()
    cancel()
}