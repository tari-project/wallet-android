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

val clickEnablingHandler = Handler()

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

fun temporarilyDisableClick(
    @NonNull view: View
) {
    view.isClickable = false
    clickEnablingHandler.postDelayed(
        ClickEnablingRunnable(view),
        1000
    )
}

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