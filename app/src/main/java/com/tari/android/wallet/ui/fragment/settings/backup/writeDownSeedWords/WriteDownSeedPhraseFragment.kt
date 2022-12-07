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
package com.tari.android.wallet.ui.fragment.settings.backup.writeDownSeedWords

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.transition.ChangeBounds
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.seed_phrase_button_disabled_text_color
import com.tari.android.wallet.databinding.FragmentWriteDownSeedPhraseBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.settings.backup.BackupSettingsRouter
import com.tari.android.wallet.ui.fragment.settings.backup.writeDownSeedWords.adapter.PhraseWordsAdapter
import com.tari.android.wallet.util.Constants


class WriteDownSeedPhraseFragment : CommonFragment<FragmentWriteDownSeedPhraseBinding, WriteDownSeedPhraseViewModel>() {

    private val adapter = PhraseWordsAdapter()
    private var isExpanded = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentWriteDownSeedPhraseBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: WriteDownSeedPhraseViewModel by viewModels()
        bindViewModel(viewModel)

        setupUI()
        observeVM()
    }

    private fun setupUI() {
        ui.warningCheckBox.setOnCheckedChangeListener { _, isChecked -> updateContinueButtonState(isChecked) }
        ui.continueCtaView.setOnClickListener(ThrottleClick {
            it.animateClick { (requireActivity() as BackupSettingsRouter).toSeedPhraseVerification(viewModel.seedWords.value!!) }
        })
        ui.phraseWordsRecyclerView.layoutManager = GridLayoutManager(requireContext(), WORD_COLUMNS_COUNT)
        ui.phraseWordsRecyclerView.adapter = adapter
        ui.expandButtonView.setOnClickListener { expandList() }
    }

    private fun observeVM() = with(viewModel) {
        observe(seedWords) {
            adapter.seedWords.clear()
            adapter.seedWords.addAll(it)
            adapter.notifyDataSetChanged()
            checkScreensFit()
        }
    }

    private fun checkScreensFit() {
        ui.phraseWordsRecyclerView.measure(
            View.MeasureSpec.makeMeasureSpec(ui.phraseWordsRecyclerView.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.UNSPECIFIED
        )
        val contentHeight = ui.phraseWordsRecyclerView.measuredHeight
        val containerHeight = ui.listContainer.height
        val isOverlapped = contentHeight > containerHeight
        ui.bottomShadow.setVisible(isOverlapped)
        ui.expandButtonView.setVisible(isOverlapped)
    }

    private fun updateContinueButtonState(isChecked: Boolean) {
        ui.continueCtaView.isEnabled = isChecked
        val color = if (isChecked) Color.WHITE else color(seed_phrase_button_disabled_text_color)
        ui.continueCtaView.setTextColor(color)
    }

    private fun expandList() {
        isExpanded = !isExpanded
        adapter.isExpanded = isExpanded
        for (i in adapter.seedWords.indices) {
            adapter.notifyItemChanged(i)
        }
        val resDrawable = if (!isExpanded) R.drawable.recovery_expand_icon else R.drawable.recovery_collapse_button
        ui.expandButtonView.setImageResource(resDrawable)
        animateExpanding(isExpanded)
    }

    private fun animateExpanding(isExpanding: Boolean = true) {
        val set = ConstraintSet()
        set.clone(ui.root)
        set.clear(R.id.list_container)
        set.clear(R.id.expand_button_view, ConstraintSet.TOP)
        set.clear(R.id.expand_button_view, ConstraintSet.BOTTOM)

        if (isExpanding) {
            set.connect(R.id.list_container, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            set.connect(R.id.list_container, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
            set.connect(R.id.list_container, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            set.connect(R.id.list_container, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)

            set.connect(
                R.id.expand_button_view,
                ConstraintSet.TOP,
                R.id.list_container,
                ConstraintSet.TOP,
                dimenPx(R.dimen.write_down_seed_expand_button_top_margin)
            )
        } else {
            set.connect(
                R.id.list_container,
                ConstraintSet.TOP,
                R.id.description,
                ConstraintSet.BOTTOM,
                dimenPx(R.dimen.write_down_seed_phrase_list_vertical_margin)
            )
            set.connect(
                R.id.list_container,
                ConstraintSet.LEFT,
                ConstraintSet.PARENT_ID,
                ConstraintSet.LEFT,
                dimenPx(R.dimen.write_down_seed_phrase_list_horizontal_margin)
            )
            set.connect(
                R.id.list_container,
                ConstraintSet.BOTTOM,
                R.id.warning_view,
                ConstraintSet.TOP,
                dimenPx(R.dimen.write_down_seed_phrase_list_vertical_margin)
            )
            set.connect(
                R.id.list_container,
                ConstraintSet.RIGHT,
                ConstraintSet.PARENT_ID,
                ConstraintSet.RIGHT,
                dimenPx(R.dimen.write_down_seed_phrase_list_horizontal_margin)
            )

            set.connect(
                R.id.expand_button_view,
                ConstraintSet.BOTTOM,
                R.id.list_container,
                ConstraintSet.BOTTOM,
                dimenPx(R.dimen.write_down_seed_expand_button_bottom_margin)
            )
        }
        val transition: Transition = ChangeBounds()
        transition.duration = Constants.UI.mediumDurationMs
        TransitionManager.beginDelayedTransition(ui.root, transition)
        set.applyTo(ui.root)
    }

    companion object {
        private const val WORD_COLUMNS_COUNT = 2
    }
}

