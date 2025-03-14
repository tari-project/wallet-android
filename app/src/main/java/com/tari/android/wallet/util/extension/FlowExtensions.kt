package com.tari.android.wallet.util.extension

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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch

inline fun LifecycleOwner.launchAndRepeatOnLifecycle(
    state: Lifecycle.State,
    crossinline block: suspend CoroutineScope.() -> Unit,
) = lifecycleScope.launch { repeatOnLifecycle(state) { block() } }

fun <T> AppCompatActivity.collectFlow(stateFlow: Flow<T>, onError: (Throwable) -> Unit = {}, action: suspend (T) -> Unit): Job {
    return launchAndRepeatOnLifecycle(Lifecycle.State.STARTED) {
        try {
            stateFlow.collect { state -> action(state) }
        } catch (e: Exception) {
            e.printStackTrace()
            onError(e)
        }
    }
}

fun <T> Fragment.collectFlow(stateFlow: Flow<T>, onError: (Throwable) -> Unit = {}, action: suspend (T) -> Unit): Job {
    return viewLifecycleOwner.launchAndRepeatOnLifecycle(Lifecycle.State.STARTED) {
        try {
            stateFlow.collect { state -> action(state) }
        } catch (e: Exception) {
            e.printStackTrace()
            onError(e)
        }
    }
}

fun <T> Fragment.collectNonNullFlow(stateFlow: Flow<T?>, onError: (Throwable) -> Unit = {}, action: suspend (T) -> Unit): Job {
    return viewLifecycleOwner.launchAndRepeatOnLifecycle(Lifecycle.State.STARTED) {
        try {
            stateFlow.filter { it != null }.collect { state -> action(state!!) }
        } catch (e: Exception) {
            e.printStackTrace()
            onError(e)
        }
    }
}

fun <T> ViewModel.collectFlow(stateFlow: Flow<T>, onError: (Throwable) -> Unit = {}, action: suspend (T) -> Unit): Job {
    return viewModelScope.launch {
        try {
            stateFlow.collect { state -> action(state) }
        } catch (e: Exception) {
            e.printStackTrace()
            onError(e)
        }
    }
}

fun <T> ViewModel.collectNonNullFlow(stateFlow: Flow<T?>, onError: (Throwable) -> Unit = {}, action: suspend (T) -> Unit): Job {
    return viewModelScope.launch {
        try {
            stateFlow.filter { it != null }.collect { state -> action(state!!) }
        } catch (e: Exception) {
            e.printStackTrace()
            onError(e)
        }
    }
}

fun <T> CoroutineScope.collectFlow(stateFlow: Flow<T>, onError: (Throwable) -> Unit = {}, action: suspend (T) -> Unit): Job {
    return launch {
        try {
            stateFlow.collect { state -> action(state) }
        } catch (e: Exception) {
            e.printStackTrace()
            onError(e)
        }
    }
}

fun <A, B> Flow<A>.combineToPair(other: Flow<B>): Flow<Pair<A, B>> {
    return this.combine(other) { a, b -> a to b }
}

fun <A, B> Flow<A>.zipToPair(other: Flow<B>): Flow<Pair<A, B>> {
    return this.zip(other) { a, b -> a to b }
}