package com.tari.android.wallet.ui.screen.settings.logs.logs.module

import com.tari.android.wallet.ui.dialog.modular.IDialogModule
import com.tari.android.wallet.ui.dialog.modular.modules.checked.CheckedModule

class LogSourceCheckedModule(val logFilter: LogSourceFilters, val checkedModule: CheckedModule) : IDialogModule()