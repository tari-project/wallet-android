package com.tari.android.wallet.ui.fragment.contact_book.root

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.ui.common.CommonViewModel

class ContactBookViewModel : CommonViewModel() {
    val navigation = MutableLiveData<ContactBookNavigation>()

    fun navigate(navigation: ContactBookNavigation) {
        this.navigation.postValue(navigation)
    }
}