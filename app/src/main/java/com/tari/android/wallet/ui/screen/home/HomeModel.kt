package com.tari.android.wallet.ui.screen.home

object HomeModel {
    data class UiState(
        val selectedMenuItem: BottomMenuOption = BottomMenuOption.Home,
        val airdropLoggedIn: Boolean,

        val airdropEnabled: Boolean,
    )

    enum class BottomMenuOption { Home, Shop, Gem, Profile, Settings }
}