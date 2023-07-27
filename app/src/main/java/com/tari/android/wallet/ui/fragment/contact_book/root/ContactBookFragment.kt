package com.tari.android.wallet.ui.fragment.contact_book.root

import android.app.Activity
import android.content.Intent
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
import com.tari.android.wallet.application.deeplinks.DeeplinkViewModel
import com.tari.android.wallet.databinding.FragmentContactBookRootBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.component.clipboardController.ClipboardController
import com.tari.android.wallet.ui.component.tari.toolbar.TariToolbarActionArg
import com.tari.android.wallet.ui.extension.hideKeyboard
import com.tari.android.wallet.ui.extension.postDelayed
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.extension.showKeyboard
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.extension.temporarilyDisableClick
import com.tari.android.wallet.ui.fragment.contact_book.contacts.ContactsFragment
import com.tari.android.wallet.ui.fragment.contact_book.favorites.FavoritesFragment
import com.tari.android.wallet.ui.fragment.contact_book.root.share.ShareOptionArgs
import com.tari.android.wallet.ui.fragment.contact_book.root.share.ShareOptionView
import com.tari.android.wallet.ui.fragment.home.HomeActivity
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.ui.fragment.qr.QRScannerActivity
import com.tari.android.wallet.ui.fragment.qr.QrScannerSource
import com.tari.android.wallet.util.Constants
import java.lang.ref.WeakReference

class ContactBookFragment : CommonFragment<FragmentContactBookRootBinding, ContactBookViewModel>() {

    private lateinit var clipboardController: ClipboardController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentContactBookRootBinding.inflate(inflater, container, false).also { ui = it }.root

    private val deeplinkViewModel: DeeplinkViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ContactBookViewModel by viewModels()
        bindViewModel(viewModel)
        subscribeVM(deeplinkViewModel)

        clipboardController = ClipboardController(listOf(ui.dimmerView), ui.clipboardWallet, viewModel.walletAddressViewModel)

        setupUI()

        subscribeUI()

        grantPermission()

//        initTests()
    }

    override fun onResume() {
        super.onResume()
        viewModel.walletAddressViewModel.tryToCheckClipboard()
        grantPermission()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == QRScannerActivity.REQUEST_QR_SCANNER && resultCode == Activity.RESULT_OK && data != null) {
            val qrData = data.getStringExtra(QRScannerActivity.EXTRA_QR_DATA) ?: return
            deeplinkViewModel.tryToHandle(qrData)
        }
    }

    private fun subscribeUI() = with(viewModel) {
        observe(contactsRepository.loadingState) {
            ui.isSyncingProgressBar.setVisible(it.isLoading)
            ui.syncingStatus.text = it.name + " " + it.time + "s"
        }

        observe(contactSelectionRepository.isSelectionState) { updateSharedState() }

        observe(walletAddressViewModel.discoveredWalletAddressFromClipboard) { clipboardController.showClipboardData(it) }

        observe(walletAddressViewModel.discoveredWalletAddressFromQuery) { ui.sendButton.setVisible(it != null) }

        observe(contactSelectionRepository.isPossibleToShare) { updateSharedState() }

        observe(shareList) { updateShareList(it) }
    }

    private fun grantPermission() {
        permissionManagerUI.runWithPermission(android.Manifest.permission.READ_CONTACTS, false) {
            viewModel.contactsRepository.contactPermission.value = true
            viewModel.contactsRepository.phoneBookRepositoryBridge.loadFromPhoneBook()
        }
    }

    private fun initTests() {
        ui.testButtons.visibility = View.VISIBLE
        ui.removeAllButton.setOnClickListener { viewModel.contactsRepository.phoneBookRepositoryBridge.clean() }
        ui.add1000Button.setOnClickListener { viewModel.contactsRepository.phoneBookRepositoryBridge.addTestContacts() }
    }

    private fun setupUI() {
        ui.viewPager.adapter = ContactBookAdapter(requireActivity())

        ui.qrCodeButton.setOnClickListener { onQRButtonClick(it) }

        TabLayoutMediator(ui.viewPagerIndicators, ui.viewPager) { tab, position ->
            tab.setText(
                when (position) {
                    0 -> R.string.contact_book_contacts_title
                    else -> R.string.contact_book_favorites_title
                }
            )
        }.attach()

        ui.searchView.setOnQueryTextFocusChangeListener { _, hasFocus -> (requireActivity() as? HomeActivity)?.setBottomBarVisibility(!hasFocus) }

        ui.searchView.setOnFocusChangeListener { _, hasFocus -> (requireActivity() as? HomeActivity)?.setBottomBarVisibility(!hasFocus) }

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

        clipboardController.listener = object : ClipboardController.ClipboardControllerListener {

            override fun onPaste(walletAddress: TariWalletAddress) {
                ui.searchView.setQuery(viewModel.walletAddressViewModel.discoveredWalletAddressFromClipboard.value?.emojiId, false)
            }

            override fun focusOnEditText(isFocused: Boolean) {
                (requireActivity() as? HomeActivity)?.setBottomBarVisibility(!isFocused)
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
        clipboardController.hidePasteEmojiIdViews(animate = true) {
            ui.rootView.postDelayed(Constants.UI.keyboardHideWaitMs) { startQRCodeActivity() }
        }
    }

    private fun startQRCodeActivity() {
        QRScannerActivity.startScanner(requireActivity(), QrScannerSource.ContactBook)
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
        val sharedState = viewModel.contactSelectionRepository.isSelectionState.value ?: false
        val possibleToShare = viewModel.contactSelectionRepository.isPossibleToShare.value ?: false

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
                viewModel.navigation.postValue(Navigation.ContactBookNavigation.ToAddContact)
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

    private fun getRealSearch(): View = ui.searchView.findViewById(androidx.appcompat.R.id.search_src_text)

    private inner class ContactBookAdapter(fm: FragmentActivity) : FragmentStateAdapter(fm) {

        val fragments = mutableListOf<WeakReference<ContactsFragment>>()

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> ContactsFragment()
            else -> FavoritesFragment()
        }.also { fragments.add(WeakReference(it)) }
    }
}

