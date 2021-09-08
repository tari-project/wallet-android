package com.tari.android.wallet.ui.fragment.tx.questionMark

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import com.tari.android.wallet.databinding.ViewQuestionMarkBinding
import com.tari.android.wallet.ui.component.common.CommonView

internal class QuestionMarkView : CommonView<QuestionMarkViewModel, ViewQuestionMarkBinding> {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean):
            ViewQuestionMarkBinding = ViewQuestionMarkBinding.inflate(layoutInflater, parent, attachToRoot)

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var isDowned: Boolean = false

    override fun setup() = Unit

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return when (event?.action) {
            MotionEvent.ACTION_UP -> {
                if (isDowned && isExistPoint(event)) {
                    viewModel.showUniversityDialog()
                    true
                } else {
                    false
                }
            }
            MotionEvent.ACTION_DOWN -> {
                if (isExistPoint(event)) {
                    isDowned = true
                }
                false
            }
            else -> super.onTouchEvent(event)
        }
    }

    private fun isExistPoint(event: MotionEvent): Boolean {
        val screenPos = IntArray(2)
        this.getLocationOnScreen(screenPos)
        val rect = Rect(screenPos[0], screenPos[1], screenPos[0] + this.width, screenPos[1] + this.height)

        return rect.contains(event.rawX.toInt(), event.rawY.toInt())
    }
}



