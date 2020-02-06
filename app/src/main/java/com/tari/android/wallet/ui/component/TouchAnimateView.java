/**
 * Copyright 2020 The Tari Project
 * <p>
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * <p>
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * <p>
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p>
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
package com.tari.android.wallet.ui.component;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tari.android.wallet.R;

/**
 * Customized view for the button touch effect.
 *
 * @author The Tari Development Team
 */
public class TouchAnimateView extends FrameLayout {

    private Animator downAnimator, upAnimator;

    private GestureDetector gestureDetector =
            new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public void onShowPress(MotionEvent e) {
                    downAnimator.start();
                }

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    performClick();
                    return true;
                }
            });

    public TouchAnimateView(@NonNull Context context) {
        super(context);
        initAnimators();
    }

    public TouchAnimateView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initAnimators();
    }

    public TouchAnimateView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAnimators();
    }

    private void initAnimators() {
        downAnimator = AnimatorInflater.loadAnimator(getContext(), R.animator.touch_animate_click_down);
        downAnimator.setTarget(this);
        upAnimator = AnimatorInflater.loadAnimator(getContext(), R.animator.touch_animate_click_up);
        upAnimator.setTarget(this);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                upAnimator.start();
                break;
        }
        return true;
    }
}
