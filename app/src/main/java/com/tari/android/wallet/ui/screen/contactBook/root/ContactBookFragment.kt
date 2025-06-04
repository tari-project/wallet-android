package com.tari.android.wallet.ui.screen.contactBook.root

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.FragmentContactBookRootBinding
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonXmlFragment
import com.tari.android.wallet.ui.component.clipboardController.ClipboardController
import com.tari.android.wallet.ui.component.tari.toolbar.TariToolbarActionArg
import com.tari.android.wallet.ui.screen.contactBook.contacts.ContactsFragment
import com.tari.android.wallet.ui.screen.contactBook.contacts.FavoritesFragment
import com.tari.android.wallet.ui.screen.contactBook.root.share.ShareOptionArgs
import com.tari.android.wallet.ui.screen.contactBook.root.share.ShareOptionView
import com.tari.android.wallet.ui.screen.qr.QrScannerSource
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.extension.hideKeyboard
import com.tari.android.wallet.util.extension.observe
import com.tari.android.wallet.util.extension.postDelayed
import com.tari.android.wallet.util.extension.setVisible
import com.tari.android.wallet.util.extension.showKeyboard
import com.tari.android.wallet.util.extension.string
import com.tari.android.wallet.util.extension.temporarilyDisableClick
import java.lang.ref.WeakReference

class ContactBookFragment : CommonXmlFragment<FragmentContactBookRootBinding, ContactBookViewModel>() {

    private var clipboardController: ClipboardController? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentContactBookRootBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ContactBookViewModel by viewModels()
        bindViewModel(viewModel)

        clipboardController = ClipboardController(listOf(ui.dimmerView), ui.clipboardWallet, viewModel.walletAddressViewModel)

        setupUI()

        subscribeUI()
    }

    override fun onResume() {
        super.onResume()
        viewModel.walletAddressViewModel.tryToCheckClipboard()
    }

    override fun onDestroy() {
        super.onDestroy()
        clipboardController?.onDestroy()
    }

    private fun subscribeUI() = with(viewModel) {
        observe(contactSelectionRepository.isSelectionState) { updateSharedState() }

        observe(walletAddressViewModel.discoveredWalletAddressFromClipboard) { clipboardController?.showClipboardData(it) }

        observe(walletAddressViewModel.discoveredWalletAddressFromQuery) { ui.sendButton.setVisible(it != null) }

        observe(contactSelectionRepository.isPossibleToShare) { updateSharedState() }

        observe(shareList) { updateShareList(it) }

        observe(query) { ui.searchView.setQuery(it, true) }
    }

    private fun setupUI() {
        ui.viewPager.adapter = ContactBookAdapter(requireActivity())

        ui.qrCodeButton.setOnClickListener { onQRButtonClick(it) }

        TabLayoutMediator(ui.viewPagerIndicators, ui.viewPager) { tab, position ->
            tab.setText(
                when (position) {
                    0 -> R.string.contact_book_contacts_title
                    1 -> R.string.contact_book_favorites_title
                    else -> error("Invalid tab position")
                }
            )
        }.attach()

        ui.searchView.setIconifiedByDefault(false)

        ui.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                (ui.viewPager.adapter as ContactBookAdapter).fragments.forEach { it.get()?.search(newText.orEmpty()) }
                viewModel.doSearch(newText.orEmpty())
                return true
            }

            override fun onQueryTextSubmit(query: String?): Boolean = true
        })

        ui.sendButton.setOnClickListener {
            clearFocusOnSearch()
            viewModel.send()
        }

        clipboardController?.listener = object : ClipboardController.ClipboardControllerListener {

            override fun onPaste(walletAddress: TariWalletAddress) {
                ui.searchView.setQuery(viewModel.walletAddressViewModel.discoveredWalletAddressFromClipboard.value?.fullEmojiId, false)
            }

            override fun focusOnEditText(isFocused: Boolean) {
                if (isFocused) {
                    focusEditTextAndShowKeyboard()
                } else {
                    clearFocusOnSearch()
                }
            }
        }
    }

    private fun onQRButtonClick(view: View) {
        view.temporarilyDisableClick()
        requireActivity().hideKeyboard()
        clipboardController?.hidePasteEmojiIdViews(animate = true) {
            ui.rootView.postDelayed(Constants.UI.keyboardHideWaitMs) { startQRCodeActivity() }
        }
    }

    private fun startQRCodeActivity() {
        startQrScanner(QrScannerSource.ContactBook)
    }

    private fun focusEditTextAndShowKeyboard() {
        getRealSearch().postDelayed(150) {
            getRealSearch().requestFocus()
            requireActivity().showKeyboard(getRealSearch())
        }
    }

    private fun clearFocusOnSearch() {
        requireActivity().hideKeyboard(getRealSearch())
        getRealSearch().clearFocus()
    }

    private fun updateSharedState() {
        val sharedState = viewModel.contactSelectionRepository.isSelectionState.value == true
        val possibleToShare = viewModel.contactSelectionRepository.isPossibleToShare.value == true

        if (sharedState) {
            val shareArgs = TariToolbarActionArg(title = string(R.string.common_share), isDisabled = possibleToShare.not()) {
                viewModel.shareSelectedContacts()
            }
            ui.toolbar.setRightArgs(shareArgs)
            val cancelArgs = TariToolbarActionArg(title = string(R.string.common_cancel)) {
                viewModel.contactSelectionRepository.isSelectionState.postValue(false)
            }
            ui.toolbar.setLeftArgs(cancelArgs)
        } else {
            val addContactArg = TariToolbarActionArg(icon = R.drawable.vector_add_contact) {
                viewModel.tariNavigator.navigate(Navigation.ContactBook.ToAddContact)
            }
            val shareContactArg = TariToolbarActionArg(icon = R.drawable.vector_share_dots) {
                viewModel.contactSelectionRepository.isSelectionState.postValue(true)
            }
            ui.toolbar.setLeftArgs()
            ui.toolbar.setRightArgs(shareContactArg, addContactArg)
        }
        ui.shareTypesContainer.setVisible(sharedState)
    }

    private fun updateShareList(list: List<ShareOptionArgs>) {
        ui.shareTypesContainer.removeAllViews()
        for (item in list) {
            val shareOption = ShareOptionView(requireContext()).apply { setArgs(item) }
            ui.shareTypesContainer.addView(shareOption)
            shareOption.updateLayoutParams<LinearLayout.LayoutParams> {
                width = 0
                weight = 1f
            }
        }
    }

    private fun getRealSearch(): View = ui.searchView.findViewById(R.id.search_src_text)

    private inner class ContactBookAdapter(fm: FragmentActivity) : FragmentStateAdapter(fm) {

        val fragments = mutableListOf<WeakReference<ContactsFragment>>()

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> ContactsFragment()
            else -> FavoritesFragment()
        }.also { fragments.add(WeakReference(it)) }
    }
}

