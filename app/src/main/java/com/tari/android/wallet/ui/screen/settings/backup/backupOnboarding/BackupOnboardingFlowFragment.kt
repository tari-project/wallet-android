package com.tari.android.wallet.ui.screen.settings.backup.backupOnboarding

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
import com.tari.android.wallet.databinding.FragmentBackupOnboardingFlowBinding
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.screen.home.HomeActivity
import com.tari.android.wallet.ui.screen.settings.backup.backupOnboarding.item.BackupOnboardingArgs
import com.tari.android.wallet.ui.screen.settings.backup.backupOnboarding.item.BackupOnboardingFlowDataSource
import com.tari.android.wallet.ui.screen.settings.backup.backupOnboarding.item.BackupOnboardingFlowItemFragment
import com.tari.android.wallet.util.extension.setVisible

class BackupOnboardingFlowFragment : CommonFragment<FragmentBackupOnboardingFlowBinding, BackupOnboardingFlowViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentBackupOnboardingFlowBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: BackupOnboardingFlowViewModel by viewModels()
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
            BackupOnboardingArgs.StageOne(viewModel.resourceManager) { openStage1() },
            BackupOnboardingArgs.StageTwo(viewModel.resourceManager) { openStage1B() },
            BackupOnboardingArgs.StageThree(viewModel.resourceManager) { openStage2() },
            BackupOnboardingArgs.StageFour(viewModel.resourceManager) { openStage3() },
        )

        init {
            BackupOnboardingFlowDataSource.save(args)
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

        override fun createFragment(position: Int): Fragment = BackupOnboardingFlowItemFragment.createInstance(position)
    }

    private fun onBackPressed() {
        HomeActivity.instance.get()?.onBackPressed()
    }
}

