package com.tari.android.wallet.ffi

abstract class FinalizerBase
{
    abstract fun destroy()
    protected fun finalize()
    {
        destroy()
    }
}