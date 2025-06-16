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
    return ExecuteWithError<jint>(jEnv, error, [&](int *errorPointer) {
        auto pContacts = GetPointerField<TariContacts *>(jEnv, jThis);
        return contacts_get_length(pContacts, errorPointer);
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIContacts_jniGetAt(
        JNIEnv *jEnv,
        jobject jThis,
        jint index,
        jobject error) {
    return ExecuteWithErrorAndCast<TariContact *>(jEnv, error, [&](int *errorPointer) -> TariContact * {
        auto pContacts = GetPointerField<TariContacts *>(jEnv, jThis);
        return contacts_get_at(pContacts, static_cast<unsigned int>(index), errorPointer);
    });
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
    return ExecuteWithError<jint>(jEnv, error, [&](int *errorPointer) {
        auto pCompletedTransactions = GetPointerField<TariCompletedTransactions *>(jEnv, jThis);
        return completed_transactions_get_length(pCompletedTransactions, errorPointer);
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFICompletedTxs_jniGetAt(
        JNIEnv *jEnv,
        jobject jThis,
        jint index,
        jobject error) {
    return ExecuteWithErrorAndCast<TariCompletedTransaction *>(jEnv, error, [&](int *errorPointer) {
        auto pCompletedTransactions = GetPointerField<TariCompletedTransactions *>(jEnv, jThis);
        return completed_transactions_get_at(pCompletedTransactions, static_cast<unsigned int>(index), errorPointer);
    });
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
    return ExecuteWithError<jint>(jEnv, error, [&](int *errorPointer) {
        auto pInboundTxs = GetPointerField<TariPendingInboundTransactions *>(jEnv, jThis);
        return pending_inbound_transactions_get_length(pInboundTxs, errorPointer);
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIPendingInboundTxs_jniGetAt(
        JNIEnv *jEnv,
        jobject jThis,
        jint index,
        jobject error) {
    return ExecuteWithErrorAndCast<TariPendingInboundTransaction *>(jEnv, error, [&](int *errorPointer) {
        auto pInboundTxs = GetPointerField<TariPendingInboundTransactions *>(jEnv, jThis);
        return pending_inbound_transactions_get_at(pInboundTxs, static_cast<unsigned int>(index), errorPointer);
    });
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
    return ExecuteWithError<jint>(jEnv, error, [&](int *errorPointer) {
        auto pOutboundTxs = GetPointerField<TariPendingOutboundTransactions *>(jEnv, jThis);
        return pending_outbound_transactions_get_length(pOutboundTxs, errorPointer);
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIPendingOutboundTxs_jniGetAt(
        JNIEnv *jEnv,
        jobject jThis,
        jint index,
        jobject error) {
    return ExecuteWithErrorAndCast<TariPendingOutboundTransaction *>(jEnv, error, [&](int *errorPointer) {
        auto pOutboundTxs = GetPointerField<TariPendingOutboundTransactions *>(jEnv, jThis);
        return pending_outbound_transactions_get_at(pOutboundTxs, static_cast<unsigned int>(index), errorPointer);
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIPendingOutboundTxs_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    pending_outbound_transactions_destroy(GetPointerField<TariPendingOutboundTransactions *>(jEnv, jThis));
    SetNullPointerField(jEnv, jThis);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_FFITariUnblindedOutputs_jniGetLength(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithError<jint>(jEnv, error, [&](int *errorPointer) {
        auto pOutboundTxs = GetPointerField<TariUnblindedOutputs *>(jEnv, jThis);
        return unblinded_outputs_get_length(pOutboundTxs, errorPointer);
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFITariUnblindedOutputs_jniGetAt(
        JNIEnv *jEnv,
        jobject jThis,
        jint index,
        jobject error) {
    return ExecuteWithErrorAndCast<TariUnblindedOutput *>(jEnv, error, [&](int *errorPointer) {
        auto pOutboundTxs = GetPointerField<TariUnblindedOutputs *>(jEnv, jThis);
        return unblinded_outputs_get_at(pOutboundTxs, static_cast<unsigned int>(index), errorPointer);
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFITariUnblindedOutputs_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    unblinded_outputs_destroy(GetPointerField<TariUnblindedOutputs *>(jEnv, jThis));
    SetNullPointerField(jEnv, jThis);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_FFITariPaymentRecords_jniGetLength(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithError<jint>(jEnv, error, [&](int *errorPointer) {
        auto pPaymentRecords = GetPointerField<TariPaymentRecords *>(jEnv, jThis);
        return payment_records_get_length(pPaymentRecords, errorPointer);
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFITariPaymentRecords_jniGetAt(
        JNIEnv *jEnv,
        jobject jThis,
        jint index,
        jobject error) {
    return ExecuteWithErrorAndCast<TariPaymentRecord *>(jEnv, error, [&](int *errorPointer) {
        auto pPaymentRecords = GetPointerField<TariPaymentRecords *>(jEnv, jThis);
        return payment_records_get_at(pPaymentRecords, static_cast<unsigned int>(index), errorPointer);
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFITariPaymentRecords_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    payment_records_destroy(GetPointerField<TariPaymentRecords *>(jEnv, jThis));
    SetNullPointerField(jEnv, jThis);
}