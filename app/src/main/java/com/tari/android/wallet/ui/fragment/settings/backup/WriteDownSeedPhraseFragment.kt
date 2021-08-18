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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.os.IBinder
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.seed_phrase_button_disabled_text_color
import com.tari.android.wallet.databinding.FragmentWriteDownSeedPhraseBinding
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.ui.activity.settings.BackupSettingsRouter
import com.tari.android.wallet.ui.dialog.error.ErrorDialog
import com.tari.android.wallet.ui.extension.ThrottleClick
import com.tari.android.wallet.ui.extension.animateClick
import com.tari.android.wallet.ui.extension.color
import com.tari.android.wallet.ui.extension.string
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WriteDownSeedPhraseFragment @Deprecated(
    """Use newInstance() and supply all
the necessary data via arguments instead, as fragment's default no-op constructor is used by the
framework for UI tree rebuild on configuration changes"""
) constructor() : Fragment(), ServiceConnection {

    private lateinit var ui: FragmentWriteDownSeedPhraseBinding
    private lateinit var walletService: TariWalletService

    private val seedWords = mutableListOf<String>()

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
                    .toSeedPhraseVerification(this, seedWords)
            }
        })
        ui.phraseWordsRecyclerView.layoutManager =
            GridLayoutManager(requireContext(), WORD_COLUMNS_COUNT)
        ui.phraseWordsRecyclerView.adapter = PhraseWordsAdapter(seedWords)
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
        bindToWalletService()
    }

    override fun onDestroyView() {
        requireActivity().unbindService(this)
        super.onDestroyView()
    }

    private fun bindToWalletService() {
        val bindIntent = Intent(requireActivity(), WalletService::class.java)
        requireActivity().bindService(bindIntent, this, Context.BIND_AUTO_CREATE)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Logger.i("WriteDownSeedPhraseFragment onServiceConnected")
        walletService = TariWalletService.Stub.asInterface(service)
        lifecycleScope.launch(Dispatchers.IO) {
            getSeedWords()
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Logger.i("WriteDownSeedPhraseFragment onServiceDisconnected")
        // No-op for now
    }

    private fun getSeedWords() {
        val error = WalletError()
        val seedWords = walletService.getSeedWords(error)
        if (seedWords != null) {
            this.seedWords.clear()
            this.seedWords.addAll(seedWords)
            lifecycleScope.launch(Dispatchers.Main) {
                ui.phraseWordsRecyclerView.adapter?.notifyDataSetChanged()
            }
        } else {
            // display error
            ErrorDialog(
                requireContext(),
                title = string(R.string.common_error_title),
                description = string(R.string.back_up_seed_phrase_error)
            ).show()
        }
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
    }

}
