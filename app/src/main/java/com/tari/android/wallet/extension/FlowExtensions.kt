package com.tari.android.wallet.extension

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

inline fun LifecycleOwner.launchAndRepeatOnLifecycle(
    state: Lifecycle.State,
    crossinline block: suspend CoroutineScope.() -> Unit,
) = lifecycleScope.launch { repeatOnLifecycle(state) { block() } }

fun <T> AppCompatActivity.collectFlow(stateFlow: Flow<T>, action: (T) -> Unit): Job {
    return launchAndRepeatOnLifecycle(Lifecycle.State.STARTED) {
        stateFlow.collect { state ->
            action(state)
        }
    }
}

fun <T> Fragment.collectFlow(stateFlow: Flow<T>, action: (T) -> Unit): Job {
    return viewLifecycleOwner.launchAndRepeatOnLifecycle(Lifecycle.State.STARTED) {
        stateFlow.collect { state ->
            action(state)
        }
    }
}

fun <T> ViewModel.collectFlow(stateFlow: Flow<T>, action: (T) -> Unit): Job {
    return viewModelScope.launch {
        stateFlow.collect { state ->
            action(state)
        }
    }
}