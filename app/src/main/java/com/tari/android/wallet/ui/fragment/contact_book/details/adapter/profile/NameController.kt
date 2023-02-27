package com.tari.android.wallet.ui.fragment.contact_book.details.adapter.profile

import android.app.Activity
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ItemContactProfileBinding
import com.tari.android.wallet.ui.extension.hideKeyboard
import com.tari.android.wallet.ui.extension.setSelectionToEnd
import com.tari.android.wallet.ui.extension.showKeyboard
import com.tari.android.wallet.ui.fragment.home.HomeActivity

class NameController(private val binding: ItemContactProfileBinding, private val saveName: (String) -> Unit) {

    private var editState: Boolean = false

    init {
        applyEdit()
        binding.aliasEditIcon.setOnClickListener { toggleEdit() }
    }

    private fun toggleEdit() {
        editState = !editState
        if (!editState) {
            saveName.invoke(binding.alias.text.toString())
        }
        applyEdit()
    }

    private fun applyEdit() {
        val icon = if (editState) R.drawable.vector_profile_edit_apply else R.drawable.vector_profile_edit_pen
        binding.aliasEditIcon.setImageResource(icon)
        binding.aliasBackground.switch(editState)
        binding.alias.isEnabled = editState
        if (editState) {
            binding.alias.requestFocus()
            binding.alias.setSelectionToEnd()
            HomeActivity.instance.get()?.showKeyboard()
        } else {
            HomeActivity.instance.get()?.hideKeyboard()
        }
    }
}