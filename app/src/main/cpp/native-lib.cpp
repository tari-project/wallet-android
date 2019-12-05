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

#include "../../../../jniLibs/wallet.h"
#include <android/log.h>

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
    int getEnvStat = g_vm->GetEnv((void **) &jniEnv, JNI_VERSION_1_6);
    if (getEnvStat == JNI_EDETACHED) {
        LOGD("GetEnv: not attached, will attach.");
        if (g_vm->AttachCurrentThread(&jniEnv, nullptr) != 0) {
            LOGE("VM failed to attach.");
            return nullptr;
        } else {
            LOGD("VM attached.");
        }
    } else if (getEnvStat == JNI_OK) {
        // no-op
    } else if (getEnvStat == JNI_EVERSION) {
        LOGE("GetEnv: JNI version not supported.");
        return nullptr;
    }
    return jniEnv;
}

/**
 * TX broadcast callback function forward declaration.
 */
void onTransactionBroadcast(struct TariCompletedTransaction *);

/**
 * TX received callback function forward declaration.
 */
void onTransactionReceived(struct TariPendingInboundTransaction *);

/**
 * TX mined callback function forward declaration.
 */
void onTransactionMined(struct TariCompletedTransaction *);

/**
 * TX reply received callback function forward declaration.
 */
void onReceivedTransactionReply(struct TariCompletedTransaction *);

//region Byte Vector

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_ByteVector_createJNI(
        JNIEnv *jEnv,
        jclass jClass,
        jstring jStr) {
    // get string length
    const auto strLen = (unsigned int) jEnv->GetStringUTFLength(jStr);
    // get native string
    const auto *pStr = jEnv->GetStringUTFChars(jStr, JNI_FALSE);
    // create byte vector
    ByteVector *pByteVector = byte_vector_create(
            reinterpret_cast<const unsigned char *>(pStr),
            strLen);
    // release native string
    jEnv->ReleaseStringUTFChars(jStr, pStr);
    // return pointer
    return (jlong) pByteVector;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_ByteVector_getLengthJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpByteVector) {
    // cast pointer
    auto *pByteVector = (ByteVector *) jpByteVector;
    // return length
    return byte_vector_get_length(pByteVector);
}

extern "C"
JNIEXPORT jchar JNICALL
Java_com_tari_android_wallet_ffi_ByteVector_getAtJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpByteVector,
        jint index) {
    // cast pointer
    auto *pByteVector = (ByteVector *) jpByteVector;
    return byte_vector_get_at(pByteVector, (unsigned int) index);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_ByteVector_destroyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpByteVector) {
    // cast pointer & destroy
    byte_vector_destroy((ByteVector *) jpByteVector);
}

//endregion

