package com.tari.android.wallet.ui.screen.settings.bugReporting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.tari.android.wallet.databinding.FragmentBugsReportingBinding
import com.tari.android.wallet.ui.common.CommonXmlFragment
import com.tari.android.wallet.ui.screen.debug.DebugNavigation
import com.tari.android.wallet.ui.screen.debug.activity.DebugActivity

class BugsReportingFragment : CommonXmlFragment<FragmentBugsReportingBinding, BugsReportingViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentBugsReportingBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: BugsReportingViewModel by viewModels()
        bindViewModel(viewModel)

        setupUI()
        observeUI()
    }

    private fun setupUI() = with(ui) {
        sendButton.setOnClickListener {
            viewModel.send(
                name = nameEditText.ui.editText.text?.toString().orEmpty(),
                email = emailEditText.ui.editText.text?.toString().orEmpty(),
                bugDescription = bugDescription.text?.toString().orEmpty(),
            )
        }
        viewLogsButton.setOnClickListener { (requireActivity() as? DebugActivity)?.navigate(DebugNavigation.Logs, rooted = false) }
    }

    private fun observeUI() = with(viewModel) { }
}