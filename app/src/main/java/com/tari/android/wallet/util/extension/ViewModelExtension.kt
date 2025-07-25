package com.tari.android.wallet.util.extension

import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orhanobut.logger.Logger
import com.tari.android.wallet.ui.component.common.CommonView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val logger
    get() = Logger.t("ViewModelExtension")

fun <T> Fragment.observe(liveData: LiveData<T>, action: (data: T) -> Unit) {
    liveData.observe(this.viewLifecycleOwner) {
        try {
            action.invoke(it)
        } catch (e: Exception) {
            logger.i(e.toString())
        }
    }
}

fun <T> Fragment.observeOnLoad(liveData: LiveData<T>) {
    observe(liveData) { }
}

fun <T> AppCompatActivity.observe(liveData: LiveData<T>, action: (data: T) -> Unit) {
    liveData.observe(this) {
        try {
            action.invoke(it)
        } catch (e: Exception) {
            logger.i(e.toString())
        }
    }
}

fun <T> CommonView<*, *>.observe(liveData: LiveData<T>, action: (data: T) -> Unit) {
    liveData.observe(viewLifecycle) {
        try {
            action.invoke(it)
        } catch (e: Exception) {
            logger.i(e.toString())
        }
    }
}

fun <T> LiveData<T>.debounce(duration: Long = 1000L) = MediatorLiveData<T>().also { mld ->
    val source = this
    val handler = Handler(Looper.getMainLooper())

    val runnable = Runnable {
        mld.value = source.value
    }

    mld.addSource(source) {
        handler.removeCallbacks(runnable)
        handler.postDelayed(runnable, duration)
    }
}

fun ViewModel.launchOnIo(action: suspend () -> Unit): Job {
    return viewModelScope.launch(Dispatchers.IO) {
        action()
    }
}

fun ViewModel.launchOnMain(action: suspend () -> Unit): Job {
    return viewModelScope.launch(Dispatchers.Main) {
        action()
    }
}

suspend fun <T> switchToIo(action: suspend () -> T): T = withContext(Dispatchers.IO) { action() }

suspend fun <T> switchToMain(action: suspend () -> T): T = withContext(Dispatchers.Main) { action() }
