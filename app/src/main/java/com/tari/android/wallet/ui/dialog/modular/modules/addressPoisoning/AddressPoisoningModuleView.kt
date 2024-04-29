package com.tari.android.wallet.ui.dialog.modular.modules.addressPoisoning

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.DialogModuleAddressPoisoningBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.ui.dialog.modular.modules.addressPoisoning.adapter.SimilarAddressListAdapter
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.fragment.contact_book.address_poisoning.SimilarAddressItem


@SuppressLint("ViewConstructor")
class AddressPoisoningModuleView(
    context: Context,
    private val addressPoisoningModule: AddressPoisoningModule,
) : CommonView<CommonViewModel, DialogModuleAddressPoisoningBinding>(context) {

    private var recyclerViewAdapter = SimilarAddressListAdapter()

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleAddressPoisoningBinding =
        DialogModuleAddressPoisoningBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        ui.titleDescription.text = string(R.string.address_poisoning_title_description, addressPoisoningModule.addresses.size)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        ui.similarAddressListRecyclerView.layoutManager = LinearLayoutManager(context)
        recyclerViewAdapter.setClickListener(CommonAdapter.ItemClickListener { holderItem ->
            when (holderItem) {
                is SimilarAddressItem -> {
                    addressPoisoningModule.selectedAddress?.let {
                        it.selected = false
                        recyclerViewAdapter.notifyItemChanged(addressPoisoningModule.addresses.indexOf(it))
                    }
                    holderItem.selected = !holderItem.selected
                    recyclerViewAdapter.notifyItemChanged(addressPoisoningModule.addresses.indexOf(holderItem))
                }
            }
        })
        ui.similarAddressListRecyclerView.adapter = recyclerViewAdapter
        recyclerViewAdapter.update(addressPoisoningModule.addresses)
    }
}