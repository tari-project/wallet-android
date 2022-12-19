package com.tari.android.wallet.ui.fragment.settings.themeSelector

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.fragment.settings.themeSelector.adapter.ThemeViewHolderItem

class ThemeSelectorViewModel : CommonViewModel() {

    val themes: MutableLiveData<MutableList<ThemeViewHolderItem>> = MutableLiveData()

    val newTheme = SingleLiveEvent<TariTheme>()

    init {
        component.inject(this)

        EventBus.baseNodeState.subscribe(this) { loadList() }

        loadList()
    }

    fun selectTheme(theme: TariTheme) {
        if (tariSettingsSharedRepository.currentTheme == theme) return
        tariSettingsSharedRepository.currentTheme = theme
        newTheme.postValue(theme)
    }

    fun refresh() = loadList()

    private fun loadList() {
        val currentTheme = tariSettingsSharedRepository.currentTheme!!
        val list = TariTheme.values().map { ThemeViewHolderItem(it, currentTheme) { isChecked -> if (isChecked) selectTheme(it) } }.toMutableList()
        themes.postValue(list)
    }
}