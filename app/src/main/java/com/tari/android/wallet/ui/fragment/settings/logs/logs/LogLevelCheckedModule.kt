package com.tari.android.wallet.ui.fragment.settings.logs.logs

import com.tari.android.wallet.ui.dialog.modular.IDialogModule
import com.tari.android.wallet.ui.dialog.modular.modules.checked.CheckedModule

class LogLevelCheckedModule(val logFilter: LogLevelFilters, val checkedModule: CheckedModule) : IDialogModule()