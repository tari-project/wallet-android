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

// Wallet is a singleton so only one of each is needed, should wallet be a class these would
// have to be arrays with some means to track which wallet maps to which functions
jobject callbackHandler = nullptr;
jmethodID txReceivedCallbackMethodId;
jmethodID txReplyReceivedCallbackMethodId;
jmethodID txFinalizedCallbackMethodId;
jmethodID txBroadcastCallbackMethodId;
jmethodID txMinedCallbackMethodId;
jmethodID txMinedUnconfirmedCallbackMethodId;
jmethodID txFauxConfirmedCallbackMethodId;
jmethodID txFauxUnconfirmedCallbackMethodId;
jmethodID directSendResultCallbackMethodId;
jmethodID txCancellationCallbackMethodId;
jmethodID contactsLivenessDataUpdatedCallbackMethodId;
jmethodID connectivityStatusCallbackId;
jmethodID txoValidationCompleteCallbackMethodId;
jmethodID transactionValidationCompleteCallbackMethodId;
jmethodID recoveringProcessCompleteCallbackMethodId;
jmethodID balanceUpdatedCallbackMethodId;

void txBroadcastCallback(TariCompletedTransaction *pCompletedTransaction) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    auto jpCompletedTransaction = reinterpret_cast<jlong>(pCompletedTransaction);
    jniEnv->CallVoidMethod(callbackHandler, txBroadcastCallbackMethodId, jpCompletedTransaction);
    g_vm->DetachCurrentThread();
}

void txMinedCallback(TariCompletedTransaction *pCompletedTransaction) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    auto jpCompletedTransaction = reinterpret_cast<jlong>(pCompletedTransaction);
    jniEnv->CallVoidMethod(callbackHandler, txMinedCallbackMethodId, jpCompletedTransaction);
    g_vm->DetachCurrentThread();
}

void txMinedUnconfirmedCallback(TariCompletedTransaction *pCompletedTransaction, uint64_t confirmationCount) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    jbyteArray bytes = getBytesFromUnsignedLongLong(jniEnv, confirmationCount);
    auto jpCompletedTransaction = reinterpret_cast<jlong>(pCompletedTransaction);
    jniEnv->CallVoidMethod(callbackHandler, txMinedUnconfirmedCallbackMethodId, jpCompletedTransaction, bytes);
    g_vm->DetachCurrentThread();
}

void txFauxConfirmedCallback(TariCompletedTransaction *pCompletedTransaction) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    auto jpCompletedTransaction = reinterpret_cast<jlong>(pCompletedTransaction);
    jniEnv->CallVoidMethod(callbackHandler, txFauxConfirmedCallbackMethodId, jpCompletedTransaction);
    g_vm->DetachCurrentThread();
}

void txFauxUnconfirmedCallback(TariCompletedTransaction *pCompletedTransaction, uint64_t confirmationCount) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    jbyteArray bytes = getBytesFromUnsignedLongLong(jniEnv, confirmationCount);
    auto jpCompletedTransaction = reinterpret_cast<jlong>(pCompletedTransaction);
    jniEnv->CallVoidMethod(callbackHandler, txFauxUnconfirmedCallbackMethodId, jpCompletedTransaction, bytes);
    g_vm->DetachCurrentThread();
}

void txReceivedCallback(TariPendingInboundTransaction *pPendingInboundTransaction) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    auto jpPendingInboundTransaction = reinterpret_cast<jlong>(pPendingInboundTransaction);
    jniEnv->CallVoidMethod(callbackHandler, txReceivedCallbackMethodId, jpPendingInboundTransaction);
    g_vm->DetachCurrentThread();
}

void txReplyReceivedCallback(TariCompletedTransaction *pCompletedTransaction) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    auto jpCompletedTransaction = reinterpret_cast<jlong>(pCompletedTransaction);
    jniEnv->CallVoidMethod(callbackHandler, txReplyReceivedCallbackMethodId, jpCompletedTransaction);
    g_vm->DetachCurrentThread();
}

void txFinalizedCallback(TariCompletedTransaction *pCompletedTransaction) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    auto jpCompletedTransaction = reinterpret_cast<jlong>(pCompletedTransaction);
    jniEnv->CallVoidMethod(callbackHandler, txFinalizedCallbackMethodId, jpCompletedTransaction);
    g_vm->DetachCurrentThread();
}

void txDirectSendResultCallback(unsigned long long txId, TariTransactionSendStatus *status) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    jbyteArray bytes = getBytesFromUnsignedLongLong(jniEnv, txId);
    jniEnv->CallVoidMethod(callbackHandler, directSendResultCallbackMethodId, bytes, status);
    g_vm->DetachCurrentThread();
}

