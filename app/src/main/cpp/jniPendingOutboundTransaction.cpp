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
Java_com_tari_android_wallet_ffi_FFIPendingOutboundTx_jniGetId(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithError<jbyteArray>(jEnv, error, [&](int *errorPointer) {
        auto pOutboundTx = GetPointerField<TariPendingOutboundTransaction *>(jEnv, jThis);
        return getBytesFromUnsignedLongLong(jEnv, pending_outbound_transaction_get_transaction_id(pOutboundTx, errorPointer));
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIPendingOutboundTx_jniGetDestinationPublicKey(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithErrorAndCast<TariWalletAddress *>(jEnv, error, [&](int *errorPointer) {
        auto pOutboundTx = GetPointerField<TariPendingOutboundTransaction *>(jEnv, jThis);
        return pending_outbound_transaction_get_destination_tari_address(pOutboundTx, errorPointer);
    });
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIPendingOutboundTx_jniGetAmount(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithError<jbyteArray>(jEnv, error, [&](int *errorPointer) {
        auto pOutboundTx = GetPointerField<TariPendingOutboundTransaction *>(jEnv, jThis);
        return getBytesFromUnsignedLongLong(jEnv, pending_outbound_transaction_get_amount(pOutboundTx, errorPointer));
    });
}


extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIPendingOutboundTx_jniGetFee(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithError<jbyteArray>(jEnv, error, [&](int *errorPointer) {
        auto pOutboundTx = GetPointerField<TariPendingOutboundTransaction *>(jEnv, jThis);
        return getBytesFromUnsignedLongLong(jEnv, pending_outbound_transaction_get_fee(pOutboundTx, errorPointer));
    });
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tari_android_wallet_ffi_FFIPendingOutboundTx_jniGetPaymentId(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithError<jstring>(jEnv, error, [&](int *errorPointer) {
        auto pOutboundTx = GetPointerField<TariPendingOutboundTransaction *>(jEnv, jThis);
        const char *pPaymentId = pending_outbound_transaction_get_payment_id(pOutboundTx, errorPointer);
        jstring result = jEnv->NewStringUTF(pPaymentId);
        string_destroy(const_cast<char *>(pPaymentId));
        return result;
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIPendingOutboundTx_jniGetPaymentIdBytes(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithErrorAndCast<ByteVector *>(jEnv, error, [&](int *errorPointer) {
        auto pOutboundTx = GetPointerField<TariPendingOutboundTransaction *>(jEnv, jThis);
        return pending_outbound_transaction_get_payment_id_as_bytes(pOutboundTx, errorPointer);
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIPendingOutboundTx_jniGetPaymentIdUserBytes(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithErrorAndCast<ByteVector *>(jEnv, error, [&](int *errorPointer) {
        auto pOutboundTx = GetPointerField<TariPendingOutboundTransaction *>(jEnv, jThis);
        return pending_outbound_transaction_get_user_payment_id_as_bytes(pOutboundTx, errorPointer);
    });
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIPendingOutboundTx_jniGetTimestamp(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithError<jbyteArray>(jEnv, error, [&](int *errorPointer) {
        auto pOutboundTx = GetPointerField<TariPendingOutboundTransaction *>(jEnv, jThis);
        return getBytesFromUnsignedLongLong(jEnv, pending_outbound_transaction_get_timestamp(pOutboundTx, errorPointer));
    });
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_FFIPendingOutboundTx_jniGetStatus(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithError<jint>(jEnv, error, [&](int *errorPointer) {
        auto pOutboundTx = GetPointerField<TariPendingOutboundTransaction *>(jEnv, jThis);
        return reinterpret_cast<jint>(pending_outbound_transaction_get_status(pOutboundTx, errorPointer));
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIPendingOutboundTx_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    pending_outbound_transaction_destroy(GetPointerField<TariPendingOutboundTransaction *>(jEnv, jThis));
    SetNullPointerField(jEnv, jThis);
}