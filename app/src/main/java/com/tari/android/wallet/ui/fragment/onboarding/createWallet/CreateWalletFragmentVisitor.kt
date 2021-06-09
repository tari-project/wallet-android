package com.tari.android.wallet.ui.fragment.onboarding.createWallet

import com.tari.android.wallet.model.yat.EmojiId


interface WalletCreationState {
    fun dispatch(visitor: WalletCreationStateVisitor)
}

object InitialState : WalletCreationState {
    override fun dispatch(visitor: WalletCreationStateVisitor) =
        visitor.onInitial()
}

object SearchingForInitialYatState : WalletCreationState {
    override fun dispatch(visitor: WalletCreationStateVisitor) =
        visitor.onSearchingForInitialYat()
}

data class InitialYatFoundState(private val yat: EmojiId) : WalletCreationState {
    override fun dispatch(visitor: WalletCreationStateVisitor) = visitor.onInitialYatFound(yat)
}

data class InitialYatSearchErrorState(private val exception: Exception) : WalletCreationState {
    override fun dispatch(visitor: WalletCreationStateVisitor) =
        visitor.onInitialYatSearchError(exception)
}

object YatAcquiringState : WalletCreationState {
    override fun dispatch(visitor: WalletCreationStateVisitor) = visitor.onYatAcquiring()
}

object YatAcquiredState : WalletCreationState {
    override fun dispatch(visitor: WalletCreationStateVisitor) = visitor.onYatAcquired()
}

interface WalletCreationStateVisitor {
    fun onInitial()

    fun onSearchingForInitialYat()
    fun onInitialYatFound(yat: EmojiId)
    fun onInitialYatSearchError(exception: Exception)

    fun onYatAcquiring()
    fun onYatAcquired()
}

