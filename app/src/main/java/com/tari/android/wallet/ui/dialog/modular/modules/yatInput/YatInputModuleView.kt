package com.tari.android.wallet.ui.dialog.modular.modules.yatInput

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.tari.android.wallet.databinding.DialogModuleInputYatBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.domain.PaletteManager
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.ui.extension.setSelectionToEnd
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.extension.showKeyboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@SuppressLint("ViewConstructor")
class YatInputModuleView(context: Context, private val inputModule: YatInputModule) :
    CommonView<CommonViewModel, DialogModuleInputYatBinding>(context) {

    private val coroutineScope = CoroutineScope(Job())

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleInputYatBinding =
        DialogModuleInputYatBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        isLoading(false)
        val editText = ui.input.ui.editText
        editText.setText(inputModule.value)
        editText.hint = inputModule.hint
        ui.input.textChangedListener = { text, _, _, _ ->
            inputModule.value = text.toString()
            loadYatInfo(text.toString())
        }
        editText.imeOptions = if (inputModule.isEnd) EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_NEXT
        editText.setOnEditorActionListener { _, id, _ -> if (id == EditorInfo.IME_ACTION_DONE) inputModule.onDoneAction() else false }
        if (inputModule.isFirst) {
            ui.input.ui.editText.postDelayed({
                editText.requestFocus()
                editText.postDelayed({
                    (context as? Activity)?.showKeyboard(editText)
                    editText.setSelectionToEnd()
                }, 100)
            }, 200)
        }
    }

    private fun loadYatInfo(yat: String) {
        isLoading(true)

        coroutineScope.launch(Dispatchers.IO) {
            val yatInfo = inputModule.search(yat)

            launch(Dispatchers.Main) {
                isLoading(false)
                ui.yat.imageTintList = if (yatInfo) createColorStateList(PaletteManager().getIconDefault(context))
                else createColorStateList(PaletteManager().getIconInactive(context))
            }
        }
    }

    private fun createColorStateList(color: Int): ColorStateList {
        val states = arrayOf(intArrayOf(android.R.attr.state_enabled))
        val colors = intArrayOf(color)
        return ColorStateList(states, colors)
    }

    private fun isLoading(isLoading: Boolean) {
        ui.progressBar.setVisible(isLoading)
        ui.yat.setVisible(!isLoading)
    }
}