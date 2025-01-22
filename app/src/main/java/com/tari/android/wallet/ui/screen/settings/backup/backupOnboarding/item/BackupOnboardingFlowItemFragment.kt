package com.tari.android.wallet.ui.screen.settings.backup.backupOnboarding.item

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tari.android.wallet.databinding.FragmentBackupOnboardingFlowItemBinding
import com.tari.android.wallet.ui.common.CommonXmlFragment
import com.tari.android.wallet.util.extension.setVisible

class BackupOnboardingFlowItemFragment : CommonXmlFragment<FragmentBackupOnboardingFlowItemBinding, BackupOnboardingFlowItemViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentBackupOnboardingFlowItemBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val position = requireArguments().getInt(ARGS_KEY)
        val args = BackupOnboardingFlowDataSource.getByPosition(position)

        with(ui) {
            imageView.setImageResource(args.image)
            title.text = args.title
            description.text = args.description
            moduleButton.initItem(args.button) { }
            textBelow.text = args.bottomText

            divider.setVisible(args.bottomText.isNotEmpty())
            textBelow.setVisible(args.bottomText.isNotEmpty())
        }
    }

    companion object {

        private const val ARGS_KEY = "tari_wallet_onboarding_args"

        fun createInstance(position: Int): BackupOnboardingFlowItemFragment =
            BackupOnboardingFlowItemFragment().apply { arguments = Bundle().apply { putInt(ARGS_KEY, position) } }
    }
}