package com.tari.android.wallet.ui.dialog.modular.modules.option

import com.tari.android.wallet.ui.dialog.modular.IDialogModule

class OptionModule(val text: String? = null, action: () -> Unit = {}) : IDialogModule()