void
txCancellationCallback(TariCompletedTransaction *pCompletedTransaction, uint64_t rejectionReason) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    jbyteArray bytes = getBytesFromUnsignedLongLong(jniEnv, rejectionReason);
    auto jpCompletedTransaction = reinterpret_cast<jlong>(pCompletedTransaction);
    jniEnv->CallVoidMethod(callbackHandler, txCancellationCallbackMethodId, jpCompletedTransaction, bytes);
    g_vm->DetachCurrentThread();
}

void txoValidationCompleteCallback(uint64_t requestId, uint64_t status) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    jbyteArray requestIdBytes = getBytesFromUnsignedLongLong(jniEnv, requestId);
    jbyteArray statusBytes = getBytesFromUnsignedLongLong(jniEnv, status);
    jniEnv->CallVoidMethod(callbackHandler, txoValidationCompleteCallbackMethodId, requestIdBytes, statusBytes);
    g_vm->DetachCurrentThread();
}

void contactsLivenessDataUpdatedCallback(TariContactsLivenessData *pTariContactsLivenessData) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    auto jpTariContactsLivenessData = reinterpret_cast<jlong>(pTariContactsLivenessData);
    jniEnv->CallVoidMethod(callbackHandler, contactsLivenessDataUpdatedCallbackMethodId, jpTariContactsLivenessData);
    g_vm->DetachCurrentThread();
}

void transactionValidationCompleteCallback(uint64_t requestId, uint64_t status) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    jbyteArray requestIdBytes = getBytesFromUnsignedLongLong(jniEnv, requestId);
    jbyteArray statusBytes = getBytesFromUnsignedLongLong(jniEnv, status);
    jniEnv->CallVoidMethod(callbackHandler, transactionValidationCompleteCallbackMethodId, requestIdBytes, statusBytes);
    g_vm->DetachCurrentThread();
}

void connectivityStatusCallback(uint64_t status) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    jbyteArray requestIdBytes = getBytesFromUnsignedLongLong(jniEnv, status);
    jniEnv->CallVoidMethod(callbackHandler, connectivityStatusCallbackId, requestIdBytes);
    g_vm->DetachCurrentThread();
}

void balanceUpdatedCallback(TariBalance *pBalance) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    auto jpBalance = reinterpret_cast<jlong>(pBalance);
    jniEnv->CallVoidMethod(callbackHandler, balanceUpdatedCallbackMethodId, jpBalance);
    g_vm->DetachCurrentThread();
}

void storeAndForwardMessagesReceivedCallback() {
    // no-op
}


