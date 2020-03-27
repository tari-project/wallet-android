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
package com.tari.android.wallet.ui.activity.home

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.tari.android.wallet.ui.util.UiUtil

/**
 * Scroll view customized for the transaction list nested scroll functionality.
 *
 * @author The Tari Development Team
 */
internal class CustomScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

    var flingIsRunning = false
    var isScrollable = true

    fun completeScroll() {
        if (flingIsRunning) {
            return
        }
        val maxScrollY = UiUtil.getHeight(getChildAt(0)) - height
        val scrollRatio = scrollY.toFloat() / maxScrollY.toFloat()
        //if (scrollRatio == 0f || scrollRatio == 1f) {
        //    return
        //}
        if (scrollRatio > 0.5) {
            smoothScrollTo(0, maxScrollY)
        } else {
            smoothScrollTo(0, 0)
        }
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        if (!isScrollable) {
            consumed[1] = dy
            return
        }
        if (dy > 0) {
            if (canScrollVertically(dy)) {
                scrollBy(0, dy)
                consumed[1] = dy
                return
            } else {
                return
            }
        } else if (dy < 0) {
            if (target is SwipeRefreshLayout && !isRvScrolledToTop(target.getChildAt(0) as RecyclerView)) {
                return
            } else if (canScrollVertically(dy)) {
                scrollBy(0, dy)
                consumed[1] = dy
                return
            }
        }
    }

    private fun flingScroll(velocityY: Int) {
        val targetScrollY = if (velocityY < 0) {
            0
        } else {
            UiUtil.getHeight(getChildAt(0)) - height
        }
        smoothScrollTo(0, targetScrollY)
        flingIsRunning = true
    }

    override fun fling(velocityY: Int) {
        flingScroll(velocityY)
    }

    override fun onNestedPreFling(target: View?, velocityX: Float, velocityY: Float): Boolean {
        if (target is SwipeRefreshLayout) {
            if (canScrollVertically(velocityY.toInt())) {
                if (isRvScrolledToTop(target.getChildAt(0) as RecyclerView)) {
                    flingScroll(velocityY.toInt())
                    return true
                }
            }
        } else if (target is RecyclerView) {
            if (canScrollVertically(velocityY.toInt())) {
                if (isRvScrolledToTop(target)) {
                    flingScroll(velocityY.toInt())
                    return true
                }
            }
        }
        return super.onNestedPreFling(target, velocityX, velocityY)
    }

    private fun isRvScrolledToTop(rv: RecyclerView): Boolean {
        val lm = rv.layoutManager as LinearLayoutManager
        if (lm.childCount == 0) return true
        return (lm.findFirstVisibleItemPosition() == 0
                && lm.findViewByPosition(0)?.top == 0)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        if (!isScrollable) {
            return true
        }
        return super.onTouchEvent(ev)
    }

}