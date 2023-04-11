package com.tari.android.wallet.application.deeplinks

import android.content.Context
import com.tari.android.wallet.R
import com.tari.android.wallet.application.baseNodes.BaseNodes
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeSharedRepository
import com.tari.android.wallet.ffi.FFITariWalletAddress
import com.tari.android.wallet.ffi.HexString
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.confirm.ConfirmDialogArgs
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialog
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.FFIContactDto
import javax.inject.Inject

class DeeplinkViewModel : CommonViewModel() {

    @Inject
    lateinit var baseNodes: BaseNodes

    @Inject
    lateinit var baseNodeRepository: BaseNodeSharedRepository

    @Inject
    lateinit var contactRepository: ContactsRepository

    init {
        component.inject(this)
    }

    fun executeAction(context: Context, deeplink: DeepLink.AddBaseNode) {
        val baseNode = BaseNodeDto.fromDeeplink(deeplink)
        val args = ConfirmDialogArgs(
            resourceManager.getString(R.string.home_custom_base_node_title),
            resourceManager.getString(R.string.home_custom_base_node_description),
            resourceManager.getString(R.string.home_custom_base_node_no_button),
            resourceManager.getString(R.string.common_lets_do_it),
            onConfirm = { addBaseNode(baseNode) }
        ).getModular(baseNode, resourceManager)
        ModularDialog(context, args).show()
    }

    fun addContacts(contacts: List<DeepLink.Contacts.DeeplinkContact>) {
        val contactDtos = contacts.mapNotNull {
            runCatching {
                val ffiWalletAddress = FFITariWalletAddress(HexString(it.hex))
                val tariWalletAddress = TariWalletAddress(ffiWalletAddress.toString(), ffiWalletAddress.getEmojiId())
                ContactDto(FFIContactDto(tariWalletAddress, it.alias))
            }.getOrNull()
        }
        if (contactDtos.isEmpty()) return
        val names = contactDtos.joinToString(", ") { it.contact.getAlias().trim() }
        val args = ModularDialogArgs(
            DialogArgs(), listOf(
                HeadModule(resourceManager.getString(R.string.contact_deeplink_title)),
                BodyModule(resourceManager.getString(R.string.contact_deeplink_message, contactDtos.size.toString()) + ". " +  names),
                ButtonModule(resourceManager.getString(R.string.common_confirm), ButtonStyle.Normal) {
                    contactDtos.forEach { contactRepository.addContact(it) }
                    _dismissDialog.postValue(Unit)
                },
                ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
            )
        )
        _modularDialog.postValue(args)
    }

    private fun addBaseNode(baseNodeDto: BaseNodeDto) {
        baseNodeRepository.addUserBaseNode(baseNodeDto)
        baseNodes.setBaseNode(baseNodeDto)
    }
}