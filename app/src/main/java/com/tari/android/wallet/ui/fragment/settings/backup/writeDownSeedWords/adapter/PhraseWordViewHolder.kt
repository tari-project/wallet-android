package com.tari.android.wallet.ui.fragment.settings.backup.writeDownSeedWords.adapter

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tari.android.wallet.R

class PhraseWordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val indexTV: TextView = itemView.findViewById(R.id.word_position_text_view)
    private val contentTV: TextView = itemView.findViewById(R.id.word_content_text_view)

    fun bind(index: Int, word: String) {
        val text = (index + 1).toString()
        indexTV.text = text
        contentTV.text = word
    }
}