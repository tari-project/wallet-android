package com.tari.android.wallet.ui.fragment.contact_book.details.adapter.profile

import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ItemContactProfileBinding

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
    }
}