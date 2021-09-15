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

/**
 * Java virtual machine pointer for later use in callbacks.
 */
JavaVM *g_vm;

/**
 * Called by the environment on JNI load.
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *) {
    g_vm = vm;
    return JNI_VERSION_1_6;
}

/**
 * Helper method to get JNI environment attached to the current thread.
 * Used in callback functions.
 */
JNIEnv *getJNIEnv() {
    JNIEnv *jniEnv;
    JNIEnv *result = nullptr;
    int getEnvStat = g_vm->GetEnv((void **) &jniEnv, JNI_VERSION_1_6);
    switch (getEnvStat) {
        case JNI_EDETACHED: {
            if (g_vm->AttachCurrentThread(&jniEnv, nullptr) != 0) {
                LOGE("VM failed to attach.");
            } else {
                result = jniEnv;
            }
            break;
        }
        case JNI_EVERSION: {
            LOGE("GetEnv: JNI version not supported.");
            break;
        }
        default:
            result = jniEnv;
    }
    return result;
}

// region Wallet
// Wallet is a singleton so only one of each is needed, should wallet be a class these would
// have to be arrays with some means to track which wallet maps to which functions
jobject callbackHandler = nullptr;
jmethodID txReceivedCallbackMethodId;
jmethodID txReplyReceivedCallbackMethodId;
jmethodID txFinalizedCallbackMethodId;
jmethodID txBroadcastCallbackMethodId;
jmethodID txMinedCallbackMethodId;
jmethodID txMinedUnconfirmedCallbackMethodId;
jmethodID directSendResultCallbackMethodId;
jmethodID storeAndForwardSendResultCallbackMethodId;
jmethodID txCancellationCallbackMethodId;
jmethodID utxoValidationCompleteCallbackMethodId;
jmethodID stxoValidationCompleteCallbackMethodId;
jmethodID invalidTxoValidationCompleteCallbackMethodId;
jmethodID transactionValidationCompleteCallbackMethodId;
jmethodID recoveringProcessCompleteCallbackMethodId;

void txBroadcastCallback(struct TariCompletedTransaction *pCompletedTransaction) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    auto jpCompletedTransaction = reinterpret_cast<jlong>(pCompletedTransaction);
    jniEnv->CallVoidMethod(
            callbackHandler,
            txBroadcastCallbackMethodId,
            jpCompletedTransaction);
    g_vm->DetachCurrentThread();
}

void txMinedCallback(struct TariCompletedTransaction *pCompletedTransaction) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    auto jpCompletedTransaction = reinterpret_cast<jlong>(pCompletedTransaction);
    jniEnv->CallVoidMethod(
            callbackHandler,
            txMinedCallbackMethodId,
            jpCompletedTransaction);
    g_vm->DetachCurrentThread();
}

void txMinedUnconfirmedCallback(struct TariCompletedTransaction *pCompletedTransaction,
                                unsigned long long confirmationCount) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    jbyteArray bytes = getBytesFromUnsignedLongLong(jniEnv, confirmationCount);
    auto jpCompletedTransaction = reinterpret_cast<jlong>(pCompletedTransaction);
    jniEnv->CallVoidMethod(
            callbackHandler,
            txMinedUnconfirmedCallbackMethodId,
            jpCompletedTransaction,
            bytes);
    g_vm->DetachCurrentThread();
}

void txReceivedCallback(struct TariPendingInboundTransaction *pPendingInboundTransaction) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    auto jpPendingInboundTransaction = reinterpret_cast<jlong>(pPendingInboundTransaction);
    jniEnv->CallVoidMethod(
            callbackHandler,
            txReceivedCallbackMethodId,
            jpPendingInboundTransaction);
    g_vm->DetachCurrentThread();
}

void txReplyReceivedCallback(struct TariCompletedTransaction *pCompletedTransaction) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    auto jpCompletedTransaction = reinterpret_cast<jlong>(pCompletedTransaction);
    jniEnv->CallVoidMethod(
            callbackHandler,
            txReplyReceivedCallbackMethodId,
            jpCompletedTransaction);
    g_vm->DetachCurrentThread();
}

void txFinalizedCallback(struct TariCompletedTransaction *pCompletedTransaction) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    auto jpCompletedTransaction = reinterpret_cast<jlong>(pCompletedTransaction);
    jniEnv->CallVoidMethod(
            callbackHandler,
            txFinalizedCallbackMethodId,
            jpCompletedTransaction);
    g_vm->DetachCurrentThread();
}