//region Public Key

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_PublicKey_createJNI(
        JNIEnv *jEnv,
        jclass jClass,
        jlong jpByteVector) {
    // cast pointer
    auto *pByteVector = (ByteVector *) jpByteVector;
    auto *pPublicKey = public_key_create(pByteVector);
    // return pointer
    return (jlong) pPublicKey;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_PublicKey_fromHexJNI(
        JNIEnv *jEnv,
        jclass jClass,
        jstring jHexStr) {
    const auto *pStr = jEnv->GetStringUTFChars(jHexStr, JNI_FALSE);
    TariPublicKey *pPublicKey = public_key_from_hex(pStr);
    jEnv->ReleaseStringUTFChars(jHexStr, pStr);
    return (jlong) pPublicKey;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_PublicKey_fromPrivateKeyJNI(
        JNIEnv *jEnv,
        jclass jClass,
        jlong jpPrivateKey) {
    // cast pointer
    auto *pPrivateKey = (TariPrivateKey *) jpPrivateKey;
    auto *pPublicKey = public_key_from_private_key(pPrivateKey);
    return (jlong) pPublicKey;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_PublicKey_getBytesJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpPublicKey) {
    // cast pointer
    auto *pPublicKey = (TariPublicKey *) jpPublicKey;
    // get byte vector
    auto *pByteVector = public_key_get_bytes(pPublicKey);
    return (jlong) pByteVector;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_PublicKey_destroyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpPublicKey) {
    public_key_destroy((TariPublicKey *) jpPublicKey);
}

//endregion

//region Private Key

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_PrivateKey_createJNI(
        JNIEnv *jEnv,
        jclass jClass,
        jlong jpByteVector) {
    // cast pointer
    auto *pByteVector = (ByteVector *) jpByteVector;
    auto *pPrivateKey = private_key_create(pByteVector);
    return (jlong) pPrivateKey;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_PrivateKey_generateJNI(
        JNIEnv *jEnv,
        jclass jClass) {
    auto *pPrivateKey = private_key_generate();
    return (jlong) pPrivateKey;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_PrivateKey_fromHexJNI(
        JNIEnv *jEnv,
        jclass jClass,
        jstring jHexStr) {
    const auto *pStr = jEnv->GetStringUTFChars(jHexStr, JNI_FALSE);
    TariPrivateKey *pPrivateKey = private_key_from_hex(pStr);
    jEnv->ReleaseStringUTFChars(jHexStr, pStr);
    return (jlong) pPrivateKey;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_PrivateKey_getBytesJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpPrivateKey) {
    auto *pPrivateKey = (TariPrivateKey *) jpPrivateKey;
    auto *pByteVector = private_key_get_bytes(pPrivateKey);
    return (jlong) pByteVector;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_PrivateKey_destroyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpPrivateKey) {
    private_key_destroy((TariPrivateKey *) jpPrivateKey);
}

//endregion

//region Contact

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Contact_createJNI(
        JNIEnv *jEnv,
        jclass jClass,
        jstring jAlias,
        jlong jpPublicKey) {
    // get native string
    const auto *pAlias = jEnv->GetStringUTFChars(jAlias, JNI_FALSE);
    auto *pPublicKey = (TariPublicKey *) jpPublicKey;
    const auto *pContact = contact_create(pAlias, pPublicKey);
    // release native string
    jEnv->ReleaseStringUTFChars(jAlias, pAlias);
    return (jlong) pContact;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tari_android_wallet_ffi_Contact_getAliasJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpContact) {
    auto *pContact = (TariContact *) jpContact;
    const auto *pAlias = contact_get_alias(pContact);
    return jEnv->NewStringUTF(pAlias);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Contact_getPublicKeyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpContact) {
    auto *pContact = (TariContact *) jpContact;
    const auto *pPublicKey = contact_get_public_key(pContact);
    return (jlong) pPublicKey;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_Contact_destroyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpContact) {
    contact_destroy((TariContact *) jpContact);
}

//endregion Contact

//region Contacts

extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_Contacts_getLengthJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpContacts) {
    auto *pContacts = (TariContacts *) jpContacts;
    return contacts_get_length(pContacts);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Contacts_getAtJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpContacts,
        jint index) {
    auto *pContacts = (TariContacts *) jpContacts;
    TariContact *pContact = contacts_get_at(pContacts, static_cast<unsigned int>(index));
    return (jlong) pContact;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_Contacts_destroyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpContacts) {
    contacts_destroy((TariContacts *) jpContacts);
}

//endregion

//region CommsConfig

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_CommsConfig_createJNI(
        JNIEnv *jEnv,
        jclass jClass,
        jstring jControlServiceAddress,
        jstring jListenerAddress,
        jstring jDatabaseName,
        jstring jDatastorePath,
        jlong jpPrivateKey) {
    auto *pControlServiceAddress = (char *) jEnv->GetStringUTFChars(jControlServiceAddress,
                                                                    JNI_FALSE);
    auto *pListenerAddress = (char *) jEnv->GetStringUTFChars(jListenerAddress, JNI_FALSE);
    auto *pDatabaseName = (char *) jEnv->GetStringUTFChars(jDatabaseName, JNI_FALSE);
    auto *pDatastorePath = (char *) jEnv->GetStringUTFChars(jDatastorePath, JNI_FALSE);
    auto *pPrivateKey = (TariPrivateKey *) jpPrivateKey;

    TariCommsConfig *pCommsConfig = comms_config_create(
            pControlServiceAddress,
            pListenerAddress,
            pDatabaseName,
            pDatastorePath,
            pPrivateKey);

    jEnv->ReleaseStringUTFChars(jControlServiceAddress, pControlServiceAddress);
    jEnv->ReleaseStringUTFChars(jListenerAddress, pListenerAddress);
    jEnv->ReleaseStringUTFChars(jDatabaseName, pDatabaseName);
    jEnv->ReleaseStringUTFChars(jDatastorePath, pDatastorePath);

    return (jlong) pCommsConfig;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_CommsConfig_destroyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpCommsConfig) {
    comms_config_destroy((TariCommsConfig *) jpCommsConfig);
}

//endregion

// region Wallet

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Wallet_createJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWalletConfig,
        jstring jLogPath) {
    auto *pWalletConfig = (TariWalletConfig *) jpWalletConfig;
    auto *pLogPath = (char *) jEnv->GetStringUTFChars(jLogPath, JNI_FALSE);
    const auto *pWallet = wallet_create(pWalletConfig, pLogPath);
    jEnv->ReleaseStringUTFChars(jLogPath, pLogPath);
    return (jlong) pWallet;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Wallet_getPublicKeyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet) {
    auto *pWallet = (TariWallet *) jpWallet;
    const auto *pPublicKey = wallet_get_public_key(pWallet);
    return (jlong) pPublicKey;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Wallet_getAvailableBalanceJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet) {
    auto *pWallet = (TariWallet *) jpWallet;
    return (jlong) wallet_get_available_balance(pWallet);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Wallet_getPendingIncomingBalanceJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet) {
    auto *pWallet = (TariWallet *) jpWallet;
    return (jlong) wallet_get_pending_incoming_balance(pWallet);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Wallet_getPendingOutgoingBalanceJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet) {
    auto *pWallet = (TariWallet *) jpWallet;
    return (jlong) wallet_get_pending_outgoing_balance(pWallet);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Wallet_getContactsJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet) {
    auto *pWallet = (TariWallet *) jpWallet;
    const auto *pContacts = wallet_get_contacts(pWallet);
    return (jlong) pContacts;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_Wallet_addContactJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jlong jpContact) {
    auto *pWallet = (TariWallet *) jpWallet;
    auto *pContact = (TariContact *) jpContact;
    return (jboolean) wallet_add_contact(pWallet, pContact);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_Wallet_removeContactJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jlong jpContact) {
    auto *pWallet = (TariWallet *) jpWallet;
    auto *pContact = (TariContact *) jpContact;
    return (jboolean) wallet_remove_contact(pWallet, pContact);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Wallet_getCompletedTransactionsJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet) {
    auto *pWallet = (TariWallet *) jpWallet;
    const auto *pCompletedTransactions = wallet_get_completed_transactions(pWallet);
    return (jlong) pCompletedTransactions;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Wallet_getCompletedTransactionByIdJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jlong jTxId) {
    auto *pWallet = (TariWallet *) jpWallet;
    const auto *pCompletedTransaction = wallet_get_completed_transaction_by_id(pWallet,
                                                                               static_cast<unsigned long long>(jTxId));
    return (jlong) pCompletedTransaction;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Wallet_getPendingOutboundTransactionsJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet) {
    auto *pWallet = (TariWallet *) jpWallet;
    const auto *pPendingOutboundTransactions = wallet_get_pending_outbound_transactions(pWallet);
    return (jlong) pPendingOutboundTransactions;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Wallet_getPendingOutboundTransactionByIdJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jlong jTxId) {
    auto *pWallet = (TariWallet *) jpWallet;
    const auto *pPendingOutboundTransaction = wallet_get_pending_outbound_transaction_by_id(pWallet,
                                                                                            static_cast<unsigned long long>(jTxId));
    return (jlong) pPendingOutboundTransaction;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Wallet_getPendingInboundTransactionsJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet) {
    auto *pWallet = (TariWallet *) jpWallet;
    const auto *pPendingInboundTransactions = wallet_get_pending_inbound_transactions(pWallet);
    return (jlong) pPendingInboundTransactions;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Wallet_getPendingInboundTransactionByIdJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jlong jTxId) {
    auto *pWallet = (TariWallet *) jpWallet;
    const auto *pPendingInboundTransaction = wallet_get_pending_inbound_transaction_by_id(pWallet,
                                                                                          static_cast<unsigned long long>(jTxId));
    return (jlong) pPendingInboundTransaction;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_Wallet_destroyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet) {
    wallet_destroy((TariWallet *) jpWallet);
}

//endregion

//region Completed Transaction

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_CompletedTransaction_getIdJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpCompletedTx) {
    auto *pCompletedTx = (TariCompletedTransaction *) jpCompletedTx;
    return (jlong) completed_transaction_get_transaction_id(pCompletedTx);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_CompletedTransaction_getDestinationPublicKeyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpCompletedTx) {
    auto *pCompletedTx = (TariCompletedTransaction *) jpCompletedTx;
    auto *pPublicKey = completed_transaction_get_destination_public_key(pCompletedTx);
    return (jlong) pPublicKey;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_CompletedTransaction_getSourcePublicKeyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpCompletedTx) {
    auto *pCompletedTx = (TariCompletedTransaction *) jpCompletedTx;
    auto *pPublicKey = completed_transaction_get_source_public_key(pCompletedTx);
    return (jlong) pPublicKey;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_CompletedTransaction_getAmountJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpCompletedTx) {
    auto *pCompletedTx = (TariCompletedTransaction *) jpCompletedTx;
    return (jlong) completed_transaction_get_amount(pCompletedTx);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_CompletedTransaction_getFeeJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpCompletedTx) {
    auto *pCompletedTx = (TariCompletedTransaction *) jpCompletedTx;
    return (jlong) completed_transaction_get_fee(pCompletedTx);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_CompletedTransaction_getTimestampJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpCompletedTx) {
    auto *pCompletedTx = (TariCompletedTransaction *) jpCompletedTx;
    return (jlong) completed_transaction_get_timestamp(pCompletedTx);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tari_android_wallet_ffi_CompletedTransaction_getMessageJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpCompletedTx) {
    auto *pCompletedTx = (TariCompletedTransaction *) jpCompletedTx;
    const auto *pMessage = completed_transaction_get_message(pCompletedTx);
    return jEnv->NewStringUTF(pMessage);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_CompletedTransaction_getStatusJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpCompletedTx) {
    auto *pCompletedTx = (TariCompletedTransaction *) jpCompletedTx;
    return (jint) completed_transaction_get_status(pCompletedTx);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_CompletedTransaction_destroyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpCompletedTx) {
    auto *pCompletedTx = (TariCompletedTransaction *) jpCompletedTx;
    completed_transaction_destroy(pCompletedTx);
}

//endregion

//region Completed Transactions

extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_CompletedTransactions_getLengthJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpCompletedTransactions) {
    auto *pCompletedTransactions = (TariCompletedTransactions *) jpCompletedTransactions;
    return completed_transactions_get_length(pCompletedTransactions);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_CompletedTransactions_getAtJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpCompletedTransactions,
        jint index) {
    auto *pCompletedTransactions = (TariCompletedTransactions *) jpCompletedTransactions;
    TariCompletedTransaction *pTx = completed_transactions_get_at(pCompletedTransactions,
                                                                  static_cast<unsigned int>(index));
    return (jlong) pTx;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_CompletedTransactions_destroyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpCompletedTransactions) {
    completed_transactions_destroy((TariCompletedTransactions *) jpCompletedTransactions);
}

