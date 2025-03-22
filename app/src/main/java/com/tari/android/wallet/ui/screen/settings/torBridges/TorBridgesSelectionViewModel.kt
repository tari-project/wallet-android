package com.tari.android.wallet.ui.screen.settings.torBridges

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.data.sharedPrefs.tor.TorBridgeConfigurationList
import com.tari.android.wallet.data.sharedPrefs.tor.TorPrefRepository
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.tor.TorProxyManager
import com.tari.android.wallet.tor.TorProxyState
import com.tari.android.wallet.tor.TorProxyStateHandler
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.SimpleDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.shareQr.ShareQrCodeModule
import com.tari.android.wallet.ui.screen.settings.torBridges.torItem.TorBridgeViewHolderItem
import com.tari.android.wallet.util.DebugConfig
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.launchOnMain
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TorBridgesSelectionViewModel : CommonViewModel() {

    @Inject
    lateinit var torSharedRepository: TorPrefRepository

    @Inject
    lateinit var torProxyManager: TorProxyManager

    @Inject
    lateinit var torProxyStateHandler: TorProxyStateHandler

    private val _torBridges = MutableLiveData<MutableList<TorBridgeViewHolderItem>>()
    val torBridges: LiveData<MutableList<TorBridgeViewHolderItem>> = _torBridges

    private var torStateCollectingJob: Job? = null

    init {
        component.inject(this)
        loadData()
    }

    fun loadData() {
        val noBridges = TorBridgeViewHolderItem.Empty(resourceManager)
        val customBridges = TorBridgeViewHolderItem.CustomBridges(resourceManager)

        val bridges = mutableListOf<TorBridgeViewHolderItem.Bridge>()
        bridges.addAll(torSharedRepository.customTorBridges.map { TorBridgeViewHolderItem.Bridge(it, false) })
        if (torSharedRepository.currentTorBridges.isEmpty()) {
            noBridges.isSelected = true
        } else {
            bridges.forEach {
                if (torSharedRepository.currentTorBridges.contains(it.bridgeConfiguration)) {
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
            is TorBridgeViewHolderItem.CustomBridges -> tariNavigator.navigate(Navigation.TorBridge.ToCustomBridges)
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
        _torBridges.postValue(_torBridges.value!!.map { it.deepCopy() as TorBridgeViewHolderItem }.toMutableList())
    }

    fun showBridgeQrCode(torBridgeItem: TorBridgeViewHolderItem) {
        if (torBridgeItem !is TorBridgeViewHolderItem.Bridge) return
        val data = deeplinkManager.getDeeplinkString(DeepLink.TorBridges(listOf(torBridgeItem.bridgeConfiguration)))
        showModularDialog(
            ModularDialogArgs(
                DialogArgs(true, canceledOnTouchOutside = true), listOf(
                    HeadModule(resourceManager.getString(R.string.share_via_qr_code_title)),
                    ShareQrCodeModule(data),
                    ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close),
                )
            )
        )
    }

    fun connect() {
        val selectedBridges = _torBridges.value.orEmpty().filter { it.isSelected }
        if (selectedBridges.any { it is TorBridgeViewHolderItem.Empty }) {
            torSharedRepository.currentTorBridges = TorBridgeConfigurationList(emptyList())
        } else {
            torSharedRepository.currentTorBridges =
                TorBridgeConfigurationList(selectedBridges.map { (it as TorBridgeViewHolderItem.Bridge).bridgeConfiguration })
        }

        restartTor()
    }

    private fun restartTor() {
        showModularDialog(
            SimpleDialogArgs(
                title = resourceManager.getString(R.string.tor_bridges_connection_progress_title),
                description = resourceManager.getString(R.string.tor_bridges_connection_progress_description),
                closeButtonTextRes = R.string.common_cancel,
                cancelable = true,
                onClose = { stopConnecting() },
            ).getModular(resourceManager)
        )
        torProxyManager.shutdown()
        subscribeToTorState()
        torProxyManager.run()
        if (DebugConfig.selectBaseNodeEnabled) {
            walletManager.syncBaseNode()
        }
    }

    private fun subscribeToTorState() {
        launchOnIo {
            torProxyStateHandler.doOnTorFailed {
                launchOnMain {
                    showModularDialog(
                        SimpleDialogArgs(
                            title = resourceManager.getString(R.string.tor_bridges_connecting_error_title),
                            description = resourceManager.getString(R.string.tor_bridges_connecting_error_description, it.e.message.orEmpty()),
                            cancelable = true,
                            onClose = { stopConnecting() },
                        ).getModular(resourceManager)
                    )
                }
            }
        }

        torStateCollectingJob = collectFlow(
            torProxyStateHandler.torProxyState
                .filter { it is TorProxyState.Running }
                .map { it as TorProxyState.Running }
        ) {
            launchOnMain {
                if (it.bootstrapStatus.progress == 100) {
                    var description = resourceManager.getString(R.string.tor_bridges_connection_progress_successful_description)
                    if (torSharedRepository.currentTorBridges.isEmpty()) {
                        description += resourceManager.getString(R.string.tor_bridges_connection_progress_successful_no_bridges)
                    } else {
                        description += resourceManager.getString(R.string.tor_bridges_connection_progress_successful_used_bridges)
                        for (bridge in torSharedRepository.currentTorBridges) {
                            description += "${bridge.ip}:${bridge.port}\n"
                        }
                    }
                    showModularDialog(
                        ModularDialogArgs(
                            DialogArgs(cancelable = false, canceledOnTouchOutside = false), modules = listOf(
                                HeadModule(resourceManager.getString(R.string.tor_bridges_connection_progress_successful_title)),
                                BodyModule(description),
                                ButtonModule(resourceManager.getString(R.string.common_confirm), ButtonStyle.Normal) {
                                    hideDialog()
                                    backPressed.postValue(Unit)
                                },
                            )
                        )
                    )
                    torStateCollectingJob?.cancel()
                } else {
                    val description = resourceManager.getString(
                        R.string.tor_bridges_connection_progress_description_full,
                        it.bootstrapStatus.summary + ", " + it.bootstrapStatus.warning.orEmpty(),
                        it.bootstrapStatus.progress.toString()
                    )
                    showModularDialog(
                        SimpleDialogArgs(
                            title = resourceManager.getString(R.string.tor_bridges_connection_progress_title),
                            description = description,
                            closeButtonTextRes = R.string.common_cancel,
                            cancelable = true,
                            onClose = { stopConnecting() },
                        ).getModular(resourceManager)
                    )
                }
            }
        }
    }

    private fun stopConnecting() {
        torStateCollectingJob?.cancel()
        torSharedRepository.currentTorBridges = TorBridgeConfigurationList()
        hideDialog()
    }
}