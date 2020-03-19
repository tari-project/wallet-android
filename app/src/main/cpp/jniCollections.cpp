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
    int i = 0;
    int *r = &i;
    jlong lContacts = GetPointerField(jEnv,jThis);
    TariContacts *pContacts = reinterpret_cast<TariContacts *>(lContacts);
    jint result = contacts_get_length(pContacts, r);
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIContacts_jniGetAt(
        JNIEnv *jEnv,
        jobject jThis,
        jint index,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lContacts = GetPointerField(jEnv,jThis);
    auto *pContacts = reinterpret_cast<TariContacts *>(lContacts);
    jlong result = reinterpret_cast<jlong>(contacts_get_at(pContacts,
                                                           static_cast<unsigned int>(index), r));
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIContacts_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    jlong lContacts = GetPointerField(jEnv,jThis);
    contacts_destroy(reinterpret_cast<TariContacts *>(lContacts));
    SetPointerField(jEnv,jThis, reinterpret_cast<jlong>(nullptr));
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_FFICompletedTxs_jniGetLength(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lCompletedTransactions = GetPointerField(jEnv,jThis);
    TariCompletedTransactions *pCompletedTransactions = reinterpret_cast<TariCompletedTransactions *>(lCompletedTransactions);
    jint result = completed_transactions_get_length(pCompletedTransactions, r);
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFICompletedTxs_jniGetAt(
        JNIEnv *jEnv,
        jobject jThis,
        jint index,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lCompletedTransactions = GetPointerField(jEnv,jThis);
    auto *pCompletedTransactions = reinterpret_cast<TariCompletedTransactions *>(lCompletedTransactions);
    jlong result = reinterpret_cast<jlong>(completed_transactions_get_at(pCompletedTransactions,
                                                                         static_cast<unsigned int>(index),
                                                                         r));
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFICompletedTxs_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    jlong lCompletedTransactions = GetPointerField(jEnv,jThis);
    completed_transactions_destroy(
            reinterpret_cast<TariCompletedTransactions *>(lCompletedTransactions));
    SetPointerField(jEnv,jThis, reinterpret_cast<jlong>(nullptr));
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_FFIPendingInboundTxs_jniGetLength(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lInboundTransactions = GetPointerField(jEnv,jThis);
    TariPendingInboundTransactions *pInboundTxs = reinterpret_cast<TariPendingInboundTransactions *>(lInboundTransactions);
    jint result = pending_inbound_transactions_get_length(pInboundTxs, r);
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIPendingInboundTxs_jniGetAt(
        JNIEnv *jEnv,
        jobject jThis,
        jint index,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lInboundTransactions = GetPointerField(jEnv,jThis);
    TariPendingInboundTransactions *pInboundTxs = reinterpret_cast<TariPendingInboundTransactions *>(lInboundTransactions);
    jlong result = reinterpret_cast<jlong>(pending_inbound_transactions_get_at(pInboundTxs,
                                                                               static_cast<unsigned int>(index),
                                                                               r));
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIPendingInboundTxs_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    jlong lInboundTransactions = GetPointerField(jEnv,jThis);
    pending_inbound_transactions_destroy(
            reinterpret_cast<TariPendingInboundTransactions *>(lInboundTransactions));
    SetPointerField(jEnv,jThis, reinterpret_cast<jlong>(nullptr));
}


extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_FFIPendingOutboundTxs_jniGetLength(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lOutboundTransactions = GetPointerField(jEnv,jThis);
    TariPendingOutboundTransactions *pOutboundTxs = reinterpret_cast<TariPendingOutboundTransactions *>(lOutboundTransactions);
    jint result = pending_outbound_transactions_get_length(pOutboundTxs, r);
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIPendingOutboundTxs_jniGetAt(
        JNIEnv *jEnv,
        jobject jThis,
        jint index,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lOutboundTransactions = GetPointerField(jEnv,jThis);
    TariPendingOutboundTransactions *pOutboundTxs = reinterpret_cast<TariPendingOutboundTransactions *>(lOutboundTransactions);
    jlong result = reinterpret_cast<jlong>(pending_outbound_transactions_get_at(pOutboundTxs,
                                                                                static_cast<unsigned int>(index),
                                                                                r));
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIPendingOutboundTxs_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    jlong lOutboundTransactions = GetPointerField(jEnv,jThis);
    pending_outbound_transactions_destroy(
            reinterpret_cast<TariPendingOutboundTransactions *>(lOutboundTransactions));
    SetPointerField(jEnv,jThis, reinterpret_cast<jlong>(nullptr));
}