//endregion

//region Pending Outbound Transaction

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_PendingOutboundTransaction_getIdJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpOutboundTx) {
    auto *pOutboundTx = (TariPendingOutboundTransaction *) jpOutboundTx;
    return (jlong) pending_outbound_transaction_get_transaction_id(pOutboundTx);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_PendingOutboundTransaction_getDestinationPublicKeyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpOutboundTx) {
    auto *pOutboundTx = (TariPendingOutboundTransaction *) jpOutboundTx;
    auto *pPublicKey = pending_outbound_transaction_get_destination_public_key(pOutboundTx);
    return (jlong) pPublicKey;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_PendingOutboundTransaction_getAmountJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpOutboundTx) {
    auto *pOutboundTx = (TariPendingOutboundTransaction *) jpOutboundTx;
    return (jlong) pending_outbound_transaction_get_amount(pOutboundTx);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tari_android_wallet_ffi_PendingOutboundTransaction_getMessageJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpOutboundTx) {
    auto *pOutboundTx = (TariPendingOutboundTransaction *) jpOutboundTx;
    const auto *pMessage = pending_outbound_transaction_get_message(pOutboundTx);
    return jEnv->NewStringUTF(pMessage);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_PendingOutboundTransaction_getTimestampJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpOutboundTx) {
    auto *pOutboundTx = (TariPendingOutboundTransaction *) jpOutboundTx;
    return (jlong) pending_outbound_transaction_get_timestamp(pOutboundTx);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_PendingOutboundTransaction_destroyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpOutboundTx) {
    pending_outbound_transaction_destroy((TariPendingOutboundTransaction *) jpOutboundTx);
}

