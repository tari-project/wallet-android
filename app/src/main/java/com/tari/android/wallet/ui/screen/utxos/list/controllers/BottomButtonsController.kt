package com.tari.android.wallet.ui.screen.utxos.list.controllers

import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.FragmentUtxosListBinding
import com.tari.android.wallet.util.extension.setVisible
import kotlinx.coroutines.*

class BottomButtonsController(val ui: FragmentUtxosListBinding, private val fragmentScope: CoroutineScope) {

    private var counterToStartAnimation: Job? = null
    private var currentState: JoinSplitButtonsState = JoinSplitButtonsState.None

    fun setState(state: JoinSplitButtonsState) {
        if (currentState == state) return
        currentState = state
        when (state) {
            JoinSplitButtonsState.None -> {
                ui.splitJoinContainer.setVisible(false)
                ui.buttonsDivider.setVisible(false)
                ui.joinButton.setVisible(false)
                ui.combineButtonText.setVisible(false)
                ui.combineAndBreakButtonText.setVisible(false)
            }
            JoinSplitButtonsState.Break -> {
                ui.splitJoinContainer.setVisible(true)
                ui.buttonsDivider.setVisible(false)
                ui.joinButton.setVisible(false)
                ui.combineAndBreakButtonText.setText(R.string.utxos_break_button)
                ui.combineButtonText.setVisible(false)
                ui.combineAndBreakButtonText.setVisible(true)
            }
            JoinSplitButtonsState.JoinAndBreak -> {
                ui.splitJoinContainer.setVisible(true)
                ui.buttonsDivider.setVisible(true)
                ui.joinButton.setVisible(true)
                ui.combineAndBreakButtonText.setText(R.string.utxos_combine_and_break_button)
                ui.combineButtonText.setVisible(true)
                ui.combineAndBreakButtonText.setVisible(true)
            }
        }
        fragmentScope.launch {
            counterToStartAnimation?.cancelAndJoin()
            counterToStartAnimation = fragmentScope.launch(Dispatchers.IO) {
                delay(ANIMATION_DELAY)
                fragmentScope.launch(Dispatchers.Main) {
                    ui.combineButtonText.setVisible(false)
                    ui.combineAndBreakButtonText.setVisible(false)
                }
            }
        }
    }

    companion object {
        const val ANIMATION_DELAY = 3000L
    }
}