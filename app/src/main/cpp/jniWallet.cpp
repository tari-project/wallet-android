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
            } else
            {
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
jmethodID txDiscoveryCallbackMethodId;

void BroadcastCallback(struct TariCompletedTransaction *pCompletedTransaction) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr) {
        return;
    }
    jlong jpCompletedTransaction = reinterpret_cast<jlong>(pCompletedTransaction);
    jniEnv->CallVoidMethod(
            callbackHandler,
            txBroadcastCallbackMethodId,
            jpCompletedTransaction);
    g_vm->DetachCurrentThread();
}

void MinedCallback(struct TariCompletedTransaction *pCompletedTransaction) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr) {
        return;
    }
    jlong jpCompletedTransaction = reinterpret_cast<jlong>(pCompletedTransaction);
    jniEnv->CallVoidMethod(
            callbackHandler,
            txMinedCallbackMethodId,
            jpCompletedTransaction);
    g_vm->DetachCurrentThread();
}

void ReceivedCallback(struct TariPendingInboundTransaction *pPendingInboundTransaction) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr) {
        return;
    }
    jlong jpPendingInboundTransaction = reinterpret_cast<jlong>(pPendingInboundTransaction);
    jniEnv->CallVoidMethod(
            callbackHandler,
            txReceivedCallbackMethodId,
            jpPendingInboundTransaction);
    g_vm->DetachCurrentThread();
}

void ReplyCallback(struct TariCompletedTransaction *pCompletedTransaction) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr) {
        return;
    }
    jlong jpCompletedTransaction = reinterpret_cast<jlong>(pCompletedTransaction);
    jniEnv->CallVoidMethod(
            callbackHandler,
            txReplyReceivedCallbackMethodId,
            jpCompletedTransaction);
    g_vm->DetachCurrentThread();
}

void FinalizedCallback(struct TariCompletedTransaction *pCompletedTransaction) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr) {
        return;
    }
    jlong jpCompletedTransaction = reinterpret_cast<jlong>(pCompletedTransaction);
    jniEnv->CallVoidMethod(
            callbackHandler,
            txFinalizedCallbackMethodId,
            jpCompletedTransaction);
    g_vm->DetachCurrentThread();
}