//endregion

//region Pending Outbound Transactions

extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_PendingOutboundTransactions_getLengthJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpOutboundTxs) {
    auto *pOutboundTxs = (TariPendingOutboundTransactions *) jpOutboundTxs;
    return pending_outbound_transactions_get_length(pOutboundTxs);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_PendingOutboundTransactions_getAtJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpOutboundTxs,
        jint index) {
    auto *pOutboundTxs = (TariPendingOutboundTransactions *) jpOutboundTxs;
    return (jlong) pending_outbound_transactions_get_at(pOutboundTxs,
                                                        static_cast<unsigned int>(index));
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_PendingOutboundTransactions_destroyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpOutboundTxs) {
    pending_outbound_transactions_destroy((TariPendingOutboundTransactions *) jpOutboundTxs);
}

//endregion

//region Pending Inbound Transaction

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_PendingInboundTransaction_getIdJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpInboundTx) {
    auto *pInboundTx = (TariPendingInboundTransaction *) jpInboundTx;
    return (jlong) pending_inbound_transaction_get_transaction_id(pInboundTx);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_PendingInboundTransaction_getSourcePublicKeyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpInboundTx) {
    auto *pInboundTx = (TariPendingInboundTransaction *) jpInboundTx;
    auto *pPublicKey = pending_inbound_transaction_get_source_public_key(pInboundTx);
    return (jlong) pPublicKey;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_PendingInboundTransaction_getAmountJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpInboundTx) {
    auto *pInboundTx = (TariPendingInboundTransaction *) jpInboundTx;
    return (jlong) pending_inbound_transaction_get_amount(pInboundTx);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tari_android_wallet_ffi_PendingInboundTransaction_getMessageJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpInboundTx) {
    auto *pInboundTx = (TariPendingInboundTransaction *) jpInboundTx;
    const auto *pMessage = pending_inbound_transaction_get_message(pInboundTx);
    return jEnv->NewStringUTF(pMessage);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_PendingInboundTransaction_getTimestampJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpInboundTx) {
    auto *pInboundTx = (TariPendingInboundTransaction *) jpInboundTx;
    return (jlong) pending_inbound_transaction_get_timestamp(pInboundTx);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_PendingInboundTransaction_destroyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpInboundTx) {
    pending_inbound_transaction_destroy((TariPendingInboundTransaction *) jpInboundTx);
}

