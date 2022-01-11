/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <jni.h>
#include <android/log.h>
#include <wallet.h>
#include <string>
#include <cmath>
#include <android/log.h>
#include "jniCommon.cpp"

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIBalance_jniGetAvailable(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    jlong lBalance = GetPointerField(jEnv, jThis);
    auto *pBalance = reinterpret_cast<TariBalance *>(lBalance);
    jbyteArray result = getBytesFromUnsignedLongLong(
            jEnv,
            balance_get_available(pBalance, errorCodePointer)
    );
    setErrorCode(jEnv, error, errorCode);
    return result;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIBalance_jniGetIncoming(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    jlong lBalance = GetPointerField(jEnv, jThis);
    auto *pBalance = reinterpret_cast<TariBalance *>(lBalance);
    jbyteArray result = getBytesFromUnsignedLongLong(
            jEnv,
            balance_get_pending_incoming(pBalance, errorCodePointer)
    );
    setErrorCode(jEnv, error, errorCode);
    return result;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIBalance_jniGetOutgoing(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    jlong lBalance = GetPointerField(jEnv, jThis);
    auto *pBalance = reinterpret_cast<TariBalance *>(lBalance);
    jbyteArray result = getBytesFromUnsignedLongLong(
            jEnv,
            balance_get_pending_outgoing(pBalance, errorCodePointer)
    );
    setErrorCode(jEnv, error, errorCode);
    return result;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIBalance_jniGetTimeLocked(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    jlong lBalance = GetPointerField(jEnv, jThis);
    auto *pBalance = reinterpret_cast<TariBalance *>(lBalance);
    jbyteArray result = getBytesFromUnsignedLongLong(
            jEnv,
            balance_get_time_locked(pBalance, errorCodePointer)
    );
    setErrorCode(jEnv, error, errorCode);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIBalance_jniDestroy(
        JNIEnv *jEnv,
jobject jThis) {
jlong lByteVector = GetPointerField(jEnv, jThis);
balance_destroy(reinterpret_cast<TariBalance *>(lByteVector));
SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
}
