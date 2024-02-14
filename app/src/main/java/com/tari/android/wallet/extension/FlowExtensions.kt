package com.tari.android.wallet.extension

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

inline fun LifecycleOwner.launchAndRepeatOnLifecycle(
    state: Lifecycle.State,
    crossinline block: suspend CoroutineScope.() -> Unit,
) = lifecycleScope.launch { repeatOnLifecycle(state) { block() } }
