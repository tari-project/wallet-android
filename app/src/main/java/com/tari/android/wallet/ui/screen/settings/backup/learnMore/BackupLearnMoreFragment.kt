package com.tari.android.wallet.ui.screen.settings.backup.learnMore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.tari.android.wallet.databinding.FragmentBackupLearnMoreBinding
import com.tari.android.wallet.ui.common.CommonXmlFragment
import com.tari.android.wallet.ui.screen.settings.backup.learnMore.item.BackupLearnMoreDataSource
import com.tari.android.wallet.ui.screen.settings.backup.learnMore.item.BackupLearnMoreItemFragment
import com.tari.android.wallet.ui.screen.settings.backup.learnMore.item.BackupLearnMoreStageArgs
import com.tari.android.wallet.util.extension.setVisible

class BackupLearnMoreFragment : CommonXmlFragment<FragmentBackupLearnMoreBinding, BackupLearnMoreViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentBackupLearnMoreBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: BackupLearnMoreViewModel by viewModels()
        bindViewModel(viewModel)

        setupUI()
    }

    private fun setupUI() = with(ui) {
        closeButton.setOnClickListener { requireActivity().onBackPressed() }
        nextButton.setOnClickListener { viewPager.currentItem += 1 }

        viewPager.adapter = BackupOnboardingFlowAdapter(requireActivity())

        ui.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                nextButton.setVisible(position != ui.viewPager.adapter!!.itemCount - 1)
            }
        })

        TabLayoutMediator(ui.viewPagerIndicators, ui.viewPager) { _, _ -> }.attach()
    }


    private inner class BackupOnboardingFlowAdapter(fm: FragmentActivity) : FragmentStateAdapter(fm) {

        private val args = arrayListOf(
            BackupLearnMoreStageArgs.StageOne(viewModel.resourceManager) { openStage1() },
            BackupLearnMoreStageArgs.StageTwo(viewModel.resourceManager) { openStage1B() },
            BackupLearnMoreStageArgs.StageThree(viewModel.resourceManager) { openStage2() },
            BackupLearnMoreStageArgs.StageFour(viewModel.resourceManager) { openStage3() },
        )

        init {
            BackupLearnMoreDataSource.save(args)
        }

        private fun openStage1() {
            onBackPressed()
            viewModel.navigateToWalletBackup()
        }

        private fun openStage1B() {
            onBackPressed()
        }

        private fun openStage2() {
            onBackPressed()
            viewModel.navigateToChangePassword()
        }

        private fun openStage3() {
            onBackPressed()
        }

        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment = BackupLearnMoreItemFragment.createInstance(position)
    }

    private fun onBackPressed() {
        activity?.onBackPressed()
    }
}

