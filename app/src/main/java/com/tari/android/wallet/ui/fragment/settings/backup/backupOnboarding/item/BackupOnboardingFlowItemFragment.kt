package com.tari.android.wallet.ui.fragment.settings.backup.backupOnboarding.item

import android.os.Bundle
import android.view.*
import com.tari.android.wallet.databinding.FragmentBackupOnboardingFlowItemBinding
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.extension.serializable

class BackupOnboardingFlowItemFragment : CommonFragment<FragmentBackupOnboardingFlowItemBinding, BackupOnboardingFlowItemViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentBackupOnboardingFlowItemBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = requireArguments().serializable<BackupOnboardingArgs>(argsKey)
        //todo set values
    }

    companion object {

        private val argsKey = "tari_wallet_onboarding_args"

        fun createInstance(args: BackupOnboardingArgs): BackupOnboardingFlowItemFragment =
            BackupOnboardingFlowItemFragment().apply { arguments = Bundle().apply { putSerializable(argsKey, args) } }
    }
}