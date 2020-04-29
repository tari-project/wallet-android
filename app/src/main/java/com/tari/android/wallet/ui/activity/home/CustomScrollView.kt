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

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ScrollView
import androidx.core.animation.addListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindDimen
import butterknife.BindView
import butterknife.ButterKnife
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.Constants
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.min

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

    @BindView(R.id.recycler_view_container_view)
    lateinit var recyclerViewContainerView: View
    @BindView(R.id.update_progress_content_container_view)
    lateinit var progressContainerView: View
    @BindView(R.id.update_progress_content_view_bg)
    lateinit var progressViewBg: View
    @BindView(R.id.update_progress_content_view)
    lateinit var progressView: View

    @BindDimen(R.dimen.home_swipe_refresh_max_scroll_y)
    @JvmField
    var refreshSwipeMaxScrollY = 0
    @BindDimen(R.dimen.home_swipe_refresh_progress_view_content_invisible_top_margin)
    @JvmField
    var progressViewInvisibleTopMargin = 0
    @BindDimen(R.dimen.home_swipe_refresh_progress_view_content_visible_top_margin)
    @JvmField
    var progressViewVisibleTopMargin = 0
    @BindDimen(R.dimen.home_swipe_refresh_progress_view_container_height)
    @JvmField
    var progressViewContainerHeight = 0
    @BindDimen(R.dimen.common_view_elevation)
    @JvmField
    var elevation = 0
    @BindDimen(R.dimen.home_grabber_height)
    @JvmField
    var grabberHeight = 0

    private var swipeRefreshYOffset = 0
    private var lastDeltaY = 0
    private var isUpdating = false
    private val updateProgressViewRevealDurationMs = 400L
    var recyclerViewContainerInitialHeight = 0

    var listenerWeakReference: WeakReference<Listener>? = null
    var updateProgressViewController: UpdateProgressViewController? = null

    fun bindUI() {
        ButterKnife.bind(this, this)
    }

    fun completeScroll() {
        if (flingIsRunning) {
            return
        }
        if (swipeRefreshYOffset > 0 && !isUpdating) {
            val targetOffset = if (swipeRefreshYOffset >= progressViewContainerHeight) {
                progressViewContainerHeight
            } else {
                0
            }
            val anim = ValueAnimator.ofInt(swipeRefreshYOffset, targetOffset)
            anim.addUpdateListener {
                swipeRefreshYOffset = it.animatedValue as Int
                requestLayout()
            }
            anim.duration = Constants.UI.shortDurationMs
            anim.interpolator = EasingInterpolator(Ease.CIRC_IN)
            anim.addListener(onEnd = {
                if (targetOffset > 0) {
                    listenerWeakReference?.get()?.onSwipeRefresh(this@CustomScrollView)
                    postDelayed({
                        isUpdating = true
                    }, Constants.UI.shortDurationMs)
                }
                anim.removeAllListeners()
            })
            anim.start()
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

    override fun onNestedPreScroll(
        target: View,
        deltaX: Int,
        deltaY: Int,
        consumed: IntArray
    ) {
        if (!isScrollable) {
            consumed[1] = deltaY
            return
        }
        if (deltaY > 0) {
            when {
                swipeRefreshYOffset > 0 && lastDeltaY > 0 && !isUpdating -> {
                    swipeRefreshYOffset -= deltaY
                    swipeRefreshYOffset = max(0, swipeRefreshYOffset)
                    consumed[1] = deltaY
                    requestLayout()
                    lastDeltaY = deltaY
                }
                canScrollVertically(deltaY) -> {
                    if (swipeRefreshYOffset == 0 || isUpdating) {
                        scrollBy(0, deltaY)
                    }
                    consumed[1] = deltaY
                    lastDeltaY = deltaY
                }
            }
        } else if (deltaY < 0) {
            if (target is RecyclerView && !isRvScrolledToTop(target)) {
                lastDeltaY = deltaY
                return
            } else if (canScrollVertically(deltaY)) {
                scrollBy(0, deltaY)
                consumed[1] = deltaY
            } else if (lastDeltaY < 0 && !isUpdating) { // enter the swipe-refresh zone
                updateProgressViewController?.reset()
                swipeRefreshYOffset = min(
                    refreshSwipeMaxScrollY,
                    swipeRefreshYOffset - deltaY
                )
                consumed[1] = deltaY
                requestLayout()
            }
            lastDeltaY = deltaY
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (!isUpdating) {
            if (swipeRefreshYOffset > 0) {
                UiUtil.setHeight(
                    progressContainerView,
                    swipeRefreshYOffset
                )
                UiUtil.setHeight(
                    recyclerViewContainerView,
                    recyclerViewContainerInitialHeight - swipeRefreshYOffset
                )
                val totalMarginTopDelta = progressViewVisibleTopMargin - progressViewInvisibleTopMargin
                val ratio = min(swipeRefreshYOffset.toFloat() / progressViewContainerHeight, 1f)
                UiUtil.setTopMargin(
                    progressView,
                    progressViewInvisibleTopMargin + (totalMarginTopDelta * ratio).toInt()
                )
                progressView.alpha = ratio
                progressViewBg.elevation = elevation / 2 * ratio
                invalidate()
                if (scrollY < grabberHeight * 2) scrollTo(0, 0)
            } else {
                UiUtil.setHeight(progressContainerView, 0)
                UiUtil.setHeight(
                    recyclerViewContainerView,
                    recyclerViewContainerInitialHeight
                )
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
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
        if (swipeRefreshYOffset > 0 && !isUpdating) {
            return
        }
        flingScroll(velocityY)
    }

    override fun onNestedPreFling(target: View?, velocityX: Float, velocityY: Float): Boolean {
        if (swipeRefreshYOffset > 0 && !isUpdating) {
            return true
        }
        if (target is RecyclerView) {
            if (canScrollVertically(velocityY.toInt())) {
                if (isRvScrolledToTop(target)) {
                    flingScroll(velocityY.toInt())
                    return true
                }
            }
        }
        return super.onNestedPreFling(target, velocityX, velocityY)
    }

    private fun isRvScrolledToTop(recyclerView: RecyclerView): Boolean {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        if (layoutManager.childCount == 0) return true
        return (layoutManager.findFirstVisibleItemPosition() == 0
                && layoutManager.findViewByPosition(0)?.top == 0)
    }

    fun beginUpdate() {
        if (isUpdating) return
        val anim = ValueAnimator.ofInt(0, progressViewContainerHeight)
        anim.addUpdateListener {
            swipeRefreshYOffset = it.animatedValue as Int
            requestLayout()
        }
        anim.duration = updateProgressViewRevealDurationMs
        anim.addListener(onEnd = {
            isUpdating = true
            anim.removeAllListeners()
        })
        anim.start()
    }

    fun finishUpdate(onComplete: (() -> Unit)? = null) {
        isUpdating = false
        val anim = ValueAnimator.ofInt(swipeRefreshYOffset, 0)
        anim.addUpdateListener {
            swipeRefreshYOffset = it.animatedValue as Int
            requestLayout()
        }
        anim.duration = updateProgressViewRevealDurationMs
        anim.addListener(onEnd = {
            onComplete?.let { callback -> callback() }
        })
        anim.start()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isScrollable) {
            return true
        }
        return super.onTouchEvent(event)
    }

    interface Listener {

        fun onSwipeRefresh(source: CustomScrollView)

    }

}