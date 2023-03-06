package com.tari.android.wallet.ui.dialog.modular.modules.input

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.tari.android.wallet.databinding.DialogModuleInputBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.ui.extension.setSelectionToEnd
import com.tari.android.wallet.ui.extension.showKeyboard

@SuppressLint("ViewConstructor")
class InputModuleView(context: Context, inputModule: InputModule) : CommonView<CommonViewModel, DialogModuleInputBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleInputBinding =
        DialogModuleInputBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        val editText = ui.input.ui.editText
        editText.setText(inputModule.value)
        editText.hint = inputModule.hint
        ui.input.textChangedListener = { text, _, _, _ -> inputModule.value = text.toString() }
        editText.imeOptions = EditorInfo.IME_ACTION_DONE
        editText.setOnEditorActionListener { _, _, _ -> inputModule.onDoneAction(editText.text.toString()) }
        if (inputModule.focused) {
            ui.input.ui.editText.postDelayed({
                editText.requestFocus()
                editText.postDelayed({
                    (context as? Activity)?.showKeyboard(editText)
                    editText.setSelectionToEnd()
                }, 100)
            }, 200)
        }
    }
}