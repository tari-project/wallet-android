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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.dialog.BottomSlideDialog
import com.tari.android.wallet.ui.util.UiUtil

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
 * Used for full-screen views.
 */
@SuppressLint("ObsoleteSdkInt")
internal fun Activity.makeStatusBarTransparent() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
            statusBarColor = Color.TRANSPARENT
        }
    }
}

/**
 * Sets the width of a TextView to the measured width of its contents
 * taking into account the text size and the typeface.
 */
internal fun TextView.setWidthToMeasured() {
    this.measure(
        View.MeasureSpec.UNSPECIFIED,
        View.MeasureSpec.UNSPECIFIED
    )
    UiUtil.setWidth(
        this,
        this.measuredWidth
    )
}

/**
 * Sets the size of a TextView to the measured size of its contents
 * taking into account the text size and the typeface.
 */
internal fun TextView.setWidthAndHeightToMeasured() {
    this.measure(
        View.MeasureSpec.UNSPECIFIED,
        View.MeasureSpec.UNSPECIFIED
    )
    UiUtil.setWidth(
        this,
        this.measuredWidth
    )
    UiUtil.setHeight(
        this,
        this.measuredHeight
    )
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

internal fun View.doOnGlobalLayout(block: () -> Unit) {
    this.viewTreeObserver.addOnGlobalLayoutListener(
        object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                this@doOnGlobalLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                block()
            }
        })
}

internal fun View.setHeight(value: Int) {
    this.layoutParams = this.layoutParams.also { it.height = value }
}

internal fun View.string(@StringRes id: Int): String = context.string(id)

internal fun View.color(@ColorRes id: Int): Int = context.color(id)

internal fun View.dimenPx(@DimenRes id: Int): Int = context.dimenPx(id)

internal fun View.drawable(@DrawableRes id: Int): Drawable? = context.drawable(id)
