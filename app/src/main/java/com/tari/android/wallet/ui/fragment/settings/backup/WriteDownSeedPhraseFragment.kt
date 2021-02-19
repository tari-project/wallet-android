/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
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
package com.tari.android.wallet.ui.fragment.settings.backup

import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tari.android.wallet.R.color.seed_phrase_button_disabled_text_color
import com.tari.android.wallet.databinding.FragmentWriteDownSeedPhraseBinding
import com.tari.android.wallet.ui.activity.settings.BackupSettingsRouter
import com.tari.android.wallet.ui.extension.ThrottleClick
import com.tari.android.wallet.ui.extension.animateClick
import com.tari.android.wallet.ui.extension.color

class WriteDownSeedPhraseFragment @Deprecated(
    """Use newInstance() and supply all
the necessary data via arguments instead, as fragment's default no-op constructor is used by the
framework for UI tree rebuild on configuration changes"""
) constructor() : Fragment() {

    private lateinit var ui: FragmentWriteDownSeedPhraseBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentWriteDownSeedPhraseBinding.inflate(inflater, container, false)
        .also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ui.warningCheckBox.setOnCheckedChangeListener { _, isChecked ->
            updateContinueButtonState(isChecked)
        }
        ui.backCtaView.setOnClickListener { requireActivity().onBackPressed() }
        ui.continueCtaView.setOnClickListener(ThrottleClick {
            it.animateClick {
                (requireActivity() as BackupSettingsRouter)
                    .toRecoveryPhraseVerification(this, WORDS_POOL)
            }
        })
        ui.phraseWordsRecyclerView.layoutManager =
            GridLayoutManager(requireContext(), WORD_COLUMNS_COUNT)
        ui.phraseWordsRecyclerView.adapter = PhraseWordsAdapter(WORDS_POOL)
        ui.phraseWordsRecyclerView.addItemDecoration(
            VerticalInnerMarginDecoration(
                value = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    WORD_BOTTOM_MARGIN_DP.toFloat(),
                    resources.displayMetrics
                ).toInt(),
                spans = WORD_COLUMNS_COUNT
            )
        )
    }

    private fun updateContinueButtonState(isChecked: Boolean) {
        ui.continueCtaView.isEnabled = isChecked
        ui.continueCtaView.setTextColor(
            if (isChecked) Color.WHITE
            else color(seed_phrase_button_disabled_text_color)
        )
    }

    private class VerticalInnerMarginDecoration(private val value: Int, private val spans: Int) :
        RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            if (parent.getChildLayoutPosition(view) < spans) {
                outRect.top = value
            }
            outRect.bottom = value
        }
    }

    companion object {
        private const val WORD_COLUMNS_COUNT = 2
        private const val WORD_BOTTOM_MARGIN_DP = 24

        @Suppress("DEPRECATION")
        fun newInstance() = WriteDownSeedPhraseFragment()
        private val WORDS_POOL = listOf(
            "Aurora",
            "Decentralized",
            "Fluffy",
            "Digital",
            "Contribute",
            "Collect",
            "Tokens",
            "Assets",
            "Scalable",
            "Tari",
            "Gems",
            "Code",
            "Code",
            "Gems",
            "Tari",
            "Scalable",
            "Assets",
            "Tokens",
            "Collect",
            "Contribute",
            "Digital",
            "Fluffy",
            "Decentralized",
            "Aurora"
        )
    }

}
