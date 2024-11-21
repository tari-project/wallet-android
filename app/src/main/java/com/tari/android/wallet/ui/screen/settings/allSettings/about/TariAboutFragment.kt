package com.tari.android.wallet.ui.screen.settings.allSettings.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.FragmentTariAboutBinding
import com.tari.android.wallet.util.extension.observe
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.screen.settings.allSettings.about.list.TariIconsAdapter
import yat.android.ui.extension.HtmlHelper

class TariAboutFragment : CommonFragment<FragmentTariAboutBinding, TariAboutViewModel>() {

    private val adapter = TariIconsAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentTariAboutBinding.inflate(inflater, container, false).apply { ui = this }.rootView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel: TariAboutViewModel by viewModels()
        bindViewModel(viewModel)

        ui.iconsList.adapter = adapter
        ui.iconsList.layoutManager = LinearLayoutManager(requireContext())
        adapter.setClickListener(CommonAdapter.ItemClickListener { viewModel.openUrl(it) })
        ui.description.setOnClickListener { viewModel.openLicense() }
        ui.description.text = HtmlHelper.getSpannedText(getString(R.string.tari_about_description))

        subscribeVM()
    }

    private fun subscribeVM() = with(viewModel) {
        observe(iconList) { adapter.update(it) }
    }
}