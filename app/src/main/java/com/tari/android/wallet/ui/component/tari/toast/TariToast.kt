package com.tari.android.wallet.ui.component.tari.toast

import android.content.Context
import android.widget.Toast

class TariToast(context: Context, args: TariToastArgs) {

    init {
        Toast.makeText(context, args.text, args.length).show()
    }
}

