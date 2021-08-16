package com.tari.android.wallet.extension

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import com.tari.android.wallet.ui.component.common.CommonView

fun <T> Fragment.observe(liveData: LiveData<T>, action: (data: T) -> Unit) {
    liveData.observe(this.viewLifecycleOwner, {
        try {
            action.invoke(it)
        } catch (e: Exception) {
            println(e)
        }
    })
}

fun <T> Fragment.observeOnLoad(liveData: LiveData<T>) {
    observe(liveData) { }
}

fun <T> AppCompatActivity.observe(liveData: LiveData<T>, action: (data: T) -> Unit) {
    liveData.observe(this, {
        try {
            action.invoke(it)
        } catch (e: Exception) {
            println(e)
        }
    })
}

fun <T> AppCompatActivity.observeOnLoad(liveData: LiveData<T>) {
    observe(liveData) { }
}

fun <T> CommonView<*, *>.observe(liveData: LiveData<T>, action: (data: T) -> Unit) {
    liveData.observe(viewLifecycle, {
        try {
            action.invoke(it)
        } catch (e: Exception) {
            println(e)
        }
    })
}

fun <T> CommonView<*, *>.observeOnLoad(liveData: LiveData<T>) {
    observe(liveData) { }
}