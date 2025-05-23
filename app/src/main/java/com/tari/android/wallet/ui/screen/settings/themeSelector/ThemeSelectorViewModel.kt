package com.tari.android.wallet.ui.screen.settings.themeSelector

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.data.baseNode.BaseNodeStateHandler
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.screen.settings.themeSelector.adapter.ThemeViewHolderItem
import javax.inject.Inject

class ThemeSelectorViewModel : CommonViewModel() {

    @Inject
    lateinit var baseNodeStateHandler: BaseNodeStateHandler

    val themes: MutableLiveData<List<ThemeViewHolderItem>> = MutableLiveData()

    val newTheme = SingleLiveEvent<TariTheme>()

    init {
        component.inject(this)

        collectFlow(baseNodeStateHandler.baseNodeState) { loadList() }

        loadList()
    }

    fun selectTheme(theme: TariTheme) {
        if (tariSettingsSharedRepository.currentTheme == theme) return
        tariSettingsSharedRepository.currentTheme = theme
        newTheme.postValue(theme)
    }

    fun refresh() = loadList()

    private fun loadList() {
        val currentTheme = tariSettingsSharedRepository.currentTheme
        val list = TariTheme.entries.map { ThemeViewHolderItem(it, currentTheme) { isChecked -> if (isChecked) selectTheme(it) } }
        themes.postValue(list)
    }
}