//endregion

//region Pending Inbound Transactions

extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_PendingInboundTransactions_getLengthJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpInboundTxs) {
    auto *pInboundTxs = (TariPendingInboundTransactions *) jpInboundTxs;
    return pending_inbound_transactions_get_length(pInboundTxs);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_PendingInboundTransactions_getAtJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpInboundTxs,
        jint index) {
    auto *pInboundTxs = (TariPendingInboundTransactions *) jpInboundTxs;
    return (jlong) pending_inbound_transactions_get_at(pInboundTxs,
                                                       static_cast<unsigned int>(index));
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_PendingInboundTransactions_destroyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpInboundTxs) {
    pending_inbound_transactions_destroy((TariPendingInboundTransactions *) jpInboundTxs);
}

//endregion

//region Callback Registration

jobject txBroadcastCallbackReceiver;
jmethodID txBroadcastCallbackMethodId;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_Wallet_registerTransactionBroadcastListenerJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jobject jCallbackObject) {
    txBroadcastCallbackReceiver = jEnv->NewGlobalRef(jCallbackObject);
    jclass jClass = jEnv->GetObjectClass(jCallbackObject);
    if (jClass == nullptr) {
        return (jboolean) false;
    }
    txBroadcastCallbackMethodId = jEnv->GetMethodID(jClass, "onTransactionBroadcast", "(J)V");
    if (txBroadcastCallbackMethodId == nullptr) {
        return (jboolean) false;
    }
    auto *pWallet = (TariWallet *) jpWallet;
    return (jboolean) wallet_callback_register_transaction_broadcast(pWallet,
                                                                     onTransactionBroadcast);
}

