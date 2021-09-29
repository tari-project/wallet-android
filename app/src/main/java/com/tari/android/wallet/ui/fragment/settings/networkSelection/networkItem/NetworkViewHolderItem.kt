package com.tari.android.wallet.ui.fragment.settings.networkSelection.networkItem

import com.tari.android.wallet.application.Network
import com.tari.android.wallet.data.network.TariNetwork
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

class NetworkViewHolderItem(val network: TariNetwork, var currentNetwork: Network) : CommonViewHolderItem()