void recoveringProcessCompleteCallback(uint8_t first, uint64_t second, uint64_t third) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr || callbackHandler == nullptr) {
        return;
    }
    jbyteArray bytes2 = getBytesFromUnsignedLongLong(jniEnv, second);
    jbyteArray bytes3 = getBytesFromUnsignedLongLong(jniEnv, third);
    jniEnv->CallVoidMethod(callbackHandler, recoveringProcessCompleteCallbackMethodId, static_cast<jint>(first), bytes2, bytes3);
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
        jstring jNetwork,
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
        jstring callback_tx_faux_confirmed,
        jstring callback_tx_faux_confirmed_sig,
        jstring callback_tx_faux_unconfirmed,
        jstring callback_tx_faux_unconfirmed_sig,
        jstring callback_direct_send_result,
        jstring callback_direct_send_result_sig,
        jstring callback_tx_cancellation,
        jstring callback_tx_cancellation_sig,
        jstring callback_txo_validation_complete,
        jstring callback_txo_validation_complete_sig,
        jstring callback_contacts_liveness_data_updated,
        jstring callback_contacts_liveness_data_updated_sig,
        jstring callback_balance_updated,
        jstring callback_balance_updated_sig,
        jstring callback_transaction_validation_complete,
        jstring callback_transaction_validation_complete_sig,
        jstring callback_connectivity_status,
        jstring callback_connectivity_status_sig,
        jobject error) {

    int errorCode = 0;
    if (callbackHandler == nullptr) {
        callbackHandler = jEnv->NewGlobalRef(jThis);
    }
    jclass jClass = jEnv->GetObjectClass(jThis);
    if (jClass == nullptr) {
        SetNullPointerField(jEnv, jThis);
    }

    txReceivedCallbackMethodId = getMethodId(jEnv, jThis, txReceivedCallbackMethodName, txReceivedCallbackMethodSignature);
    if (txReceivedCallbackMethodId == nullptr) {
        SetNullPointerField(jEnv, jThis);
    }

    txReplyReceivedCallbackMethodId = getMethodId(jEnv, jThis, callback_received_tx_reply, callback_received_tx_reply_sig);
    if (txReplyReceivedCallbackMethodId == nullptr) {
        SetNullPointerField(jEnv, jThis);
    }

    txFinalizedCallbackMethodId = getMethodId(jEnv, jThis, callback_received_finalized_tx, callback_received_finalized_tx_sig);
    if (txFinalizedCallbackMethodId == nullptr) {
        SetNullPointerField(jEnv, jThis);
    }

    txBroadcastCallbackMethodId = getMethodId(jEnv, jThis, callback_tx_broadcast, callback_tx_broadcast_sig);
    if (txBroadcastCallbackMethodId == nullptr) {
        SetNullPointerField(jEnv, jThis);
    }

    txMinedCallbackMethodId = getMethodId(jEnv, jThis, callback_tx_mined, callback_tx_mined_sig);
    if (txMinedCallbackMethodId == nullptr) {
        SetNullPointerField(jEnv, jThis);
    }

    txMinedUnconfirmedCallbackMethodId = getMethodId(jEnv, jThis, callback_tx_mined_unconfirmed, callback_tx_mined_unconfirmed_sig);
    if (txMinedUnconfirmedCallbackMethodId == nullptr) {
        SetNullPointerField(jEnv, jThis);
    }

    txFauxConfirmedCallbackMethodId = getMethodId(jEnv, jThis, callback_tx_faux_confirmed, callback_tx_faux_confirmed_sig);
    if (txFauxConfirmedCallbackMethodId == nullptr) {
        SetNullPointerField(jEnv, jThis);
    }

    txFauxUnconfirmedCallbackMethodId = getMethodId(jEnv, jThis, callback_tx_faux_unconfirmed, callback_tx_faux_unconfirmed_sig);
    if (txFauxUnconfirmedCallbackMethodId == nullptr) {
        SetNullPointerField(jEnv, jThis);
    }

    directSendResultCallbackMethodId = getMethodId(jEnv, jThis, callback_direct_send_result, callback_direct_send_result_sig);
    if (directSendResultCallbackMethodId == nullptr) {
        SetNullPointerField(jEnv, jThis);
    }

    txCancellationCallbackMethodId = getMethodId(jEnv, jThis, callback_tx_cancellation, callback_tx_cancellation_sig);
    if (txCancellationCallbackMethodId == nullptr) {
        SetNullPointerField(jEnv, jThis);
    }

    connectivityStatusCallbackId = getMethodId(jEnv, jThis, callback_connectivity_status, callback_connectivity_status_sig);
    if (connectivityStatusCallbackId == nullptr) {
        SetNullPointerField(jEnv, jThis);
    }

    txoValidationCompleteCallbackMethodId = getMethodId(jEnv, jThis, callback_txo_validation_complete, callback_txo_validation_complete_sig);
    if (txoValidationCompleteCallbackMethodId == nullptr) {
        SetNullPointerField(jEnv, jThis);
    }

    transactionValidationCompleteCallbackMethodId = getMethodId(jEnv, jThis, callback_transaction_validation_complete,
                                                                callback_transaction_validation_complete_sig);
    if (transactionValidationCompleteCallbackMethodId == nullptr) {
        SetNullPointerField(jEnv, jThis);
    }

    contactsLivenessDataUpdatedCallbackMethodId = getMethodId(jEnv, jThis, callback_contacts_liveness_data_updated,
                                                              callback_contacts_liveness_data_updated_sig);
    if (contactsLivenessDataUpdatedCallbackMethodId == nullptr) {
        SetNullPointerField(jEnv, jThis);
    }

    balanceUpdatedCallbackMethodId = getMethodId(jEnv, jThis, callback_balance_updated, callback_balance_updated_sig);
    if (balanceUpdatedCallbackMethodId == nullptr) {
        SetNullPointerField(jEnv, jThis);
    }

    auto pWalletConfig = GetPointerField<TariCommsConfig *>(jEnv, jpWalletConfig);

    const char *pLogPath = jEnv->GetStringUTFChars(jLogPath, JNI_FALSE);
    if (strlen(pLogPath) == 0) {
        pLogPath = nullptr;
    }

    const char *pPassphrase = nullptr;
    if (jPassphrase != nullptr) {
        pPassphrase = jEnv->GetStringUTFChars(jPassphrase, JNI_FALSE);
    }

    const char *pNetwork = nullptr;
    if (jNetwork != nullptr) {
        pNetwork = jEnv->GetStringUTFChars(jNetwork, JNI_FALSE);
    }

    bool jRecoveryInProgress = false;
    bool *pRecovery = &jRecoveryInProgress;

    TariSeedWords *pSeedWords = nullptr;
    if (jSeed_words != nullptr) {
        pSeedWords = GetPointerField<TariSeedWords *>(jEnv, jSeed_words);
    }

    TariWallet *pWallet = wallet_create(
            pWalletConfig,
            pLogPath,
            static_cast<unsigned int>(maxNumberOfRollingLogFiles),
            static_cast<unsigned int>(rollingLogFileMaxSizeBytes),
            pPassphrase,
            pSeedWords,
            pNetwork,
            txReceivedCallback,
            txReplyReceivedCallback,
            txFinalizedCallback,
            txBroadcastCallback,
            txMinedCallback,
            txMinedUnconfirmedCallback,
            txFauxConfirmedCallback,
            txFauxUnconfirmedCallback,
            txDirectSendResultCallback,
            txCancellationCallback,
            txoValidationCompleteCallback,
            contactsLivenessDataUpdatedCallback,
            balanceUpdatedCallback,
            transactionValidationCompleteCallback,
            storeAndForwardMessagesReceivedCallback,
            connectivityStatusCallback,
            pRecovery,
            &errorCode);

    setErrorCode(jEnv, error, errorCode);
    jEnv->ReleaseStringUTFChars(jLogPath, pLogPath);
    SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(pWallet));
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetBalance(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithErrorAndCast<TariBalance *>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        return wallet_get_balance(pWallet, errorPointer);
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetUtxos(
        JNIEnv *jEnv,
        jobject jThis,
        jint jPage,
        jint jPageSize,
        jint jSorting,
        jlong jDustThreshold,
        jobject error) {
    return ExecuteWithErrorAndCast<TariVector *>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        auto pSorting = (TariUtxoSort) jSorting;
        //todo states
        return wallet_get_utxos(pWallet, jPage, jPageSize, pSorting, nullptr, jDustThreshold, errorPointer);
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetAllUtxos(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithErrorAndCast<TariVector *>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        return wallet_get_all_utxos(pWallet, errorPointer);
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniLogMessage(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jMessage,
        jobject error) {
    ExecuteWithError(jEnv, error, [&](int *errorPointer) {
        const char *pMessage = jEnv->GetStringUTFChars(jMessage, JNI_FALSE);
        log_debug_message(pMessage, errorPointer);
        jEnv->ReleaseStringUTFChars(jMessage, pMessage);
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetWalletAddress(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithErrorAndCast<TariWalletAddress *>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        return wallet_get_tari_address(pWallet, errorPointer);
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetContacts(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithErrorAndCast<TariContacts *>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        return wallet_get_contacts(pWallet, errorPointer);
    });
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniAddUpdateContact(
        JNIEnv *jEnv,
        jobject jThis,
        jobject jpContact,
        jobject error) {
    return ExecuteWithError<jboolean>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        auto pContact = GetPointerField<TariContact *>(jEnv, jpContact);
        return static_cast<jboolean>(wallet_upsert_contact(pWallet, pContact, errorPointer) != 0);
    });
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniRemoveContact(
        JNIEnv *jEnv,
        jobject jThis,
        jobject jpContact,
        jobject error) {
    return ExecuteWithError<jboolean>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        auto pContact = GetPointerField<TariContact *>(jEnv, jpContact);
        return static_cast<jboolean>(wallet_remove_contact(pWallet, pContact, errorPointer) != 0);
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetCompletedTxs(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithErrorAndCast<TariCompletedTransactions *>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        return wallet_get_completed_transactions(pWallet, errorPointer);
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetCancelledTxs(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithErrorAndCast<TariCompletedTransactions *>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        return wallet_get_cancelled_transactions(pWallet, errorPointer);
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetCompletedTxById(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jTxId,
        jobject error) {
    return ExecuteWithError<jlong>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        const char *nativeString = jEnv->GetStringUTFChars(jTxId, JNI_FALSE);
        char *pEnd;
        unsigned long long id = strtoull(nativeString, &pEnd, 10);
        auto result = reinterpret_cast<jlong>(wallet_get_completed_transaction_by_id(pWallet, id, errorPointer));
        jEnv->ReleaseStringUTFChars(jTxId, nativeString);
        return result;
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetCancelledTxById(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jTxId,
        jobject error) {
    return ExecuteWithError<jlong>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        const char *nativeString = jEnv->GetStringUTFChars(jTxId, JNI_FALSE);
        char *pEnd;
        unsigned long long id = strtoull(nativeString, &pEnd, 10);
        auto result = reinterpret_cast<jlong>(wallet_get_cancelled_transaction_by_id(pWallet, id, errorPointer));
        jEnv->ReleaseStringUTFChars(jTxId, nativeString);
        return result;
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetPendingOutboundTxs(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithErrorAndCast<TariPendingOutboundTransactions *>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        return wallet_get_pending_outbound_transactions(pWallet, errorPointer);
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetPendingOutboundTxById(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jTxId,
        jobject error) {
    return ExecuteWithError<jlong>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        const char *nativeString = jEnv->GetStringUTFChars(jTxId, JNI_FALSE);
        char *pEnd;
        unsigned long long id = strtoull(nativeString, &pEnd, 10);
        auto result = reinterpret_cast<jlong>(   wallet_get_pending_outbound_transaction_by_id(pWallet, id, errorPointer));
        jEnv->ReleaseStringUTFChars(jTxId, nativeString);
        return result;
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetPendingInboundTxs(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithErrorAndCast<TariPendingInboundTransactions *>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        return wallet_get_pending_inbound_transactions(pWallet, errorPointer);
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetPendingInboundTxById(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jTxId,
        jobject error) {
    return ExecuteWithError<jlong>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        const char *nativeString = jEnv->GetStringUTFChars(jTxId, JNI_FALSE);
        char *pEnd;
        unsigned long long id = strtoull(nativeString, &pEnd, 10);
        auto result = reinterpret_cast<jlong>(wallet_get_pending_inbound_transaction_by_id(pWallet, id, errorPointer));
        jEnv->ReleaseStringUTFChars(jTxId, nativeString);
        return result;
    });
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniCancelPendingTx(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jTxId,
        jobject error) {
    return ExecuteWithError<jlong>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        const char *nativeString = jEnv->GetStringUTFChars(jTxId, JNI_FALSE);
        char *pEnd;
        unsigned long long id = strtoull(nativeString, &pEnd, 10);
        auto result = static_cast<jboolean>(wallet_cancel_pending_transaction(pWallet, id, errorPointer));
        jEnv->ReleaseStringUTFChars(jTxId, nativeString);
        return result;
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
    jEnv->DeleteGlobalRef(callbackHandler);
    callbackHandler = nullptr;
    wallet_destroy(pWallet);
    SetNullPointerField(jEnv, jThis);
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniEstimateTxFee(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jAmount,
        jstring jGramFee,
        jstring jKernelCount,
        jstring jOutputCount,
        jobject error) {
    int errorCode = 0;
    auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
    const char *nativeAmount = jEnv->GetStringUTFChars(jAmount, JNI_FALSE);
    const char *nativeGramFee = jEnv->GetStringUTFChars(jGramFee, JNI_FALSE);
    const char *nativeKernels = jEnv->GetStringUTFChars(jKernelCount, JNI_FALSE);
    const char *nativeOutputs = jEnv->GetStringUTFChars(jOutputCount, JNI_FALSE);
    char *pAmountEnd;
    char *pGramFeeEnd;
    char *pKernelsEnd;
    char *pOutputsEnd;

    unsigned long long amount = strtoull(nativeAmount, &pAmountEnd, 10);
    unsigned long long gramFee = strtoull(nativeGramFee, &pGramFeeEnd, 10);
    unsigned long long kernels = strtoull(nativeKernels, &pKernelsEnd, 10);
    unsigned long long outputs = strtoull(nativeOutputs, &pOutputsEnd, 10);

    jbyteArray result = getBytesFromUnsignedLongLong(jEnv, wallet_get_fee_estimate(pWallet, amount, nullptr, gramFee, kernels, outputs, &errorCode));
    setErrorCode(jEnv, error, errorCode);
    jEnv->ReleaseStringUTFChars(jAmount, nativeAmount);
    jEnv->ReleaseStringUTFChars(jGramFee, nativeGramFee);
    jEnv->ReleaseStringUTFChars(jKernelCount, nativeKernels);
    jEnv->ReleaseStringUTFChars(jOutputCount, nativeOutputs);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniJoinUtxos(
        JNIEnv *jEnv,
        jobject jThis,
        jobjectArray jCommitments,
        jstring jFeePerGram,
        jobject error) {
    return ExecuteWithError<jlong>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);

        int size = jEnv->GetArrayLength(jCommitments);
        auto *pTariVector = create_tari_vector(Text);
        for (int i = 0; i < size; ++i) {
            auto commitmentItem = (jstring) jEnv->GetObjectArrayElement(jCommitments, i);
            const char *commitmentRef = jEnv->GetStringUTFChars(commitmentItem, JNI_FALSE);
            tari_vector_push_string(pTariVector, commitmentRef, errorPointer);
        }

        const char *nativeGramFee = jEnv->GetStringUTFChars(jFeePerGram, JNI_FALSE);
        char *pGramFeeEnd;
        unsigned long feePerGram = strtoull(nativeGramFee, &pGramFeeEnd, 10);
        return wallet_coin_join(pWallet, pTariVector, feePerGram, errorPointer);
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniSplitUtxos(
        JNIEnv *jEnv,
        jobject jThis,
        jobjectArray jCommitments,
        jstring jSplitCount,
        jstring jFeePerGram,
        jobject error) {
    return ExecuteWithError<jlong>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);

        int size = jEnv->GetArrayLength(jCommitments);
        auto *pTariVector = create_tari_vector(Text);
        for (int i = 0; i < size; ++i) {
            auto commitmentItem = (jstring) jEnv->GetObjectArrayElement(jCommitments, i);
            const char *commitmentRef = jEnv->GetStringUTFChars(commitmentItem, JNI_FALSE);
            tari_vector_push_string(pTariVector, commitmentRef, errorPointer);
        }

        const char *nativeSplitCount = jEnv->GetStringUTFChars(jSplitCount, JNI_FALSE);
        const char *nativeGramFee = jEnv->GetStringUTFChars(jFeePerGram, JNI_FALSE);
        char *pSplitCount;
        char *pGramFeeEnd;
        unsigned int splitCount = strtoull(nativeSplitCount, &pSplitCount, 10);
        unsigned long feePerGram = strtoull(nativeGramFee, &pGramFeeEnd, 10);
        return wallet_coin_split(pWallet, pTariVector, splitCount, feePerGram, errorPointer);
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniPreviewJoinUtxos(
        JNIEnv *jEnv,
        jobject jThis,
        jobjectArray jCommitments,
        jstring jFeePerGram,
        jobject error) {
    return ExecuteWithErrorAndCast<TariCoinPreview *>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);

        int size = jEnv->GetArrayLength(jCommitments);
        auto *pTariVector = create_tari_vector(Text);
        for (int i = 0; i < size; ++i) {
            auto commitmentItem = (jstring) jEnv->GetObjectArrayElement(jCommitments, i);
            const char *commitmentRef = jEnv->GetStringUTFChars(commitmentItem, JNI_FALSE);
            tari_vector_push_string(pTariVector, commitmentRef, errorPointer);
        }

        const char *nativeGramFee = jEnv->GetStringUTFChars(jFeePerGram, JNI_FALSE);
        char *pGramFeeEnd;
        unsigned long feePerGram = strtoull(nativeGramFee, &pGramFeeEnd, 10);
        return wallet_preview_coin_join(pWallet, pTariVector, feePerGram, errorPointer);
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniPreviewSplitUtxos(
        JNIEnv *jEnv,
        jobject jThis,
        jobjectArray jCommitments,
        jstring jSplitCount,
        jstring jFeePerGram,
        jobject error) {
    return ExecuteWithErrorAndCast<TariCoinPreview *>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);

        int size = jEnv->GetArrayLength(jCommitments);
        auto *pTariVector = create_tari_vector(Text);
        for (int i = 0; i < size; ++i) {
            auto commitmentItem = (jstring) jEnv->GetObjectArrayElement(jCommitments, i);
            const char *commitmentRef = jEnv->GetStringUTFChars(commitmentItem, JNI_FALSE);
            tari_vector_push_string(pTariVector, commitmentRef, errorPointer);
        }

        const char *nativeSplitCount = jEnv->GetStringUTFChars(jSplitCount, JNI_FALSE);
        const char *nativeGramFee = jEnv->GetStringUTFChars(jFeePerGram, JNI_FALSE);
        char *pSplitCount;
        char *pGramFeeEnd;
        unsigned int splitCount = strtoull(nativeSplitCount, &pSplitCount, 10);
        unsigned long feePerGram = strtoull(nativeGramFee, &pGramFeeEnd, 10);
        return wallet_preview_coin_split(pWallet, pTariVector, splitCount, feePerGram, errorPointer);
    });
}
//
//extern "C"
//JNIEXPORT jbyteArray JNICALL
//Java_com_tari_android_wallet_ffi_FFIWallet_jniImportUTXO(
//        JNIEnv *jEnv,
//        jobject jThis,
//        jstring jAmount,
//        jobject jpSpendingKey,
//        jobject jpSourcePublicKey,
//        jobject jpTariCommitmentSignature,
//        jobject jpSourceSenderPublicKey,
//        jobject jpScriptPrivateKey,
//        jstring jMessage,
//        jobject error) {
//
//    auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
//
//    auto pSpendingKey = GetPointerField<TariPrivateKey *>(jEnv, jpSpendingKey);
//
//    auto pSourcePublicKey = GetPointerField<TariWalletAddress *>(jEnv, jpSourcePublicKey);
//
//    auto pSourceSenderPublicKey = GetPointerField<TariPublicKey *>(jEnv, jpSourceSenderPublicKey);
//
//    auto pTariCommitmentSignature = GetPointerField<TariCommitmentSignature *>(jEnv, jpTariCommitmentSignature);
//
//    auto pScriptPrivateKey = GetPointerField<TariPrivateKey *>(jEnv, jpScriptPrivateKey);
//
//    char *pAmountEnd;
//    const char *nativeAmount = jEnv->GetStringUTFChars(jAmount, JNI_FALSE);
//    const char *pMessage = jEnv->GetStringUTFChars(jMessage, JNI_FALSE);
//    unsigned long long amount = strtoull(nativeAmount, &pAmountEnd, 10);
//
//    return ExecuteWithError<jbyteArray>(jEnv, error, [&](int *errorPointer) {
//        jbyteArray result = getBytesFromUnsignedLongLong(
//                jEnv,
//                wallet_import_external_utxo_as_non_rewindable(
//                        pWallet,
//                        amount,
//                        pSpendingKey,
//                        pSourcePublicKey,
//                        nullptr,
//                        pTariCommitmentSignature,
//                        pSourceSenderPublicKey,
//                        pScriptPrivateKey,
//                        nullptr,
//                        nullptr,
//                        0,
//                        pMessage,
//                        errorPointer
//                )
//        );
//
//        jEnv->ReleaseStringUTFChars(jAmount, nativeAmount);
//        jEnv->ReleaseStringUTFChars(jMessage, pMessage);
//        return result;
//    });
//}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniAddBaseNodePeer(
        JNIEnv *jEnv,
        jobject jThis,
        jobject jPublicKey,
        jstring jAddress,
        jobject error) {
    return ExecuteWithError<jboolean>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        auto pPublicKey = GetPointerField<TariPublicKey *>(jEnv, jPublicKey);
        char *pAddress = const_cast<char *>(jEnv->GetStringUTFChars(jAddress, JNI_FALSE));
        auto result = static_cast<jboolean>(  wallet_add_base_node_peer(pWallet, pPublicKey, pAddress, errorPointer) != 0);
        jEnv->ReleaseStringUTFChars(jAddress, pAddress);
        return result;
    });
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniStartTxValidation(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithError<jbyteArray>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        return getBytesFromUnsignedLongLong(jEnv, wallet_start_transaction_validation(pWallet, errorPointer));
    });
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniRestartTxBroadcast(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithError<jbyteArray>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        return getBytesFromUnsignedLongLong(jEnv, wallet_restart_transaction_broadcast(pWallet, errorPointer));
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniPowerModeNormal(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    ExecuteWithError(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        wallet_set_normal_power_mode(pWallet, errorPointer);
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniPowerModeLow(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    ExecuteWithError(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        wallet_set_low_power_mode(pWallet, errorPointer);
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetSeedWords(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithErrorAndCast<TariSeedWords *>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        return wallet_get_seed_words(pWallet, errorPointer);
    });
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniSetKeyValue(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jKey,
        jstring jValue,
        jobject error) {
    return ExecuteWithError<jboolean>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        const char *pKey = jEnv->GetStringUTFChars(jKey, JNI_FALSE);
        const char *pValue = jEnv->GetStringUTFChars(jValue, JNI_FALSE);
        auto result = static_cast<jboolean>(wallet_set_key_value(pWallet, pKey, pValue, errorPointer));
        jEnv->ReleaseStringUTFChars(jKey, pKey);
        jEnv->ReleaseStringUTFChars(jValue, pValue);
        return result;
    });
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniStartTXOValidation(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithError<jbyteArray>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        return getBytesFromUnsignedLongLong(jEnv, wallet_start_txo_validation(pWallet, errorPointer));
    });
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetKeyValue(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jKey,
        jobject error) {
    return ExecuteWithError<jstring>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        const char *pKey = jEnv->GetStringUTFChars(jKey, JNI_FALSE);
        const char *pValue = wallet_get_value(pWallet, pKey, errorPointer);
        jEnv->ReleaseStringUTFChars(jKey, pKey);
        jstring result = jEnv->NewStringUTF(pValue);
        string_destroy(const_cast<char *>(pValue));
        return result;
    });
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniRemoveKeyValue(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jKey,
        jobject error) {
    return ExecuteWithError<jboolean>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        const char *pKey = jEnv->GetStringUTFChars(jKey, JNI_FALSE);
        auto result = static_cast<jboolean>(wallet_clear_value(pWallet, pKey, errorPointer));
        jEnv->ReleaseStringUTFChars(jKey, pKey);
        return result;
    });
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniGetConfirmations(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithError<jbyteArray>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        return getBytesFromUnsignedLongLong(jEnv, wallet_get_num_confirmations_required(pWallet, errorPointer));
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniSetConfirmations(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jNumber,
        jobject error) {
    ExecuteWithError(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        const char *nativeString = jEnv->GetStringUTFChars(jNumber, JNI_FALSE);
        char *pEnd;
        unsigned long long number = strtoull(nativeString, &pEnd, 10);
        wallet_set_num_confirmations_required(pWallet, number, errorPointer);
        jEnv->ReleaseStringUTFChars(jNumber, nativeString);
    });
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniSendTx(
        JNIEnv *jEnv,
        jobject jThis,
        jobject jDestination,
        jstring jAmount,
        jstring jFeePerGram,
        jstring jMessage,
        jboolean jOneSided,
        jobject error) {
    return ExecuteWithError<jbyteArray>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        auto pDestination = GetPointerField<TariWalletAddress *>(jEnv, jDestination);
        const char *nativeAmount = jEnv->GetStringUTFChars(jAmount, JNI_FALSE);
        const char *nativeFeePerGram = jEnv->GetStringUTFChars(jFeePerGram, JNI_FALSE);
        const char *pMessage = jEnv->GetStringUTFChars(jMessage, JNI_FALSE);
        char *pAmountEnd;
        char *pFeeEnd;
        unsigned long long feePerGram = strtoull(nativeFeePerGram, &pFeeEnd, 10);
        unsigned long long amount = strtoull(nativeAmount, &pAmountEnd, 10);

        jbyteArray result = getBytesFromUnsignedLongLong(
                jEnv,
                wallet_send_transaction(pWallet, pDestination, amount, nullptr, feePerGram, pMessage,
                                        jOneSided, errorPointer));
        jEnv->ReleaseStringUTFChars(jAmount, nativeAmount);
        jEnv->ReleaseStringUTFChars(jFeePerGram, nativeFeePerGram);
        jEnv->ReleaseStringUTFChars(jMessage, pMessage);
        return result;
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniApplyEncryption(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jPassphrase,
        jobject error) {
    ExecuteWithError(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        const char *pKey = jEnv->GetStringUTFChars(jPassphrase, JNI_FALSE);
        wallet_apply_encryption(pWallet, pKey, errorPointer);
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniRemoveEncryption(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    ExecuteWithError(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        wallet_remove_encryption(pWallet, errorPointer);
    });
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniStartRecovery(
        JNIEnv *jEnv,
        jobject jThis,
        jobject base_node_public_key,
        jstring callback,
        jstring callback_sig,
        jstring recovery_output_message,
        jobject error) {
    return ExecuteWithError<jboolean>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        auto pTariPublicKey = GetPointerField<TariPublicKey *>(jEnv, base_node_public_key);
        recoveringProcessCompleteCallbackMethodId = getMethodId(jEnv, jThis, callback, callback_sig);
        if (recoveringProcessCompleteCallbackMethodId == nullptr) {
            SetNullPointerField(jEnv, jThis);
        }

        const char *pRecoveryOutputMessage = jEnv->GetStringUTFChars(recovery_output_message, JNI_FALSE);

        return wallet_start_recovery(pWallet, pTariPublicKey, recoveringProcessCompleteCallback, pRecoveryOutputMessage, errorPointer);
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIWallet_jniWalletGetFeePerGramStats(
        JNIEnv *jEnv,
        jobject jThis,
        jint count,
        jobject error
) {
    return ExecuteWithErrorAndCast<TariFeePerGramStats *>(jEnv, error, [&](int *errorPointer) {
        auto pWallet = GetPointerField<TariWallet *>(jEnv, jThis);
        return wallet_get_fee_per_gram_stats(pWallet, count, errorPointer);
    });
}