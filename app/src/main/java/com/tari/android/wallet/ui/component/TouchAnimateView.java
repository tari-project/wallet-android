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
