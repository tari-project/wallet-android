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
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_FFIContacts_jniGetLength(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    auto pContacts = GetPointerField<TariContacts *>(jEnv, jThis);
    jint result = contacts_get_length(pContacts, errorCodePointer);
    setErrorCode(jEnv, error, errorCode);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIContacts_jniGetAt(
        JNIEnv *jEnv,
        jobject jThis,
        jint index,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    auto pContacts = GetPointerField<TariContacts *>(jEnv, jThis);
    auto result = reinterpret_cast<jlong>(contacts_get_at(pContacts, static_cast<unsigned int>(index), errorCodePointer));
    setErrorCode(jEnv, error, errorCode);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIContacts_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    contacts_destroy(GetPointerField<TariContacts *>(jEnv, jThis));
    SetNullPointerField(jEnv, jThis);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_FFICompletedTxs_jniGetLength(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    auto pCompletedTransactions = GetPointerField<TariCompletedTransactions *>(jEnv, jThis);
    jint result = completed_transactions_get_length(pCompletedTransactions, errorCodePointer);
    setErrorCode(jEnv, error, errorCode);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFICompletedTxs_jniGetAt(
        JNIEnv *jEnv,
        jobject jThis,
        jint index,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    auto pCompletedTransactions = GetPointerField<TariCompletedTransactions *>(jEnv, jThis);
    auto result = reinterpret_cast<jlong>(completed_transactions_get_at(pCompletedTransactions,
                                                                        static_cast<unsigned int>(index),
                                                                        errorCodePointer));
    setErrorCode(jEnv, error, errorCode);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFICompletedTxs_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    completed_transactions_destroy(GetPointerField<TariCompletedTransactions *>(jEnv, jThis));
    SetNullPointerField(jEnv, jThis);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_FFIPendingInboundTxs_jniGetLength(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    auto pInboundTxs = GetPointerField<TariPendingInboundTransactions *>(jEnv, jThis);
    jint result = pending_inbound_transactions_get_length(pInboundTxs, errorCodePointer);
    setErrorCode(jEnv, error, errorCode);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIPendingInboundTxs_jniGetAt(
        JNIEnv *jEnv,
        jobject jThis,
        jint index,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    auto pInboundTxs = GetPointerField<TariPendingInboundTransactions *>(jEnv, jThis);
    auto result = reinterpret_cast<jlong>(
            pending_inbound_transactions_get_at(
                    pInboundTxs,
                    static_cast<unsigned int>(index),
                    errorCodePointer
            )
    );
    setErrorCode(jEnv, error, errorCode);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIPendingInboundTxs_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    pending_inbound_transactions_destroy(GetPointerField<TariPendingInboundTransactions *>(jEnv, jThis));
    SetNullPointerField(jEnv, jThis);
}


extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_FFIPendingOutboundTxs_jniGetLength(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    auto pOutboundTxs = GetPointerField<TariPendingOutboundTransactions *>(jEnv, jThis);
    jint result = pending_outbound_transactions_get_length(pOutboundTxs, errorCodePointer);
    setErrorCode(jEnv, error, errorCode);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIPendingOutboundTxs_jniGetAt(
        JNIEnv *jEnv,
        jobject jThis,
        jint index,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    auto pOutboundTxs = GetPointerField<TariPendingOutboundTransactions *>(jEnv, jThis);
    auto result = reinterpret_cast<jlong>(
            pending_outbound_transactions_get_at(
                    pOutboundTxs,
                    static_cast<unsigned int>(index),
                    errorCodePointer
            )
    );
    setErrorCode(jEnv, error, errorCode);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIPendingOutboundTxs_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    pending_outbound_transactions_destroy(GetPointerField<TariPendingOutboundTransactions *>(jEnv, jThis));
    SetNullPointerField(jEnv, jThis);
}