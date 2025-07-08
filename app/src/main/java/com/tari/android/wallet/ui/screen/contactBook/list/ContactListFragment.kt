package com.tari.android.wallet.ui.screen.contactBook.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.composeContent

class ContactListFragment : CommonFragment<ContactListViewModel>() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = composeContent {
        val state by viewModel.uiState.collectAsState()

        TariDesignSystem(viewModel.currentTheme) {
            ContactListScreen(
                uiState = state,
                onBackClick = { viewModel.onBackPressed() },
                onAddContactClick = { viewModel.onAddContactClicked() },
                onSearchQueryChange = { viewModel.onQueryChange(it) },
                onContactItemClick = { viewModel.onContactItemClicked(it) },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ContactListViewModel by viewModels()
        bindViewModel(viewModel)

        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is ContactListViewModel.Effect.SetSelectResult -> {
                    setFragmentResult(CONTACT_REQUEST_KEY, Bundle().apply { putParcelable(CONTACT_RESULT_KEY, effect.selectedContact) })
                }
            }
        }
    }

    companion object {
        const val CONTACT_REQUEST_KEY = "CONTACT_REQUEST_KEY"
        const val CONTACT_RESULT_KEY = "CONTACT_RESULT_KEY"

        const val START_FOR_SELECT_RESULT = "START_FOR_SELECT_RESULT"

        fun newInstance(startForSelectResult: Boolean = false) = ContactListFragment().apply {
            arguments = Bundle().apply {
                putBoolean(START_FOR_SELECT_RESULT, startForSelectResult)
            }
        }
    }
}