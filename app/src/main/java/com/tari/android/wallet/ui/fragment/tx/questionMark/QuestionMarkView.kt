package com.tari.android.wallet.ui.fragment.tx.questionMark

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.databinding.ViewQuestionMarkBinding
import com.tari.android.wallet.ui.component.common.CommonView

class QuestionMarkView : CommonView<QuestionMarkViewModel, ViewQuestionMarkBinding> {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean):
            ViewQuestionMarkBinding = ViewQuestionMarkBinding.inflate(layoutInflater, parent, attachToRoot)

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun setup() = Unit

    init {
        setOnClickListener { viewModel.showUniversityDialog() }
    }
}