void txDirectSendResultCallback(unsigned long long txId, bool success) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    jbyteArray bytes = getBytesFromUnsignedLongLong(jniEnv, txId);
    jniEnv->CallVoidMethod(
            callbackHandler,
            directSendResultCallbackMethodId,
            bytes,
            success);
    g_vm->DetachCurrentThread();
}

void txStoreAndForwardSendResultCallback(unsigned long long txId, bool success) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    jbyteArray bytes = getBytesFromUnsignedLongLong(jniEnv, txId);
    jniEnv->CallVoidMethod(
            callbackHandler,
            storeAndForwardSendResultCallbackMethodId,
            bytes,
            success);
    g_vm->DetachCurrentThread();
}

void txCancellationCallback(struct TariCompletedTransaction *pCompletedTransaction) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    auto jpCompletedTransaction = reinterpret_cast<jlong>(pCompletedTransaction);
    jniEnv->CallVoidMethod(
            callbackHandler,
            txCancellationCallbackMethodId,
            jpCompletedTransaction);
    g_vm->DetachCurrentThread();
}

void utxoValidationCompleteCallback(unsigned long long requestId, unsigned char result) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    jbyteArray requestIdBytes = getBytesFromUnsignedLongLong(jniEnv, requestId);
    jniEnv->CallVoidMethod(
            callbackHandler,
            utxoValidationCompleteCallbackMethodId,
            requestIdBytes,
            static_cast<jint>(result));
    g_vm->DetachCurrentThread();
}

void stxoValidationCompleteCallback(unsigned long long requestId, unsigned char result) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    jbyteArray requestIdBytes = getBytesFromUnsignedLongLong(jniEnv, requestId);
    jniEnv->CallVoidMethod(
            callbackHandler,
            stxoValidationCompleteCallbackMethodId,
            requestIdBytes,
            static_cast<jint>(result));
    g_vm->DetachCurrentThread();
}

void invalidTxoValidationCompleteCallback(unsigned long long requestId, unsigned char result) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    jbyteArray requestIdBytes = getBytesFromUnsignedLongLong(jniEnv, requestId);
    jniEnv->CallVoidMethod(
            callbackHandler,
            invalidTxoValidationCompleteCallbackMethodId,
            requestIdBytes,
            static_cast<jint>(result));
    g_vm->DetachCurrentThread();
}

void transactionValidationCompleteCallback(unsigned long long requestId, unsigned char result) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    jbyteArray requestIdBytes = getBytesFromUnsignedLongLong(jniEnv, requestId);
    jniEnv->CallVoidMethod(
            callbackHandler,
            transactionValidationCompleteCallbackMethodId,
            requestIdBytes,
            static_cast<jint>(result));
    g_vm->DetachCurrentThread();
}

void storeAndForwardMessagesReceivedCallback() {
    // no-op
}

void recoveringProcessCompleteCallback(unsigned char first, unsigned long long second, unsigned long long third) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    jbyteArray bytes2 = getBytesFromUnsignedLongLong(jniEnv, second);
    jbyteArray bytes3 = getBytesFromUnsignedLongLong(jniEnv, third);
    jniEnv->CallVoidMethod(
            callbackHandler,
            recoveringProcessCompleteCallbackMethodId,
            static_cast<jint>(first),
            bytes2,
            bytes3);
    g_vm->DetachCurrentThread();
}

