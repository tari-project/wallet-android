package com.tari.android.wallet.ui.fragment.restore.recoverFromSeedWords

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.tari.android.wallet.databinding.FragmentWalletRestoringFromSeedWordsBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.activity.restore.WalletRestoreRouter
import com.tari.android.wallet.ui.common.CommonFragment

class RecoveringFromSeedWordsFragment : CommonFragment<FragmentWalletRestoringFromSeedWordsBinding, RecoveringFromSeedWordsViewModel>() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentWalletRestoringFromSeedWordsBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: RecoveringFromSeedWordsViewModel by viewModels()
        bindViewModel(viewModel)

        setupUI()

        observeUI()
    }

    private fun setupUI() = with(ui) {

    }

    private fun observeUI() = with(viewModel) {
        observe(navigation) { processNavigation(it) }
    }

    private fun processNavigation(navigation: RecoveringFromSeedWordsNavigation) {
        val router = requireActivity() as WalletRestoreRouter
        when (navigation) {
            RecoveringFromSeedWordsNavigation.ToRestoreInProgress -> router.toRestoreInProgress()
        }
    }

    companion object {
        fun newInstance() = RecoveringFromSeedWordsFragment()
    }
}