package com.tari.android.wallet.ui.screen.settings.backup.backupOnboarding.item

import android.os.Bundle
import android.view.*
import com.tari.android.wallet.databinding.FragmentBackupOnboardingFlowItemBinding
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.util.extension.setVisible

class BackupOnboardingFlowItemFragment : CommonFragment<FragmentBackupOnboardingFlowItemBinding, BackupOnboardingFlowItemViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentBackupOnboardingFlowItemBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val position = requireArguments().getInt(argsKey)
        val args = BackupOnboardingFlowDataSource.getByPostion(position)

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

        private val argsKey = "tari_wallet_onboarding_args"

        fun createInstance(position: Int): BackupOnboardingFlowItemFragment =
            BackupOnboardingFlowItemFragment().apply { arguments = Bundle().apply { putInt(argsKey, position) } }
    }
}