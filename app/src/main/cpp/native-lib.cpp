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

//region Byte Vector

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_ByteVector_byteVectorCreateJNI(
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
Java_com_tari_android_wallet_ffi_ByteVector_byteVectorGetLengthJNI(
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
Java_com_tari_android_wallet_ffi_ByteVector_byteVectorGetAtJNI(
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
Java_com_tari_android_wallet_ffi_ByteVector_byteVectorDestroyJNI(
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
Java_com_tari_android_wallet_ffi_PublicKey_publicKeyCreateJNI(
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
Java_com_tari_android_wallet_ffi_PublicKey_publicKeyFromHexJNI(
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
Java_com_tari_android_wallet_ffi_PublicKey_publicKeyFromPrivateKeyJNI(
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
Java_com_tari_android_wallet_ffi_PublicKey_publicKeyGetBytesJNI(
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
Java_com_tari_android_wallet_ffi_PublicKey_publicKeyDestroyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpPublicKey) {
    public_key_destroy((TariPublicKey *) jpPublicKey);
}

//endregion

//region Private Key

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_PrivateKey_privateKeyCreateJNI(
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
Java_com_tari_android_wallet_ffi_PrivateKey_privateKeyGenerateJNI(
        JNIEnv *jEnv,
        jclass jClass) {
    auto *pPrivateKey = private_key_generate();
    return (jlong) pPrivateKey;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_PrivateKey_privateKeyFromHexJNI(
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
Java_com_tari_android_wallet_ffi_PrivateKey_privateKeyGetBytesJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpPrivateKey) {
    auto *pPrivateKey = (TariPrivateKey *) jpPrivateKey;
    auto *pByteVector = private_key_get_bytes(pPrivateKey);
    return (jlong) pByteVector;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_PrivateKey_privateKeyDestroyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpPrivateKey) {
    private_key_destroy((TariPrivateKey *) jpPrivateKey);
}

//endregion

//region Contact

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Contact_contactCreateJNI(
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
Java_com_tari_android_wallet_ffi_Contact_contactGetAliasJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpContact) {
    auto *pContact = (TariContact *) jpContact;
    const auto *pAlias = contact_get_alias(pContact);
    return jEnv->NewStringUTF(pAlias);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Contact_contactGetPublicKeyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpContact) {
    auto *pContact = (TariContact *) jpContact;
    const auto *pPublicKey = contact_get_public_key(pContact);
    return (jlong) pPublicKey;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_Contact_contactDestroyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpContact) {
    contact_destroy((TariContact *) jpContact);
}

//endregion Contact

//region Contacts

extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_Contacts_contactsGetLengthJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpContacts) {
    auto *pContacts = (TariContacts *) jpContacts;
    return contacts_get_length(pContacts);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Contacts_contactsGetAtJNI(
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
Java_com_tari_android_wallet_ffi_Contacts_contactsDestroyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpContacts) {
    contacts_destroy((TariContacts *) jpContacts);
}

//endregion

//region CommsConfig

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_CommsConfig_commsConfigCreateJNI(
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
Java_com_tari_android_wallet_ffi_CommsConfig_commsConfigDestroyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpCommsConfig) {
    comms_config_destroy((TariCommsConfig *) jpCommsConfig);
}

//endregion

// region Wallet

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_Wallet_walletCreateJNI(
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
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_Wallet_walletDestroyJNI(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpWallet) {
    wallet_destroy((TariWallet *) jpWallet);
}

//endregion