void onTransactionBroadcast(struct TariCompletedTransaction *pCompletedTransaction) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr) {
        return;
    }
    auto jpCompletedTransaction = (jlong) pCompletedTransaction;
    jniEnv->CallVoidMethod(
            txBroadcastCallbackReceiver,
            txBroadcastCallbackMethodId,
            jpCompletedTransaction);
    g_vm->DetachCurrentThread();
}

jobject txMinedCallbackReceiver;
jmethodID txMinedCallbackMethodId;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_Wallet_registerTransactionMinedListenerJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jobject jCallbackObject) {
    txMinedCallbackReceiver = jEnv->NewGlobalRef(jCallbackObject);
    jclass jClass = jEnv->GetObjectClass(jCallbackObject);
    if (jClass == nullptr) {
        return (jboolean) false;
    }
    txMinedCallbackMethodId = jEnv->GetMethodID(jClass, "onTransactionMined", "(J)V");
    if (txMinedCallbackMethodId == nullptr) {
        return (jboolean) false;
    }
    auto *pWallet = (TariWallet *) jpWallet;
    return (jboolean) wallet_callback_register_mined(pWallet, onTransactionMined);
}

void onTransactionMined(struct TariCompletedTransaction *pCompletedTransaction) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr) {
        return;
    }
    auto jpCompletedTransaction = (jlong) pCompletedTransaction;
    jniEnv->CallVoidMethod(
            txMinedCallbackReceiver,
            txMinedCallbackMethodId,
            jpCompletedTransaction);
    g_vm->DetachCurrentThread();
}

