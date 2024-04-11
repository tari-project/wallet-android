package com.tari.android.wallet.ffi

class FFITariUnblindedOutput() : FFIBase() {

    private external fun jniToJson(libError: FFIError): String
    private external fun jniFromJson(json: String, libError: FFIError)
    private external fun jniDestroy()

    constructor(pointer: FFIPointer) : this() {
        this.pointer = pointer
    }

    constructor(json: String) : this() {
        runWithError { jniFromJson(json, it) }
    }

    fun toJson(): String = runWithError { jniToJson(it) }

    override fun destroy() = jniDestroy()
}