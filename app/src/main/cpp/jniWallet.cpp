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

#define LOG_TAG "Tari Wallet"

/**
 * Log functions. Log example:
 *
 * int count = 5;
 * LOGE("Count is %d", count);
 * char[] name = "asd";
 * LOGI("Name is %s", name);
 */
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,    LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,     LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,     LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,    LOG_TAG, __VA_ARGS__)


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
jmethodID directSendResultCallbackMethodId;
jmethodID storeAndForwardSendResultCallbackMethodId;
jmethodID txCancellationCallbackMethodId;
jmethodID syncBaseNodeId;
jmethodID storeAndForwardMessagesReceivedCallbackMethodId;

void BroadcastCallback(struct TariCompletedTransaction *pCompletedTransaction) {
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

void MinedCallback(struct TariCompletedTransaction *pCompletedTransaction) {
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

void ReceivedCallback(struct TariPendingInboundTransaction *pPendingInboundTransaction) {
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

void ReplyCallback(struct TariCompletedTransaction *pCompletedTransaction) {
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

void FinalizedCallback(struct TariCompletedTransaction *pCompletedTransaction) {
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

void DirectSendResultCallback(unsigned long long tx_id, bool success) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    jbyteArray bytes = getBytesFromUnsignedLongLong(jniEnv, tx_id);
    jniEnv->CallVoidMethod(
            callbackHandler,
            directSendResultCallbackMethodId,
            bytes,
            success);
    g_vm->DetachCurrentThread();
}

void StoreAndForwardSendResultCallback(unsigned long long tx_id, bool success) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    jbyteArray bytes = getBytesFromUnsignedLongLong(jniEnv, tx_id);
    jniEnv->CallVoidMethod(
            callbackHandler,
            storeAndForwardSendResultCallbackMethodId,
            bytes,
            success);
    g_vm->DetachCurrentThread();
}

void TxCancellationCallback(struct TariCompletedTransaction *pCompletedTransaction) {
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

void BaseNodeSyncCallback(unsigned long long request_id, bool success) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    jbyteArray bytes = getBytesFromUnsignedLongLong(jniEnv, request_id);

    jniEnv->CallVoidMethod(
            callbackHandler,
            syncBaseNodeId, bytes, success);
    g_vm->DetachCurrentThread();
}

void StoreAndForwardMessagesReceivedCallback() {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    jniEnv->CallVoidMethod(
            callbackHandler,
            storeAndForwardMessagesReceivedCallbackMethodId);
    g_vm->DetachCurrentThread();
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
        jstring callback_received_tx,
        jstring callback_received_tx_sig,
        jstring callback_received_tx_reply,
        jstring callback_received_tx_reply_sig,
        jstring callback_received_finalized_tx,
        jstring callback_received_finalized_tx_sig,
        jstring callback_tx_broadcast,
        jstring callback_tx_broadcast_sig,
        jstring callback_tx_mined,
        jstring callback_tx_mined_sig,
        jstring callback_direct_send_result,
        jstring callback_direct_send_result_sig,
        jstring callback_store_and_forward_send_result,
        jstring callback_store_and_forward_send_result_sig,
        jstring callback_tx_cancellation,
        jstring callback_tx_cancellation_sig,
        jstring callback_base_node_sync,
        jstring callback_base_node_sync_sig,
        jstring callback_store_and_forward_messages_received,
        jstring callback_store_and_forward_messages_received_sig,
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

    const char *pReceivedMethod = jEnv->GetStringUTFChars(callback_received_tx, JNI_FALSE);
    const char *pReceivedSig = jEnv->GetStringUTFChars(callback_received_tx_sig, JNI_FALSE);
    txReceivedCallbackMethodId = jEnv->GetMethodID(jClass, pReceivedMethod, pReceivedSig);
    jEnv->ReleaseStringUTFChars(callback_received_tx_sig, pReceivedSig);
    jEnv->ReleaseStringUTFChars(callback_received_tx, pReceivedMethod);
    if (txReceivedCallbackMethodId == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    const char *pReceivedReplyMethod = jEnv->GetStringUTFChars(callback_received_tx_reply,
                                                               JNI_FALSE);
    const char *pReceivedReplySig = jEnv->GetStringUTFChars(callback_received_tx_reply_sig,
                                                            JNI_FALSE);
    txReplyReceivedCallbackMethodId = jEnv->GetMethodID(jClass, pReceivedReplyMethod,
                                                        pReceivedReplySig);
    jEnv->ReleaseStringUTFChars(callback_received_tx_reply_sig, pReceivedReplySig);
    jEnv->ReleaseStringUTFChars(callback_received_tx_reply, pReceivedReplyMethod);
    if (txReplyReceivedCallbackMethodId == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    const char *pFinalizedMethod = jEnv->GetStringUTFChars(callback_received_finalized_tx,
                                                           JNI_FALSE);
    const char *pFinalizedSig = jEnv->GetStringUTFChars(callback_received_finalized_tx_sig,
                                                        JNI_FALSE);
    txFinalizedCallbackMethodId = jEnv->GetMethodID(jClass, pFinalizedMethod, pFinalizedSig);
    jEnv->ReleaseStringUTFChars(callback_received_finalized_tx_sig, pFinalizedSig);
    jEnv->ReleaseStringUTFChars(callback_received_finalized_tx, pFinalizedMethod);
    if (txFinalizedCallbackMethodId == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    const char *pBroadcastMethod = jEnv->GetStringUTFChars(callback_tx_broadcast,
                                                           JNI_FALSE);
    const char *pBroadcastSig = jEnv->GetStringUTFChars(callback_tx_broadcast_sig,
                                                        JNI_FALSE);
    txBroadcastCallbackMethodId = jEnv->GetMethodID(jClass, pBroadcastMethod, pBroadcastSig);
    jEnv->ReleaseStringUTFChars(callback_tx_broadcast_sig, pBroadcastSig);
    jEnv->ReleaseStringUTFChars(callback_tx_broadcast, pBroadcastMethod);
    if (txBroadcastCallbackMethodId == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    const char *pMinedMethod = jEnv->GetStringUTFChars(callback_tx_mined, JNI_FALSE);
    const char *pMinedSig = jEnv->GetStringUTFChars(callback_tx_mined_sig, JNI_FALSE);
    txMinedCallbackMethodId = jEnv->GetMethodID(jClass, pMinedMethod, pMinedSig);
    jEnv->ReleaseStringUTFChars(callback_tx_mined_sig, pMinedSig);
    jEnv->ReleaseStringUTFChars(callback_tx_mined, pMinedMethod);
    if (txMinedCallbackMethodId == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    // keep direct send result callback reference
    const char *pDirectSendResultCallbackMethod = jEnv->GetStringUTFChars(
            callback_direct_send_result,
            JNI_FALSE);
    const char *pDirectSendResultCallbackMethodSignature = jEnv->GetStringUTFChars(
            callback_direct_send_result_sig,
            JNI_FALSE);
    directSendResultCallbackMethodId = jEnv->GetMethodID(
            jClass,
            pDirectSendResultCallbackMethod,
            pDirectSendResultCallbackMethodSignature);
    jEnv->ReleaseStringUTFChars(
            callback_direct_send_result,
            pDirectSendResultCallbackMethod);
    jEnv->ReleaseStringUTFChars(
            callback_direct_send_result_sig,
            pDirectSendResultCallbackMethodSignature);
    if (directSendResultCallbackMethodId == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    // keep store & forward send result callback
    const char *pStoreAndForwardSendResultCallbackMethod = jEnv->GetStringUTFChars(
            callback_store_and_forward_send_result,
            JNI_FALSE);
    const char *pStoreAndForwardSendResultCallbackMethodSignature = jEnv->GetStringUTFChars(
            callback_store_and_forward_send_result_sig,
            JNI_FALSE);
    storeAndForwardSendResultCallbackMethodId = jEnv->GetMethodID(
            jClass,
            pStoreAndForwardSendResultCallbackMethod,
            pStoreAndForwardSendResultCallbackMethodSignature);
    jEnv->ReleaseStringUTFChars(
            callback_store_and_forward_send_result,
            pStoreAndForwardSendResultCallbackMethod);
    jEnv->ReleaseStringUTFChars(
            callback_store_and_forward_send_result_sig,
            pStoreAndForwardSendResultCallbackMethodSignature);
    if (storeAndForwardSendResultCallbackMethodId == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    // keep transaction cancellation callback
    const char *pTxCancellationCallbackMethod = jEnv->GetStringUTFChars(
            callback_tx_cancellation,
            JNI_FALSE);
    const char *pTxCancellationCallbackMethodSignature = jEnv->GetStringUTFChars(
            callback_tx_cancellation_sig,
            JNI_FALSE);
    txCancellationCallbackMethodId = jEnv->GetMethodID(
            jClass,
            pTxCancellationCallbackMethod,
            pTxCancellationCallbackMethodSignature);
    jEnv->ReleaseStringUTFChars(
            callback_tx_cancellation,
            pTxCancellationCallbackMethod);
    jEnv->ReleaseStringUTFChars(
            callback_tx_cancellation_sig,
            pTxCancellationCallbackMethodSignature);
    if (txCancellationCallbackMethodId == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    const char *pBaseNodeSync = jEnv->GetStringUTFChars(callback_base_node_sync,
                                                        JNI_FALSE);
    const char *pBaseNodeSyncSig = jEnv->GetStringUTFChars(callback_base_node_sync_sig,
                                                           JNI_FALSE);
    syncBaseNodeId = jEnv->GetMethodID(jClass, pBaseNodeSync, pBaseNodeSyncSig);
    jEnv->ReleaseStringUTFChars(callback_base_node_sync_sig, pBaseNodeSyncSig);
    jEnv->ReleaseStringUTFChars(callback_base_node_sync, pBaseNodeSync);
    if (syncBaseNodeId == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    const char *pStoreAndForwardMessagesReceivedMethod = jEnv->GetStringUTFChars(
            callback_store_and_forward_messages_received,
            JNI_FALSE);
    const char *pStoreAndForwardMessagesReceivedMethodSig = jEnv->GetStringUTFChars(
            callback_store_and_forward_messages_received_sig,
            JNI_FALSE);
    storeAndForwardMessagesReceivedCallbackMethodId = jEnv->GetMethodID(
            jClass,
            pStoreAndForwardMessagesReceivedMethod,
            pStoreAndForwardMessagesReceivedMethodSig);
    jEnv->ReleaseStringUTFChars(
            callback_base_node_sync,
            pStoreAndForwardMessagesReceivedMethod);
    jEnv->ReleaseStringUTFChars(
            callback_base_node_sync_sig,
            pStoreAndForwardMessagesReceivedMethodSig);
    if (storeAndForwardSendResultCallbackMethodId == nullptr) {
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
    }

    jlong lWalletConfig = GetPointerField(jEnv, jpWalletConfig);
    auto *pWalletConfig = reinterpret_cast<TariWalletConfig *>(lWalletConfig);

    // TODO investigate this
    const char *pLogPath = jEnv->GetStringUTFChars(jLogPath, JNI_FALSE);

    TariWallet *pWallet;
    if (strlen(pLogPath) == 0) {
        pWallet = wallet_create(
                pWalletConfig,
                nullptr,
                static_cast<unsigned int>(maxNumberOfRollingLogFiles),
                static_cast<unsigned int>(rollingLogFileMaxSizeBytes),
                nullptr,
                ReceivedCallback,
                ReplyCallback,
                FinalizedCallback,
                BroadcastCallback,
                MinedCallback,
                DirectSendResultCallback,
                StoreAndForwardSendResultCallback,
                TxCancellationCallback,
                BaseNodeSyncCallback,
                StoreAndForwardMessagesReceivedCallback,
                r);
    } else {
        pWallet = wallet_create(
                pWalletConfig,
                pLogPath,
                static_cast<unsigned int>(maxNumberOfRollingLogFiles),
                static_cast<unsigned int>(rollingLogFileMaxSizeBytes),
                nullptr,
                ReceivedCallback,
                ReplyCallback,
                FinalizedCallback,
                BroadcastCallback,
                MinedCallback,
                DirectSendResultCallback,
                StoreAndForwardSendResultCallback,
                TxCancellationCallback,
                BaseNodeSyncCallback,
                StoreAndForwardMessagesReceivedCallback,
                r);
    }
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
    LOGE("FREE WALLET");
    jlong lWallet = GetPointerField(jEnv, jThis);
    jEnv->DeleteGlobalRef(callbackHandler);
    callbackHandler = nullptr;
    wallet_destroy(reinterpret_cast<TariWallet *>(lWallet));
    SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
}

//endregion

//region Wallet Test Functions
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

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniSendTx(
        JNIEnv *jEnv,
        jobject jThis,
        jobject jdestination,
        jstring jamount,
        jstring jfee,
        jstring jmessage,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jlong lDestination = GetPointerField(jEnv, jdestination);
    auto *pDestination = reinterpret_cast<TariPublicKey *>(lDestination);
    const char *nativeAmount = jEnv->GetStringUTFChars(jamount, JNI_FALSE);
    const char *nativeFee = jEnv->GetStringUTFChars(jfee, JNI_FALSE);
    const char *pMessage = jEnv->GetStringUTFChars(jmessage, JNI_FALSE);
    char *pAmountEnd;
    char *pFeeEnd;
    unsigned long long fee = strtoull(nativeFee, &pFeeEnd, 10);
    unsigned long long amount = strtoull(nativeAmount, &pAmountEnd, 10);

    jbyteArray result = getBytesFromUnsignedLongLong(
            jEnv,
            wallet_send_transaction(pWallet, pDestination, amount, fee, pMessage, r));
    setErrorCode(jEnv, error, i);
    jEnv->ReleaseStringUTFChars(jamount, nativeAmount);
    jEnv->ReleaseStringUTFChars(jfee, nativeFee);
    jEnv->ReleaseStringUTFChars(jmessage, pMessage);
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
Java_com_tari_android_wallet_ffi_FFIWallet_jniSyncWithBaseNode(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jbyteArray result = getBytesFromUnsignedLongLong(
            jEnv,
            wallet_sync_with_base_node(pWallet, r)
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
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetTorIdentity(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv, jThis);
    auto *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    ByteVector *pTorPrivateKey = wallet_get_tor_identity(pWallet, r);
    setErrorCode(jEnv, error, i);
    return reinterpret_cast<jlong>(pTorPrivateKey);
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

//endregion