jobject txReceivedCallbackReceiver;
jmethodID txReceivedCallbackMethodId;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_Wallet_registerTransactionReceivedListenerJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jobject jCallbackObject) {
    txReceivedCallbackReceiver = jEnv->NewGlobalRef(jCallbackObject);
    jclass jClass = jEnv->GetObjectClass(jCallbackObject);
    if (jClass == nullptr) {
        return (jboolean) false;
    }
    txReceivedCallbackMethodId = jEnv->GetMethodID(jClass, "onTransactionReceived", "(J)V");
    if (txReceivedCallbackMethodId == nullptr) {
        return (jboolean) false;
    }
    auto *pWallet = (TariWallet *) jpWallet;
    return (jboolean) wallet_callback_register_received_transaction(pWallet, onTransactionReceived);
}

void onTransactionReceived(struct TariPendingInboundTransaction *pPendingInboundTransaction) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr) {
        return;
    }
    auto jpPendingInboundTransaction = (jlong) pPendingInboundTransaction;
    jniEnv->CallVoidMethod(
            txReceivedCallbackReceiver,
            txReceivedCallbackMethodId,
            jpPendingInboundTransaction);
    g_vm->DetachCurrentThread();
}

jobject txReplyReceivedCallbackReceiver;
jmethodID txReplyReceivedCallbackMethodId;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_Wallet_registerTransactionReplyReceivedListenerJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jobject jCallbackObject) {
    txReplyReceivedCallbackReceiver = jEnv->NewGlobalRef(jCallbackObject);
    jclass jClass = jEnv->GetObjectClass(jCallbackObject);
    if (jClass == nullptr) {
        return (jboolean) false;
    }
    txReplyReceivedCallbackMethodId = jEnv->GetMethodID(jClass, "onTransactionReplyReceived",
                                                        "(J)V");
    if (txReplyReceivedCallbackMethodId == nullptr) {
        return (jboolean) false;
    }
    auto *pWallet = (TariWallet *) jpWallet;
    return (jboolean) wallet_callback_register_received_transaction_reply(pWallet,
                                                                          onReceivedTransactionReply);
}

void onReceivedTransactionReply(struct TariCompletedTransaction *pCompletedTransaction) {
    auto *jniEnv = getJNIEnv();
    if (jniEnv == nullptr) {
        return;
    }
    auto jpCompletedTransaction = (jlong) pCompletedTransaction;
    jniEnv->CallVoidMethod(
            txReplyReceivedCallbackReceiver,
            txReplyReceivedCallbackMethodId,
            jpCompletedTransaction);
    g_vm->DetachCurrentThread();
}

//

//region Wallet Test Functions

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_TestWallet_testGenerateDataJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jstring jDatastorePath) {
    auto *pDatastorePath = (char *) jEnv->GetStringUTFChars(jDatastorePath, JNI_FALSE);
    bool success = wallet_test_generate_data((TariWallet *) jpWallet, pDatastorePath);
    jEnv->ReleaseStringUTFChars(jDatastorePath, pDatastorePath);
    return (jboolean) success;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_TestWallet_testTransactionBroadcastJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jlong jTxId) {
    auto *pWallet = (TariWallet *) jpWallet;
    auto *pTx = (TariPendingInboundTransaction *) jTxId;
    return (jboolean) wallet_test_transaction_broadcast(pWallet, pTx);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_TestWallet_testCompleteSentTransactionJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jlong jTxId) {
    auto *pWallet = (TariWallet *) jpWallet;
    auto *pTx = (TariPendingOutboundTransaction *) jTxId;
    return (jboolean) wallet_test_complete_sent_transaction(pWallet, pTx);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_TestWallet_testMinedJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet,
        jlong jTxId) {
    auto *pWallet = (TariWallet *) jpWallet;
    auto *pTx = (TariCompletedTransaction *) jTxId;
    return (jboolean) wallet_test_mined(pWallet, pTx);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tari_android_wallet_ffi_TestWallet_testReceiveTransactionJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet) {
    auto *pWallet = (TariWallet *) jpWallet;
    return (jboolean) wallet_test_receive_transaction(pWallet);
}

//endregion