jmethodID getMethodId(JNIEnv *jniEnv, jobject jThis, jstring methodName, jstring methodSignature) {
    jclass jClass = jniEnv->GetObjectClass(jThis);
    const char *method = jniEnv->GetStringUTFChars(methodName, JNI_FALSE);
    const char *signature = jniEnv->GetStringUTFChars(methodSignature, JNI_FALSE);
    jmethodID methodId = jniEnv->GetMethodID(jClass, method, signature);
    jniEnv->ReleaseStringUTFChars(methodSignature, signature);
    jniEnv->ReleaseStringUTFChars(methodName, method);
    return methodId;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniCreate(
        JNIEnv *jEnv,
        jobject jThis,
        jobject jpWalletConfig,
        jstring jLogPath,
        jint maxNumberOfRollingLogFiles,
        jint rollingLogFileMaxSizeBytes,
        jstring jPassphrase,
        jobject jSeed_words,
        jstring txReceivedCallbackMethodName,
        jstring txReceivedCallbackMethodSignature,
        jstring callback_received_tx_reply,
        jstring callback_received_tx_reply_sig,
        jstring callback_received_finalized_tx,
        jstring callback_received_finalized_tx_sig,
        jstring callback_tx_broadcast,
        jstring callback_tx_broadcast_sig,
        jstring callback_tx_mined,
        jstring callback_tx_mined_sig,
        jstring callback_tx_mined_unconfirmed,
        jstring callback_tx_mined_unconfirmed_sig,
        jstring callback_direct_send_result,
        jstring callback_direct_send_result_sig,
        jstring callback_store_and_forward_send_result,
        jstring callback_store_and_forward_send_result_sig,
        jstring callback_tx_cancellation,
        jstring callback_tx_cancellation_sig,
        jstring callback_utxo_validation_complete,
        jstring callback_utxo_validation_complete_sig,
        jstring callback_stxo_validation_complete,
        jstring callback_stxo_validation_complete_sig,
        jstring callback_invalid_txo_validation_complete,
        jstring callback_invalid_txo_validation_complete_sig,
        jstring callback_transaction_validation_complete,
        jstring callback_transaction_validation_complete_sig,
        jobject error) {

    int i = 0;
    int *r = &i;
    if (callbackHandler == nullptr) {
        callbackHandler = jEnv->NewGlobalRef(jThis);
    }
    jclass jClass = jEnv->GetObjectClass(jThis);
    if (jClass == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    txReceivedCallbackMethodId = getMethodId(
            jEnv,
            jThis,
            txReceivedCallbackMethodName,
            txReceivedCallbackMethodSignature);
    if (txReceivedCallbackMethodId == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    txReplyReceivedCallbackMethodId = getMethodId(
            jEnv,
            jThis,
            callback_received_tx_reply,
            callback_received_tx_reply_sig);
    if (txReplyReceivedCallbackMethodId == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    txFinalizedCallbackMethodId = getMethodId(
            jEnv,
            jThis,
            callback_received_finalized_tx,
            callback_received_finalized_tx_sig);
    if (txFinalizedCallbackMethodId == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    txBroadcastCallbackMethodId = getMethodId(
            jEnv,
            jThis,
            callback_tx_broadcast,
            callback_tx_broadcast_sig);
    if (txBroadcastCallbackMethodId == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    txMinedCallbackMethodId = getMethodId(
            jEnv,
            jThis,
            callback_tx_mined,
            callback_tx_mined_sig);
    if (txMinedCallbackMethodId == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    txMinedUnconfirmedCallbackMethodId = getMethodId(
            jEnv,
            jThis,
            callback_tx_mined_unconfirmed,
            callback_tx_mined_unconfirmed_sig);
    if (txMinedUnconfirmedCallbackMethodId == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    directSendResultCallbackMethodId = getMethodId(
            jEnv,
            jThis,
            callback_direct_send_result,
            callback_direct_send_result_sig);
    if (directSendResultCallbackMethodId == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    storeAndForwardSendResultCallbackMethodId = getMethodId(
            jEnv,
            jThis,
            callback_store_and_forward_send_result,
            callback_store_and_forward_send_result_sig);
    if (storeAndForwardSendResultCallbackMethodId == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    txCancellationCallbackMethodId = getMethodId(
            jEnv,
            jThis,
            callback_tx_cancellation,
            callback_tx_cancellation_sig);
    if (txCancellationCallbackMethodId == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    utxoValidationCompleteCallbackMethodId = getMethodId(
            jEnv,
            jThis,
            callback_utxo_validation_complete,
            callback_utxo_validation_complete_sig);
    if (utxoValidationCompleteCallbackMethodId == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    stxoValidationCompleteCallbackMethodId = getMethodId(
            jEnv,
            jThis,
            callback_stxo_validation_complete,
            callback_stxo_validation_complete_sig);
    if (stxoValidationCompleteCallbackMethodId == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    invalidTxoValidationCompleteCallbackMethodId = getMethodId(
            jEnv,
            jThis,
            callback_invalid_txo_validation_complete,
            callback_invalid_txo_validation_complete_sig);
    if (invalidTxoValidationCompleteCallbackMethodId == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    transactionValidationCompleteCallbackMethodId = getMethodId(
            jEnv,
            jThis,
            callback_transaction_validation_complete,
            callback_transaction_validation_complete_sig);
    if (transactionValidationCompleteCallbackMethodId == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    jlong lWalletConfig = GetPointerField(jEnv, jpWalletConfig);
    auto *pWalletConfig = reinterpret_cast<TariCommsConfig *>(lWalletConfig);

    const char *pLogPath = jEnv->GetStringUTFChars(jLogPath, JNI_FALSE);
    if (strlen(pLogPath) == 0) {
        pLogPath = nullptr;
    }

    const char *pPassphrase = nullptr;
    if (jPassphrase != nullptr) {
        pPassphrase = jEnv->GetStringUTFChars(jPassphrase, JNI_FALSE);
    }

    bool recoveryInProgress = false;
    bool *recovery = &recoveryInProgress;

    TariSeedWords *pSeedWords = nullptr;
    if (jSeed_words != nullptr) {
        jlong lSeedWords = GetPointerField(jEnv, jSeed_words);
        pSeedWords = reinterpret_cast<TariSeedWords *>(lSeedWords);
    }

    TariWallet *pWallet = wallet_create(
            pWalletConfig,
            pLogPath,
            static_cast<unsigned int>(maxNumberOfRollingLogFiles),
            static_cast<unsigned int>(rollingLogFileMaxSizeBytes),
            pPassphrase,
            pSeedWords,
            txReceivedCallback,
            txReplyReceivedCallback,
            txFinalizedCallback,
            txBroadcastCallback,
            txMinedCallback,
            txMinedUnconfirmedCallback,
            txDirectSendResultCallback,
            txStoreAndForwardSendResultCallback,
            txCancellationCallback,
            utxoValidationCompleteCallback,
            stxoValidationCompleteCallback,
            invalidTxoValidationCompleteCallback,
            transactionValidationCompleteCallback,
            storeAndForwardMessagesReceivedCallback,
            recovery,
            r);

    setErrorCode(jEnv, error, i);
    jEnv->ReleaseStringUTFChars(jLogPath, pLogPath);
    SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(pWallet));
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniLogMessage(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jMessage) {
    const char *pMessage = jEnv->GetStringUTFChars(jMessage, JNI_FALSE);
    log_debug_message(pMessage);
    jEnv->ReleaseStringUTFChars(jMessage, pMessage);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetPublicKey(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    auto result = reinterpret_cast<jlong>(wallet_get_public_key(pWallet, r));
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetAvailableBalance(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jbyteArray result = getBytesFromUnsignedLongLong(
            jEnv,
            wallet_get_available_balance(pWallet, r)
    );
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetPendingIncomingBalance(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jbyteArray result = getBytesFromUnsignedLongLong(
            jEnv,
            wallet_get_pending_incoming_balance(pWallet, r)
    );
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetPendingOutgoingBalance(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jbyteArray result = getBytesFromUnsignedLongLong(
            jEnv,
            wallet_get_pending_outgoing_balance(pWallet, r)
    );
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetContacts(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    auto result = reinterpret_cast<jlong>(wallet_get_contacts(pWallet, r));
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniAddUpdateContact(
        JNIEnv *jEnv,
        jobject jThis,
        jobject jpContact,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jlong lContact = GetPointerField(jEnv, jpContact);
    auto *pContact = reinterpret_cast<TariContact *>(lContact);
    auto result = static_cast<jboolean>(
            wallet_upsert_contact(pWallet, pContact, r) != 0
    ); //this is indirectly a cast from unsigned char to jboolean
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniRemoveContact(
        JNIEnv *jEnv,
        jobject jThis,
        jobject jpContact,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jlong lContact = GetPointerField(jEnv, jpContact);
    auto *pContact = reinterpret_cast<TariContact *>(lContact);
    auto result = static_cast<jboolean>(wallet_remove_contact(pWallet, pContact, r) != 0);
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetCompletedTxs(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    TariCompletedTransactions *pCompletedTxs = wallet_get_completed_transactions(pWallet, r);
    setErrorCode(jEnv, error, i);
    return reinterpret_cast<jlong>(pCompletedTxs);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetCancelledTxs(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    TariCompletedTransactions *pCanceledTxs = wallet_get_cancelled_transactions(pWallet, r);
    setErrorCode(jEnv, error, i);
    return reinterpret_cast<jlong>(pCanceledTxs);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetCompletedTxById(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jTxId,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    const char *nativeString = jEnv->GetStringUTFChars(jTxId, JNI_FALSE);
    char *pEnd;
    unsigned long long id = strtoull(nativeString, &pEnd, 10);
    auto result = reinterpret_cast<jlong>(wallet_get_completed_transaction_by_id(pWallet, id, r));
    jEnv->ReleaseStringUTFChars(jTxId, nativeString);
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetCancelledTxById(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jTxId,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    const char *nativeString = jEnv->GetStringUTFChars(jTxId, JNI_FALSE);
    char *pEnd;
    unsigned long long id = strtoull(nativeString, &pEnd, 10);
    auto result = reinterpret_cast<jlong>(wallet_get_cancelled_transaction_by_id(pWallet, id, r));
    jEnv->ReleaseStringUTFChars(jTxId, nativeString);
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetPendingOutboundTxs(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    TariPendingOutboundTransactions *pPendingOutboundTransactions =
            wallet_get_pending_outbound_transactions(pWallet, r);
    setErrorCode(jEnv, error, i);
    return reinterpret_cast<jlong>(pPendingOutboundTransactions);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetPendingOutboundTxById(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jTxId,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    const char *nativeString = jEnv->GetStringUTFChars(jTxId, JNI_FALSE);
    char *pEnd;
    unsigned long long id = strtoull(nativeString, &pEnd, 10);
    auto result = reinterpret_cast<jlong>(
            wallet_get_pending_outbound_transaction_by_id(pWallet, id, r)
    );
    jEnv->ReleaseStringUTFChars(jTxId, nativeString);
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetPendingInboundTxs(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    auto result = reinterpret_cast<jlong>(wallet_get_pending_inbound_transactions(pWallet, r));
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetPendingInboundTxById(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jTxId,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    const char *nativeString = jEnv->GetStringUTFChars(jTxId, JNI_FALSE);
    char *pEnd;
    unsigned long long id = strtoull(nativeString, &pEnd, 10);
    auto result = reinterpret_cast<jlong>(
            wallet_get_pending_inbound_transaction_by_id(pWallet, id, r)
    );
    jEnv->ReleaseStringUTFChars(jTxId, nativeString);
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniCancelPendingTx(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jTxId,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    const char *nativeString = jEnv->GetStringUTFChars(jTxId, JNI_FALSE);
    char *pEnd;
    unsigned long long id = strtoull(nativeString, &pEnd, 10);
    auto result = static_cast<jboolean>(wallet_cancel_pending_transaction(pWallet, id, r));
    jEnv->ReleaseStringUTFChars(jTxId, nativeString);
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    jlong lWallet = GetPointerField(jEnv, jThis);
    jEnv->DeleteGlobalRef(callbackHandler);
    callbackHandler = nullptr;
    wallet_destroy(reinterpret_cast<TariWallet *>(lWallet));
    SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
}

//endregion

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniEstimateTxFee(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jamount,
        jstring jgramFee,
        jstring jkernelCount,
        jstring joutputCount,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    const char *nativeAmount = jEnv->GetStringUTFChars(jamount, JNI_FALSE);
    const char *nativeGramFee = jEnv->GetStringUTFChars(jgramFee, JNI_FALSE);
    const char *nativeKernels = jEnv->GetStringUTFChars(jkernelCount, JNI_FALSE);
    const char *nativeOutputs = jEnv->GetStringUTFChars(joutputCount, JNI_FALSE);
    char *pAmountEnd;
    char *pGramFeeEnd;
    char *pKernelsEnd;
    char *pOutputsEnd;

    unsigned long long amount = strtoull(nativeAmount, &pAmountEnd, 10);
    unsigned long long gramFee = strtoull(nativeGramFee, &pGramFeeEnd, 10);
    unsigned long long kernels = strtoull(nativeKernels, &pKernelsEnd, 10);
    unsigned long long outputs = strtoull(nativeOutputs, &pOutputsEnd, 10);

    jbyteArray result = getBytesFromUnsignedLongLong(
            jEnv,
            wallet_get_fee_estimate(pWallet, amount, gramFee, kernels, outputs, r));
    setErrorCode(jEnv, error, i);
    jEnv->ReleaseStringUTFChars(jamount, nativeAmount);
    jEnv->ReleaseStringUTFChars(jgramFee, nativeGramFee);
    jEnv->ReleaseStringUTFChars(jkernelCount, nativeKernels);
    jEnv->ReleaseStringUTFChars(joutputCount, nativeOutputs);
    return result;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniCoinSplit(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jamount,
        jstring jsplitCount,
        jstring jfee,
        jstring jmessage,
        jstring jlockHeight,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    const char *nativeAmount = jEnv->GetStringUTFChars(jamount, JNI_FALSE);
    const char *nativeFee = jEnv->GetStringUTFChars(jfee, JNI_FALSE);
    const char *nativeHeight = jEnv->GetStringUTFChars(jlockHeight, JNI_FALSE);
    const char *nativeCount = jEnv->GetStringUTFChars(jsplitCount, JNI_FALSE);
    const char *pMessage = jEnv->GetStringUTFChars(jmessage, JNI_FALSE);
    char *pAmountEnd;
    char *pFeeEnd;
    char *pLockHeightEnd;
    char *pCountEnd;
    unsigned long long fee = strtoull(nativeFee, &pFeeEnd, 10);
    unsigned long long amount = strtoull(nativeAmount, &pAmountEnd, 10);
    unsigned long long height = strtoull(nativeHeight, &pLockHeightEnd, 10);
    unsigned long long count = strtoull(nativeCount, &pCountEnd, 10);
    jbyteArray result = getBytesFromUnsignedLongLong(
            jEnv,
            wallet_coin_split(pWallet, amount, count, fee, pMessage, height, r));
    setErrorCode(jEnv, error, i);
    jEnv->ReleaseStringUTFChars(jamount, nativeAmount);
    jEnv->ReleaseStringUTFChars(jfee, nativeFee);
    jEnv->ReleaseStringUTFChars(jlockHeight, nativeHeight);
    jEnv->ReleaseStringUTFChars(jsplitCount, nativeCount);
    jEnv->ReleaseStringUTFChars(jmessage, pMessage);
    return result;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniSignMessage(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jmessage,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    const char *pMessage = jEnv->GetStringUTFChars(jmessage, JNI_FALSE);
    char *pSignature = wallet_sign_message(pWallet, pMessage, r);
    setErrorCode(jEnv, error, i);
    jEnv->ReleaseStringUTFChars(jmessage, pMessage);
    jstring result = jEnv->NewStringUTF(pSignature);
    string_destroy(pSignature);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniVerifyMessageSignature(
        JNIEnv *jEnv,
        jobject jThis,
        jobject jpPublicKey,
        jstring jmessage,
        jstring jhexSignatureNonce,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jlong lPublicKey = GetPointerField(jEnv, jpPublicKey);
    auto *pContactPublicKey = reinterpret_cast<TariPublicKey *>(lPublicKey);
    const char *pHexSignatureNonce = jEnv->GetStringUTFChars(jhexSignatureNonce, JNI_FALSE);
    const char *pMessage = jEnv->GetStringUTFChars(jmessage, JNI_FALSE);
    auto result = static_cast<jboolean>(
            wallet_verify_message_signature(
                    pWallet, pContactPublicKey, pHexSignatureNonce, pMessage, r
            ) != 0
    );
    setErrorCode(jEnv, error, i);
    jEnv->ReleaseStringUTFChars(jhexSignatureNonce, pHexSignatureNonce);
    jEnv->ReleaseStringUTFChars(jmessage, pMessage);
    return result;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniImportUTXO(
        JNIEnv *jEnv,
        jobject jThis,
        jobject jpSpendingKey,
        jobject jpSourcePublicKey,
        jstring jAmount,
        jstring jMessage,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jlong lSpendingKey = GetPointerField(jEnv, jpSpendingKey);
    auto *pSpendingKey = reinterpret_cast<TariPrivateKey *>(lSpendingKey);
    jlong lSourcePublicKey = GetPointerField(jEnv, jpSourcePublicKey);
    auto *pSourcePublicKey = reinterpret_cast<TariPublicKey *>(lSourcePublicKey);
    char *pAmountEnd;
    const char *nativeAmount = jEnv->GetStringUTFChars(jAmount, JNI_FALSE);
    const char *pMessage = jEnv->GetStringUTFChars(jMessage, JNI_FALSE);
    unsigned long long amount = strtoull(nativeAmount, &pAmountEnd, 10);
    jbyteArray result = getBytesFromUnsignedLongLong(
            jEnv,
            wallet_import_utxo(
                    pWallet,
                    amount,
                    pSpendingKey,
                    pSourcePublicKey,
                    pMessage,
                    r
            )
    );
    setErrorCode(jEnv, error, i);
    jEnv->ReleaseStringUTFChars(jAmount, nativeAmount);
    jEnv->ReleaseStringUTFChars(jMessage, pMessage);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniAddBaseNodePeer(
        JNIEnv *jEnv,
        jobject jThis,
        jobject jPublicKey,
        jstring jAddress,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jlong lPublicKey = GetPointerField(jEnv, jPublicKey);
    auto *pPublicKey = reinterpret_cast<TariPublicKey *>(lPublicKey);
    char *pAddress = const_cast<char *>(jEnv->GetStringUTFChars(jAddress, JNI_FALSE));
    auto result = static_cast<jboolean>(
            wallet_add_base_node_peer(pWallet, pPublicKey, pAddress, r) != 0
    );
    jEnv->ReleaseStringUTFChars(jAddress, pAddress);
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniStartUTXOValidation(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jbyteArray result = getBytesFromUnsignedLongLong(
            jEnv,
            wallet_start_utxo_validation(pWallet, r)
    );
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniStartSTXOValidation(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jbyteArray result = getBytesFromUnsignedLongLong(
            jEnv,
            wallet_start_stxo_validation(pWallet, r)
    );
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniStartInvalidTXOValidation(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jbyteArray result = getBytesFromUnsignedLongLong(
            jEnv,
            wallet_start_invalid_txo_validation(pWallet, r)
    );
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniStartTxValidation(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jbyteArray result = getBytesFromUnsignedLongLong(
            jEnv,
            wallet_start_transaction_validation(pWallet, r)
    );
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniRestartTxBroadcast(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jbyteArray result = getBytesFromUnsignedLongLong(
            jEnv,
            wallet_restart_transaction_broadcast(pWallet, r)
    );
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniPowerModeNormal(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    wallet_set_normal_power_mode(pWallet, r);
    setErrorCode(jEnv, error, i);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniPowerModeLow(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    wallet_set_low_power_mode(pWallet, r);
    setErrorCode(jEnv, error, i);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetSeedWords(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    TariSeedWords *pSeedwords = wallet_get_seed_words(pWallet, r);
    setErrorCode(jEnv, error, i);
    return reinterpret_cast<jlong>(pSeedwords);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniSetKeyValue(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jKey,
        jstring jValue,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    const char *pKey = jEnv->GetStringUTFChars(jKey, JNI_FALSE);
    const char *pValue = jEnv->GetStringUTFChars(jValue, JNI_FALSE);
    auto result = static_cast<jboolean>(wallet_set_key_value(pWallet, pKey, pValue, r));
    setErrorCode(jEnv, error, i);
    jEnv->ReleaseStringUTFChars(jKey, pKey);
    jEnv->ReleaseStringUTFChars(jValue, pValue);
    return result;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetKeyValue(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jKey,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    const char *pKey = jEnv->GetStringUTFChars(jKey, JNI_FALSE);
    const char *pValue = wallet_get_value(pWallet, pKey, r);
    setErrorCode(jEnv, error, i);
    jEnv->ReleaseStringUTFChars(jKey, pKey);
    jstring result = jEnv->NewStringUTF(pValue);
    string_destroy(const_cast<char *>(pValue));
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniRemoveKeyValue(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jKey,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    const char *pKey = jEnv->GetStringUTFChars(jKey, JNI_FALSE);
    auto result = static_cast<jboolean>(wallet_clear_value(pWallet, pKey, r));
    setErrorCode(jEnv, error, i);
    jEnv->ReleaseStringUTFChars(jKey, pKey);
    return result;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetConfirmations(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jbyteArray result = getBytesFromUnsignedLongLong(
            jEnv,
            wallet_get_num_confirmations_required(pWallet, r)
    );
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniSetConfirmations(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jNumber,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    const char *nativeString = jEnv->GetStringUTFChars(jNumber, JNI_FALSE);
    char *pEnd;
    unsigned long long number = strtoull(nativeString, &pEnd, 10);
    wallet_set_num_confirmations_required(pWallet, number, r);
    jEnv->ReleaseStringUTFChars(jNumber, nativeString);
    setErrorCode(jEnv, error, i);
}

//region Wallet Test Functions
/*
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGenerateTestData(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jDatastorePath,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    const char *pDatastorePath = jEnv->GetStringUTFChars(jDatastorePath, JNI_FALSE);
    auto result = static_cast<jboolean>(
            wallet_test_generate_data(
                    reinterpret_cast<TariWallet *>(lWallet), pDatastorePath, r
            ) != 0
    );
    setErrorCode(jEnv, error, i);
    jEnv->ReleaseStringUTFChars(jDatastorePath, pDatastorePath);
    return result;
}
*/

/*
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniTestBroadcastTx(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jTxID,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    const char *pTxId = jEnv->GetStringUTFChars(jTxID, JNI_FALSE);
    char *pEnd;
    unsigned long long tx = strtoull(pTxId, &pEnd, 10);
    jEnv->ReleaseStringUTFChars(jTxID, pTxId);
    auto result = static_cast<jboolean>(wallet_test_broadcast_transaction(pWallet, tx, r) != 0);
    setErrorCode(jEnv, error, i);
    return result;
}
*/

/*
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniTestFinalizeReceivedTx(
        JNIEnv *jEnv,
        jobject jThis,
        jobject jTx,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jlong lTx = GetPointerField(jEnv, jTx);
    auto *pTx = reinterpret_cast<TariPendingInboundTransaction *>(lTx);
    auto result = static_cast<jboolean>(
            wallet_test_finalize_received_transaction(pWallet, pTx, r) != 0
    );
    setErrorCode(jEnv, error, i);
    return result;
}
*/

/*
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniTestCompleteSentTx(
        JNIEnv *jEnv,
        jobject jThis,
        jobject jTx,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jlong lTx = GetPointerField(jEnv, jTx);
    auto *pTx = reinterpret_cast<TariPendingOutboundTransaction *>(lTx);
    auto result = static_cast<jboolean>(
            wallet_test_complete_sent_transaction(pWallet, pTx, r) != 0);
    setErrorCode(jEnv, error, i);
    return result;
}
*/

/*
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniTestMineTx(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jTxID,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    const char *pTxId = jEnv->GetStringUTFChars(jTxID, JNI_FALSE);
    char *pEnd;
    unsigned long long tx = strtoull(pTxId, &pEnd, 10);
    jEnv->ReleaseStringUTFChars(jTxID, pTxId);
    auto result = static_cast<jboolean>(wallet_test_mine_transaction(pWallet, tx, r) != 0);
    setErrorCode(jEnv, error, i);
    return result;
}
*/

/*
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniTestReceiveTx(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    auto result = static_cast<jboolean>(wallet_test_receive_transaction(pWallet, r) != 0);
    setErrorCode(jEnv, error, i);
    return result;
}
*/

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniSendTx(
        JNIEnv *jEnv,
        jobject jThis,
        jobject jdestination,
        jstring jamount,
        jstring jfeePerGram,
        jstring jmessage,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jlong lDestination = GetPointerField(jEnv, jdestination);
    auto *pDestination = reinterpret_cast<TariPublicKey *>(lDestination);
    const char *nativeAmount = jEnv->GetStringUTFChars(jamount, JNI_FALSE);
    const char *nativeFeePerGram = jEnv->GetStringUTFChars(jfeePerGram, JNI_FALSE);
    const char *pMessage = jEnv->GetStringUTFChars(jmessage, JNI_FALSE);
    char *pAmountEnd;
    char *pFeeEnd;
    unsigned long long feePerGram = strtoull(nativeFeePerGram, &pFeeEnd, 10);
    unsigned long long amount = strtoull(nativeAmount, &pAmountEnd, 10);

    jbyteArray result = getBytesFromUnsignedLongLong(
            jEnv,
            wallet_send_transaction(pWallet, pDestination, amount, feePerGram, pMessage, r));
    setErrorCode(jEnv, error, i);
    jEnv->ReleaseStringUTFChars(jamount, nativeAmount);
    jEnv->ReleaseStringUTFChars(jfeePerGram, nativeFeePerGram);
    jEnv->ReleaseStringUTFChars(jmessage, pMessage);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniApplyEncryption(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jPassphrase,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    const char *pKey = jEnv->GetStringUTFChars(jPassphrase, JNI_FALSE);
    wallet_apply_encryption(pWallet, pKey, r);
    setErrorCode(jEnv, error, i);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniRemoveEncryption(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    wallet_remove_encryption(pWallet, r);
    setErrorCode(jEnv, error, i);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniStartRecovery(
        JNIEnv *jEnv,
        jobject jThis,
        jobject base_node_public_key,
        jstring callback,
        jstring callback_sig,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);

    jlong lbase_node_public_key = GetPointerField(jEnv, base_node_public_key);
    auto *pTariPublicKey = reinterpret_cast<TariPublicKey *>(lbase_node_public_key);

    recoveringProcessCompleteCallbackMethodId = getMethodId(
            jEnv,
            jThis,
            callback,
            callback_sig);
    if (recoveringProcessCompleteCallbackMethodId == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);

    jboolean result = wallet_start_recovery(pWallet, pTariPublicKey, recoveringProcessCompleteCallback, r);
    setErrorCode(jEnv, error, i);
    return result;
}


//endregion