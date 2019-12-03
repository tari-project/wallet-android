/**
 * Copyright 2019 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:

 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.

 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.

 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.

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
#include <math.h>
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
            }
            break;
        }
        case JNI_EVERSION: {
            LOGE("GetEnv: JNI version not supported.");
            break;
        }
        default: result = jniEnv;
    }
    return result;
}

// region Wallet

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniCreate(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWalletConfig,
        jstring jLogPath,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariWalletConfig *pWalletConfig = reinterpret_cast<TariWalletConfig *>(jpWalletConfig);
    char *pLogPath = const_cast<char*>(jEnv->GetStringUTFChars(jLogPath, JNI_FALSE));
    TariWallet *pWallet = wallet_create(pWalletConfig, pLogPath,r);
    setErrorCode(jEnv,error,i);
    jEnv->ReleaseStringUTFChars(jLogPath, pLogPath);
    return reinterpret_cast<jlong>(pWallet);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniGetPublicKey(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(jpWallet);
    jlong result = reinterpret_cast<jlong>(wallet_get_public_key(pWallet,r));
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniGetAvailableBalance(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(jpWallet);
    jbyteArray result = getBytesFromUnsignedLongLong(jEnv,wallet_get_available_balance(pWallet,r));
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniGetPendingIncomingBalance(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(jpWallet);
    jbyteArray result = getBytesFromUnsignedLongLong(jEnv,wallet_get_pending_incoming_balance(pWallet,r));
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniGetPendingOutgoingBalance(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(jpWallet);
    jbyteArray result = getBytesFromUnsignedLongLong(jEnv,wallet_get_pending_outgoing_balance(pWallet,r));
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniGetContacts(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(jpWallet);
    jlong result = reinterpret_cast<jlong>(wallet_get_contacts(pWallet,r));
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniAddContact(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jlong jpContact,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(jpWallet);
    TariContact *pContact = reinterpret_cast<TariContact *>(jpContact);
    jboolean result = wallet_add_contact(pWallet, pContact,r)!=0; //this is indirectly a cast from unsigned char to jboolean
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniRemoveContact(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jlong jpContact,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(jpWallet);
    TariContact *pContact = reinterpret_cast<TariContact *>(jpContact);
    jboolean result = wallet_remove_contact(pWallet, pContact,r)!=0;
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniGetCompletedTransactions(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariWallet *pWallet = (TariWallet *) jpWallet;
    TariCompletedTransactions *pCompletedTransactions = wallet_get_completed_transactions(pWallet,r);
    setErrorCode(jEnv,error,i);
    return reinterpret_cast<jlong>(pCompletedTransactions);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniGetCompletedTransactionById(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jlong jTxId,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(jpWallet);
    jlong result = reinterpret_cast<jlong>(wallet_get_completed_transaction_by_id(pWallet,
                                                                                  static_cast<unsigned long long>(jTxId),r));
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniGetPendingOutboundTransactions(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(jpWallet);
    TariPendingOutboundTransactions *pPendingOutboundTransactions = wallet_get_pending_outbound_transactions(pWallet,r);
    setErrorCode(jEnv,error,i);
    return reinterpret_cast<jlong>(pPendingOutboundTransactions);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniGetPendingOutboundTransactionById(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jlong jTxId,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(jpWallet);
    jlong result = reinterpret_cast<jlong>(wallet_get_pending_outbound_transaction_by_id(pWallet,
                                                                                         static_cast<unsigned long long>(jTxId),r));
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniGetPendingInboundTransactions(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(jpWallet);
    jlong result = reinterpret_cast<jlong>(wallet_get_pending_inbound_transactions(pWallet,r));
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniGetPendingInboundTransactionById(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jlong jTxId,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(jpWallet);
    jlong result = reinterpret_cast<jlong>(wallet_get_pending_inbound_transaction_by_id(pWallet,
                                                                                        static_cast<unsigned long long>(jTxId),r));
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet) {
    wallet_destroy(reinterpret_cast<TariWallet *>(jpWallet));
}

//endregion

//region Callback Registration
// Wallet is a singleton so only one of each is needed, should wallet be a class these would
// have to be arrays with some means to track which wallet maps to which functions
jobject callbackHandler = nullptr;
jmethodID txBroadcastCallbackMethodId;
jmethodID txReceivedCallbackMethodId;
jmethodID txMinedCallbackMethodId;
jmethodID txReplyReceivedCallbackMethodId;
jmethodID txFinalizedCallbackMethodId;

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

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniRegisterFinalizedTransaction(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jstring jMethodName,
        jstring jMethodSignature,
        jobject error
) {
    int i = 0;
    int* r = &i;
    if (callbackHandler == nullptr) {
        callbackHandler = jEnv->NewGlobalRef(jThis);
    }
    jclass jClass = jEnv->GetObjectClass(jThis);
    if (jClass == nullptr) {
        return (jboolean) false;
    }
    const char *pMethod = jEnv->GetStringUTFChars(jMethodName, JNI_FALSE);
    const char *pSig = jEnv->GetStringUTFChars(jMethodSignature, JNI_FALSE);
    txFinalizedCallbackMethodId = jEnv->GetMethodID(jClass,pMethod, pSig);
    jEnv->ReleaseStringUTFChars(jMethodSignature, pSig);
    jEnv->ReleaseStringUTFChars(jMethodName, pMethod);
    if (txBroadcastCallbackMethodId == nullptr) {
        return (jboolean) false;
    }
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(jpWallet);
    jboolean result = wallet_callback_register_received_finalized_transaction(pWallet,FinalizedCallback,r)!=0;
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniRegisterTransactionBroadcast(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jstring jMethodName,
        jstring jMethodSignature,
        jobject error
        ) {
    if (callbackHandler == nullptr) {
        callbackHandler = jEnv->NewGlobalRef(jThis);
    }
    jclass jClass = jEnv->GetObjectClass(jThis);
    if (jClass == nullptr) {
        return (jboolean) false;
    }
    const char* pMethod = jEnv->GetStringUTFChars(jMethodName, JNI_FALSE);
    const char* pSig = jEnv->GetStringUTFChars(jMethodSignature, JNI_FALSE);
    txBroadcastCallbackMethodId = jEnv->GetMethodID(jClass, pMethod, pSig);
    jEnv->ReleaseStringUTFChars(jMethodSignature, pSig);
    jEnv->ReleaseStringUTFChars(jMethodName, pMethod);
    if (txBroadcastCallbackMethodId == nullptr) {
        return (jboolean) false;
    }
    int i = 0;
    int* r = &i;
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(jpWallet);
    jboolean result = wallet_callback_register_transaction_broadcast(pWallet,BroadcastCallback,r)!=0;
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniRegisterTransactionMined(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jstring jMethodName,
        jstring jMethodSignature,
        jobject error
        ) {
    if (callbackHandler == nullptr) {
        callbackHandler = jEnv->NewGlobalRef(jThis);
    }
    jclass jClass = jEnv->GetObjectClass(jThis);
    if (jClass == nullptr) {
        return (jboolean) false;
    }
    const char* pMethod = jEnv->GetStringUTFChars(jMethodName, JNI_FALSE);
    const char* pSig = jEnv->GetStringUTFChars(jMethodSignature, JNI_FALSE);
    txMinedCallbackMethodId = jEnv->GetMethodID(jClass, pMethod, pSig);
    jEnv->ReleaseStringUTFChars(jMethodSignature, pSig);
    jEnv->ReleaseStringUTFChars(jMethodName, pMethod);
    if (txMinedCallbackMethodId == nullptr) {
        return (jboolean) false;
    }
    int i = 0;
    int* r = &i;
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(jpWallet);
    jboolean result = wallet_callback_register_mined(pWallet, MinedCallback,r)!=0;
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniRegisterTransactionReceived(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jstring jMethodName,
        jstring jMethodSignature,
        jobject error
        ) {
    if (callbackHandler == nullptr) {
        callbackHandler = jEnv->NewGlobalRef(jThis);
    }
    jclass jClass = jEnv->GetObjectClass(jThis);
    if (jClass == nullptr) {
        return (jboolean) false;
    }
    const char* pMethod = jEnv->GetStringUTFChars(jMethodName, JNI_FALSE);
    const char* pSig = jEnv->GetStringUTFChars(jMethodSignature, JNI_FALSE);
    txReceivedCallbackMethodId = jEnv->GetMethodID(jClass,pMethod, pSig);
    jEnv->ReleaseStringUTFChars(jMethodSignature, pSig);
    jEnv->ReleaseStringUTFChars(jMethodName, pMethod);
    if (txReceivedCallbackMethodId == nullptr) {
        return (jboolean) false;
    }
    int i = 0;
    int* r = &i;
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(jpWallet);
    jboolean result = wallet_callback_register_received_transaction(pWallet, ReceivedCallback,r)!=0;
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniRegisterTransactionReplyReceived(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jstring jMethodName,
        jstring jMethodSignature,
        jobject error) {
    if (callbackHandler == nullptr) {
        callbackHandler = jEnv->NewGlobalRef(jThis);
    }
    jclass jClass = jEnv->GetObjectClass(jThis);
    if (jClass == nullptr) {
        return (jboolean) false;
    }
    const char* pMethod = jEnv->GetStringUTFChars(jMethodName, JNI_FALSE);
    const char* pSig = jEnv->GetStringUTFChars(jMethodSignature, JNI_FALSE);
    txReplyReceivedCallbackMethodId = jEnv->GetMethodID(jClass,pMethod, pSig);
    jEnv->ReleaseStringUTFChars(jMethodSignature, pSig);
    jEnv->ReleaseStringUTFChars(jMethodName, pMethod);
    if (txReplyReceivedCallbackMethodId == nullptr) {
        return (jboolean) false;
    }
    int i = 0;
    int* r = &i;
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(jpWallet);
    jboolean result = wallet_callback_register_received_transaction_reply(pWallet,ReplyCallback,r)!=0;
    setErrorCode(jEnv,error,i);
    return result;
}

//endregion

//region Wallet Test Functions
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniGenerateTestData(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jstring jDatastorePath,
        jobject error) {
    int i = 0;
    int* r = &i;
    const char* pDatastorePath = jEnv->GetStringUTFChars(jDatastorePath, JNI_FALSE);
    jboolean result = wallet_test_generate_data((TariWallet *) jpWallet, const_cast<char *>(pDatastorePath),r) !=0;
    setErrorCode(jEnv,error,i);
    jEnv->ReleaseStringUTFChars(jDatastorePath, pDatastorePath);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniTransactionBroadcast(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jlong jTx,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(jpWallet);
    TariPendingInboundTransaction *pTx = reinterpret_cast<TariPendingInboundTransaction *>(jTx);
    jboolean result = wallet_test_transaction_broadcast(pWallet, pTx,r)!=0;
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniCompleteSentTransaction(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jlong jTx,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(jpWallet);
    TariPendingOutboundTransaction *pTx = reinterpret_cast<TariPendingOutboundTransaction *>(jTx);
    jboolean result = wallet_test_complete_sent_transaction(pWallet, pTx,r)!=0;
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniMineCompletedTransaction(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jlong jTx,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(jpWallet);
    TariCompletedTransaction *pTx = reinterpret_cast<TariCompletedTransaction *>(jTx);
    jboolean result = wallet_test_mined(pWallet, pTx,r)!=0;
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniReceiveTransaction(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(jpWallet);
    jboolean result = wallet_test_receive_transaction(pWallet,r)!=0;
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Wallet_jniSendTransaction(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jlong jdestination,
        jlong jamount,
        jlong jfee,
        jstring jmessage,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariWallet *pWallet = reinterpret_cast<TariWallet *>(jpWallet);
    TariPublicKey *pDestination = reinterpret_cast<TariPublicKey*>(jdestination);
    unsigned long long amount = static_cast<unsigned long long>(jamount);
    unsigned long long fee = static_cast<unsigned long long>(jfee);
    const char* pMessage = jEnv->GetStringUTFChars(jmessage, JNI_FALSE);
    jboolean result = wallet_send_transaction(pWallet,pDestination,amount,fee,pMessage,r) != 0;
    setErrorCode(jEnv,error,i);
    jEnv->ReleaseStringUTFChars(jmessage, pMessage);
    return result;
}

//endregion