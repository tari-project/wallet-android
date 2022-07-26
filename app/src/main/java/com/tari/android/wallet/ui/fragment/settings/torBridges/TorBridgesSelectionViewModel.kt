package com.tari.android.wallet.ui.fragment.settings.torBridges

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.application.baseNodes.BaseNodes
import com.tari.android.wallet.data.sharedPrefs.tor.TorBridgeConfigurationList
import com.tari.android.wallet.data.sharedPrefs.tor.TorSharedRepository
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.tor.TorProxyManager
import com.tari.android.wallet.tor.TorProxyState
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import com.tari.android.wallet.ui.dialog.inProgress.ProgressDialogArgs
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.fragment.settings.torBridges.torItem.TorBridgeViewHolderItem
import javax.inject.Inject

class TorBridgesSelectionViewModel : CommonViewModel() {

    @Inject
    lateinit var torSharedRepository: TorSharedRepository

    @Inject
    internal lateinit var torProxyManager: TorProxyManager

    @Inject
    internal lateinit var baseNodes: BaseNodes

    init {
        component.inject(this)
    }

    private val _torBridges = MutableLiveData<MutableList<TorBridgeViewHolderItem>>()
    val torBridges: LiveData<MutableList<TorBridgeViewHolderItem>> = _torBridges

    private val _navigation = SingleLiveEvent<TorBridgeNavigation>()
    val navigation: LiveData<TorBridgeNavigation> = _navigation

    init {
        loadData()
    }

    fun loadData() {
        val noBridges = TorBridgeViewHolderItem.Empty(resourceManager)
        val customBridges = TorBridgeViewHolderItem.CustomBridges(resourceManager)

        val bridges = mutableListOf<TorBridgeViewHolderItem.Bridge>()
        bridges.addAll(torSharedRepository.customTorBridges.orEmpty().map { TorBridgeViewHolderItem.Bridge(it, false) })
        if (torSharedRepository.currentTorBridges.isNullOrEmpty()) {
            noBridges.isSelected = true
        } else {
            bridges.forEach {
                if (torSharedRepository.currentTorBridges.orEmpty().contains(it.bridgeConfiguration)) {
                    it.isSelected = true
                }
            }
        }

        val bridgeConfigurations = mutableListOf<TorBridgeViewHolderItem>().apply {
            add(noBridges)
            addAll(bridges)
            add(customBridges)
        }

        _torBridges.postValue(bridgeConfigurations)
    }

    fun preselect(torBridgeItem: TorBridgeViewHolderItem) {
        val list = _torBridges.value.orEmpty()
        when (torBridgeItem) {
            is TorBridgeViewHolderItem.CustomBridges -> _navigation.postValue(TorBridgeNavigation.ToCustomBridges)
            is TorBridgeViewHolderItem.Empty -> {
                list.forEach { it.isSelected = false }
                torBridgeItem.isSelected = true
            }
            else -> {
                torBridgeItem.isSelected = !torBridgeItem.isSelected
            }
        }
        val isEmptyChoice = list.filter { (it is TorBridgeViewHolderItem.Bridge) }.all { !(it as TorBridgeViewHolderItem.Bridge).isSelected }
        list.first { it is TorBridgeViewHolderItem.Empty }.isSelected = isEmptyChoice
        _torBridges.postValue(_torBridges.value)
    }

    fun connect() {
        val selectedBridges = _torBridges.value.orEmpty().filter { it.isSelected }
        if (selectedBridges.any { it is TorBridgeViewHolderItem.Empty }) {
            torSharedRepository.currentTorBridges = null
        } else {
            torSharedRepository.currentTorBridges =
                TorBridgeConfigurationList(selectedBridges.map { (it as TorBridgeViewHolderItem.Bridge).bridgeConfiguration })
        }

        restartTor()
    }

    private fun restartTor() {
        val progressArgs = ProgressDialogArgs(
            resourceManager.getString(R.string.tor_bridges_connection_progress_title),
            resourceManager.getString(R.string.tor_bridges_connection_progress_description),
            closeButtonText = resourceManager.getString(R.string.common_cancel),
            cancelable = true
        ) { stopConnecting() }
        _loadingDialog.postValue(progressArgs)
        torProxyManager.shutdown()
        subscribeToTorState()
        torProxyManager.run()
        baseNodes.startSync()
    }

    private fun subscribeToTorState() {
        EventBus.torProxyState.subscribe(this) {
            when (it) {
                is TorProxyState.Failed -> {
                    EventBus.torProxyState.unsubscribe(this)
                    val errorArgs = ErrorDialogArgs(
                        resourceManager.getString(R.string.tor_bridges_connecting_error_title),
                        resourceManager.getString(R.string.tor_bridges_connecting_error_description, it.e.message.orEmpty()),
                        cancelable = true
                    ) { stopConnecting() }
                    _modularDialog.postValue(errorArgs.getModular(resourceManager))
                }
                is TorProxyState.Running -> {
                    if (it.bootstrapStatus.progress == 100) {
                        EventBus.torProxyState.unsubscribe(this)
                        var description = resourceManager.getString(R.string.tor_bridges_connection_progress_successful_description)
                        if (torSharedRepository.currentTorBridges.orEmpty().isEmpty()) {
                            description += resourceManager.getString(R.string.tor_bridges_connection_progress_successful_no_bridges)
                        } else {
                            description += resourceManager.getString(R.string.tor_bridges_connection_progress_successful_used_bridges)
                            for (bridge in torSharedRepository.currentTorBridges.orEmpty()) {
                                description += "${bridge.ip}:${bridge.port}\n"
                            }
                        }
                        val args = ModularDialogArgs(
                            DialogArgs(false, canceledOnTouchOutside = false), modules = listOf(
                                HeadModule(resourceManager.getString(R.string.tor_bridges_connection_progress_successful_title)),
                                BodyModule(description),
                                ButtonModule(resourceManager.getString(R.string.common_confirm), ButtonStyle.Normal) {
                                    _dissmissDialog.postValue(Unit)
                                    _backPressed.postValue(Unit)
                                 },
                            )
                        )
                        _modularDialog.postValue(args)
                    } else {
                        val description = resourceManager.getString(
                            R.string.tor_bridges_connection_progress_description_full,
                            it.bootstrapStatus.summary + ", " + it.bootstrapStatus.warning.orEmpty(),
                            it.bootstrapStatus.progress.toString()
                        )
                        val nextArgs = ProgressDialogArgs(
                            resourceManager.getString(R.string.tor_bridges_connection_progress_title),
                            description,
                            closeButtonText = resourceManager.getString(R.string.common_cancel),
                            cancelable = true
                        ) { stopConnecting() }
                        _loadingDialog.postValue(nextArgs)
                    }
                }
                else -> Unit
            }
        }
    }

    private fun stopConnecting() {
        EventBus.torProxyState.unsubscribe(this)
        torSharedRepository.currentTorBridges = TorBridgeConfigurationList()
        _dissmissDialog.postValue(Unit)
    }
}