void DiscoveryCallback(unsigned long long tx_id, bool success) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr) {
        return;
    }
    jbyteArray bytes = getBytesFromUnsignedLongLong(jniEnv, tx_id);
    jniEnv->CallVoidMethod(
            callbackHandler,
            txDiscoveryCallbackMethodId, bytes, success);
    g_vm->DetachCurrentThread();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniCreate(
        JNIEnv *jEnv,
        jobject jThis,
        jobject jpWalletConfig,
        jstring jLogPath,
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
        jstring callback_discovery_process_complete,
        jstring callback_discovery_process_complete_sig,
        jobject error) {

    int i = 0;
    int *r = &i;
    if (callbackHandler == nullptr) {
        callbackHandler = jEnv->NewGlobalRef(jThis);
    }
    jclass jClass = jEnv->GetObjectClass(jThis);
    if (jClass == nullptr) {
        SetPointerField(jEnv,jThis,reinterpret_cast<jlong>(nullptr));
    }

    const char *pReceivedMethod = jEnv->GetStringUTFChars(callback_received_tx, JNI_FALSE);
    const char *pReceivedSig = jEnv->GetStringUTFChars(callback_received_tx_sig,
                                                       JNI_FALSE);
    txReceivedCallbackMethodId = jEnv->GetMethodID(jClass, pReceivedMethod, pReceivedSig);
    jEnv->ReleaseStringUTFChars(callback_received_tx_sig, pReceivedSig);
    jEnv->ReleaseStringUTFChars(callback_received_tx, pReceivedSig);
    if (txReceivedCallbackMethodId == nullptr) {
        SetPointerField(jEnv,jThis,reinterpret_cast<jlong>(nullptr));
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
        SetPointerField(jEnv,jThis,reinterpret_cast<jlong>(nullptr));
    }

    const char *pFinalizedMethod = jEnv->GetStringUTFChars(callback_received_finalized_tx,
                                                           JNI_FALSE);
    const char *pFinalizedSig = jEnv->GetStringUTFChars(callback_received_finalized_tx_sig,
                                                        JNI_FALSE);
    txFinalizedCallbackMethodId = jEnv->GetMethodID(jClass, pFinalizedMethod, pFinalizedSig);
    jEnv->ReleaseStringUTFChars(callback_received_finalized_tx_sig, pFinalizedSig);
    jEnv->ReleaseStringUTFChars(callback_received_finalized_tx, pFinalizedMethod);
    if (txFinalizedCallbackMethodId == nullptr) {
        SetPointerField(jEnv,jThis,reinterpret_cast<jlong>(nullptr));
    }

    const char *pBroadcastMethod = jEnv->GetStringUTFChars(callback_tx_broadcast,
                                                           JNI_FALSE);
    const char *pBroadcastSig = jEnv->GetStringUTFChars(callback_tx_broadcast_sig,
                                                        JNI_FALSE);
    txBroadcastCallbackMethodId = jEnv->GetMethodID(jClass, pBroadcastMethod, pBroadcastSig);
    jEnv->ReleaseStringUTFChars(callback_tx_broadcast_sig, pBroadcastSig);
    jEnv->ReleaseStringUTFChars(callback_tx_broadcast, pBroadcastMethod);
    if (txBroadcastCallbackMethodId == nullptr) {
        SetPointerField(jEnv,jThis,reinterpret_cast<jlong>(nullptr));
    }

    const char *pMinedMethod = jEnv->GetStringUTFChars(callback_tx_mined, JNI_FALSE);
    const char *pMinedSig = jEnv->GetStringUTFChars(callback_tx_mined_sig, JNI_FALSE);
    txMinedCallbackMethodId = jEnv->GetMethodID(jClass, pMinedMethod, pMinedSig);
    jEnv->ReleaseStringUTFChars(callback_tx_mined_sig, pMinedSig);
    jEnv->ReleaseStringUTFChars(callback_tx_mined, pMinedMethod);
    if (txMinedCallbackMethodId == nullptr) {
        SetPointerField(jEnv,jThis,reinterpret_cast<jlong>(nullptr));
    }

    const char *pDiscoveryMethod = jEnv->GetStringUTFChars(callback_discovery_process_complete,
                                                           JNI_FALSE);
    const char *pDiscoverySig = jEnv->GetStringUTFChars(callback_discovery_process_complete_sig,
                                                        JNI_FALSE);
    txDiscoveryCallbackMethodId = jEnv->GetMethodID(jClass, pDiscoveryMethod, pDiscoverySig);
    jEnv->ReleaseStringUTFChars(callback_received_tx_sig, pDiscoverySig);
    jEnv->ReleaseStringUTFChars(callback_received_tx, pDiscoveryMethod);
    if (txDiscoveryCallbackMethodId == nullptr) {
        SetPointerField(jEnv,jThis,reinterpret_cast<jlong>(nullptr));
    }

    jlong lWalletConfig = GetPointerField(jEnv,jpWalletConfig);
    TariWalletConfig *pWalletConfig = reinterpret_cast<TariWalletConfig *>(lWalletConfig);

    char *pLogPath = const_cast<char *>(jEnv->GetStringUTFChars(jLogPath, JNI_FALSE));

    TariWallet *pWallet = wallet_create(pWalletConfig, pLogPath, ReceivedCallback, ReplyCallback,
                                        FinalizedCallback, BroadcastCallback, MinedCallback,
                                        DiscoveryCallback, r);

    setErrorCode(jEnv, error, i);
    jEnv->ReleaseStringUTFChars(jLogPath, pLogPath);
    SetPointerField(jEnv,jThis,reinterpret_cast<jlong>(pWallet));
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetPublicKey(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jlong result = reinterpret_cast<jlong>(wallet_get_public_key(pWallet, r));
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
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jbyteArray result = getBytesFromUnsignedLongLong(jEnv,
                                                     wallet_get_available_balance(pWallet, r));
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
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jbyteArray result = getBytesFromUnsignedLongLong(jEnv,
                                                     wallet_get_pending_incoming_balance(pWallet,
                                                                                         r));
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
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jbyteArray result = getBytesFromUnsignedLongLong(jEnv,
                                                     wallet_get_pending_outgoing_balance(pWallet,
                                                                                         r));
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
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jlong result = reinterpret_cast<jlong>(wallet_get_contacts(pWallet, r));
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
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jlong lContact  = GetPointerField(jEnv,jpContact);
    TariContact *pContact = reinterpret_cast<TariContact *>(lContact);
    jboolean result = static_cast<jboolean>(wallet_upsert_contact(pWallet, pContact, r) !=
                                            0); //this is indirectly a cast from unsigned char to jboolean
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
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jlong lContact = GetPointerField(jEnv,jpContact);
    TariContact *pContact = reinterpret_cast<TariContact *>(lContact);
    jboolean result = static_cast<jboolean>(wallet_remove_contact(pWallet, pContact, r) != 0);
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniIsCompletedTxOutbound(
        JNIEnv *jEnv,
        jobject jThis,
        jobject jpCompletedTx,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jlong lCompletedTx = GetPointerField(jEnv,jpCompletedTx);
    TariCompletedTransaction *pTransaction = reinterpret_cast<TariCompletedTransaction*>(lCompletedTx);
    jboolean result = static_cast<jboolean>(
            wallet_is_completed_transaction_outbound(pWallet, pTransaction, r) != 0);
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
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    TariCompletedTransactions *pCompletedTxs = wallet_get_completed_transactions(pWallet, r);
    setErrorCode(jEnv, error, i);
    return reinterpret_cast<jlong>(pCompletedTxs);
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
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    const char* nativeString = jEnv->GetStringUTFChars(jTxId,JNI_FALSE);
    char* pEnd;
    unsigned long long id = strtoull(nativeString,&pEnd,10);
    jlong result = reinterpret_cast<jlong>(wallet_get_completed_transaction_by_id(pWallet,
                                                                                  id,
                                                                                  r));
    jEnv->ReleaseStringUTFChars(jTxId,nativeString);
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
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    TariPendingOutboundTransactions *pPendingOutboundTransactions = wallet_get_pending_outbound_transactions(
            pWallet, r);
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
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    const char* nativeString = jEnv->GetStringUTFChars(jTxId,JNI_FALSE);
    char* pEnd;
    unsigned long long id = strtoull(nativeString, &pEnd,10);
    jlong result = reinterpret_cast<jlong>(wallet_get_pending_outbound_transaction_by_id(pWallet,
                                                                                         id,
                                                                                         r));
    jEnv->ReleaseStringUTFChars(jTxId,nativeString);
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
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jlong result = reinterpret_cast<jlong>(wallet_get_pending_inbound_transactions(pWallet, r));
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
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    const char* nativeString = jEnv->GetStringUTFChars(jTxId,JNI_FALSE);
    char* pEnd;
    unsigned long long id = strtoull(nativeString, &pEnd,10);
    jlong result = reinterpret_cast<jlong>(wallet_get_pending_inbound_transaction_by_id(pWallet,
                                                                                        id,
                                                                                        r));
    jEnv->ReleaseStringUTFChars(jTxId,nativeString);
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    jlong lWallet = GetPointerField(jEnv,jThis);
    wallet_destroy(reinterpret_cast<TariWallet *>(lWallet));
    SetPointerField(jEnv,jThis, reinterpret_cast<jlong>(nullptr));
}

//endregion

//region Wallet Test Functions
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFITestWallet_jniGenerateTestData(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jDatastorePath,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv,jThis);
    const char *pDatastorePath = jEnv->GetStringUTFChars(jDatastorePath, JNI_FALSE);
    jboolean result =
            static_cast<jboolean>(wallet_test_generate_data(reinterpret_cast<TariWallet *>(lWallet),
                                                            const_cast<char *>(pDatastorePath),
                                                            r) != 0);
    setErrorCode(jEnv, error, i);
    jEnv->ReleaseStringUTFChars(jDatastorePath, pDatastorePath);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFITestWallet_jniTestBroadcastTx(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jTxID,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    const char* pTxId = jEnv->GetStringUTFChars(jTxID, JNI_FALSE);
    char* pEnd;
    unsigned long long tx = strtoull(pTxId, &pEnd,10);
    jEnv->ReleaseStringUTFChars(jTxID, pTxId);
    jboolean result = static_cast<jboolean>(wallet_test_broadcast_transaction(pWallet, tx, r) != 0);
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFITestWallet_jniTestFinalizeReceivedTx(
        JNIEnv *jEnv,
        jobject jThis,
        jobject jTx,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jlong lTx = GetPointerField(jEnv,jTx);
    TariPendingInboundTransaction *pTx = reinterpret_cast<TariPendingInboundTransaction *>(lTx);
    jboolean result = static_cast<jboolean>(
            wallet_test_finalize_received_transaction(pWallet, pTx, r) != 0);
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFITestWallet_jniTestCompleteSentTx(
        JNIEnv *jEnv,
        jobject jThis,
        jobject jTx,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jlong lTx = GetPointerField(jEnv,jTx);
    TariPendingOutboundTransaction *pTx = reinterpret_cast<TariPendingOutboundTransaction *>(lTx);
    jboolean result = static_cast<jboolean>(
            wallet_test_complete_sent_transaction(pWallet, pTx, r) != 0);
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFITestWallet_jniTestMineTx(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jTxID,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    const char* pTxId = jEnv->GetStringUTFChars(jTxID, JNI_FALSE);
    char* pEnd;
    unsigned long long tx = strtoull(pTxId, &pEnd,10);
    jEnv->ReleaseStringUTFChars(jTxID, pTxId);
    jboolean result = static_cast<jboolean>(wallet_test_mine_transaction(pWallet, tx, r) != 0);
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFITestWallet_jniTestReceiveTx(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jboolean result = static_cast<jboolean>(wallet_test_receive_transaction(pWallet, r) != 0);
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
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
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jlong lDestination = GetPointerField(jEnv,jdestination);
    TariPublicKey *pDestination = reinterpret_cast<TariPublicKey *>(lDestination);
    const char* nativeAmount = jEnv->GetStringUTFChars(jamount, JNI_FALSE);
    const char* nativeFee = jEnv->GetStringUTFChars(jfee, JNI_FALSE);
    const char* pMessage = jEnv->GetStringUTFChars(jmessage, JNI_FALSE);
    char* pAmountEnd;
    char* pFeeEnd;
    unsigned long long fee = strtoull(nativeFee, &pFeeEnd,10);
    unsigned long long amount = strtoull(nativeAmount, &pAmountEnd,10);

    jboolean result = static_cast<jboolean>(
            wallet_send_transaction(pWallet, pDestination, amount, fee, pMessage, r) != 0);
    setErrorCode(jEnv, error, i);
    jEnv->ReleaseStringUTFChars(jamount,nativeAmount);
    jEnv->ReleaseStringUTFChars(jfee,nativeFee);
    jEnv->ReleaseStringUTFChars(jmessage, pMessage);
    return result;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniSignMessage(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jmessage,
        jobject error ) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    const char* pMessage = jEnv->GetStringUTFChars(jmessage, JNI_FALSE);
    char* pSignature = wallet_sign_message(pWallet,pMessage,r);
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
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jlong lPublicKey = GetPointerField(jEnv,jpPublicKey);
    TariPublicKey *pContactPublicKey = reinterpret_cast<TariPublicKey *>(lPublicKey);
    const char* pHexSignatureNonce = jEnv->GetStringUTFChars(jhexSignatureNonce, JNI_FALSE);
    const char* pMessage = jEnv->GetStringUTFChars(jmessage, JNI_FALSE);
    jboolean result = static_cast<jboolean>(
            wallet_verify_message_signature(pWallet, pContactPublicKey, pHexSignatureNonce,
                                            pMessage, r) != 0);
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
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jlong lSpendingKey = GetPointerField(jEnv,jpSpendingKey);
    TariPrivateKey *pSpendingKey = reinterpret_cast<TariPrivateKey *>(lSpendingKey);
    jlong lSourcePublicKey = GetPointerField(jEnv,jpSourcePublicKey);
    TariPublicKey *pSourcePublicKey = reinterpret_cast<TariPublicKey *>(lSourcePublicKey);
    char* pAmountEnd;
    const char* nativeAmount = jEnv->GetStringUTFChars(jAmount, JNI_FALSE);
    const char* pMessage = jEnv->GetStringUTFChars(jMessage, JNI_FALSE);
    unsigned long long amount = strtoull(nativeAmount, &pAmountEnd,10);
    jbyteArray result = getBytesFromUnsignedLongLong(jEnv,
                                                     wallet_import_utxo(pWallet, amount, pSpendingKey, pSourcePublicKey, pMessage, r)
    );
    setErrorCode(jEnv, error, i);
    jEnv->ReleaseStringUTFChars(jAmount,nativeAmount);
    jEnv->ReleaseStringUTFChars(jMessage,pMessage);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniAddBaseNodePeer(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpPublicKey,
        jstring jAddress,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    TariPublicKey *pPublicKey = reinterpret_cast<TariPublicKey *>(jpPublicKey);
    char *pAddress = const_cast<char *>(jEnv->GetStringUTFChars(jAddress, JNI_FALSE));
    jboolean result = static_cast<jboolean>(
            wallet_add_base_node_peer(pWallet, pPublicKey, pAddress, r) != 0);
    jEnv->ReleaseStringUTFChars(jAddress, pAddress);
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniSyncBaseNode(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    jboolean result = static_cast<jboolean>(wallet_sync_with_base_node(pWallet, r) != 0);
    setErrorCode(jEnv, error, i);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetTorPrivateKey(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lWallet = GetPointerField(jEnv,jThis);
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(lWallet);
    ByteVector *pTorPrivateKey = wallet_get_tor_identity(pWallet,r);
    setErrorCode(jEnv, error, i);
    return reinterpret_cast<jlong>(pTorPrivateKey);
}

//endregion