package com.tari.android.wallet.ui.screen.settings.dataCollection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.tari.android.wallet.databinding.FragmentDataCollectionBinding
import com.tari.android.wallet.ui.common.CommonXmlFragment
import com.tari.android.wallet.ui.component.loadingSwitch.TariLoadingSwitchState
import com.tari.android.wallet.util.extension.observe

class DataCollectionFragment : CommonXmlFragment<FragmentDataCollectionBinding, DataCollectionViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentDataCollectionBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: DataCollectionViewModel by viewModels()
        bindViewModel(viewModel)

        ui.loadingSwitchView.setOnCheckedChangeListener {
            viewModel.updateState(it)
        }

        observe(viewModel.state) {
            ui.loadingSwitchView.setState(TariLoadingSwitchState(it, false))
        }
    }
}

