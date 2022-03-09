package com.tari.android.wallet.ui.fragment.settings.backup.writeDownSeedWords.adapter

import android.animation.ValueAnimator
import android.view.View
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.core.view.marginTop
import androidx.recyclerview.widget.RecyclerView
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.extension.dimen
import com.tari.android.wallet.ui.extension.setBottomMargin
import com.tari.android.wallet.ui.extension.setTopMargin
import com.tari.android.wallet.util.Constants

class PhraseWordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val indexTV: TextView = itemView.findViewById(R.id.word_position_text_view)
    private val contentTV: TextView = itemView.findViewById(R.id.word_content_text_view)

    fun bind(index: Int, word: String, isExpanded: Boolean = true) {
        val text = (index + 1).toString()
        indexTV.text = text
        contentTV.text = word

        val d = if (isExpanded) R.dimen.write_down_seed_phrase_list_expanded_decoration else R.dimen.write_down_seed_phrase_list_collapsed_decoration
        val margin = itemView.context.dimen(d)
        if(margin == itemView.marginTop.toFloat()) return
        ValueAnimator.ofFloat(itemView.marginTop.toFloat(), margin).apply {
            duration = Constants.UI.longDurationMs
            addUpdateListener {
                val value = (it.animatedValue as Float).toInt()
                itemView.setTopMargin(value)
                itemView.setBottomMargin(value)
            }
            doOnEnd {
                itemView.setTopMargin(margin.toInt())
                itemView.setBottomMargin(margin.toInt())
            }
            start()
        }
    }
}