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
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFITariWalletAddress_jniCreate(
        JNIEnv *jEnv,
        jobject jThis,
        jobject jByteVector,
        jobject error) {
    ExecuteWithError(jEnv, error, [&](int *errorPointer) {
        auto pByteVector = GetPointerField<ByteVector *>(jEnv, jByteVector);
        auto result = reinterpret_cast<jlong>(tari_address_create(pByteVector, errorPointer));
        SetPointerField(jEnv, jThis, result);
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFITariWalletAddress_jniFromBase58(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jBase58Str,
        jobject error) {
    ExecuteWithError(jEnv, error, [&](int *errorPointer) {
        const char *pBase58Str = jEnv->GetStringUTFChars(jBase58Str, JNI_FALSE);
        auto pTariWalletAddress = tari_address_from_base58(pBase58Str, errorPointer);
        jEnv->ReleaseStringUTFChars(jBase58Str, pBase58Str);
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(pTariWalletAddress));
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFITariWalletAddress_jniFromEmojiId(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jpEmoji,
        jobject error) {
    ExecuteWithError(jEnv, error, [&](int *errorPointer) {
        const char *pStr = jEnv->GetStringUTFChars(jpEmoji, JNI_FALSE);
        auto result = reinterpret_cast<jlong>(emoji_id_to_tari_address(pStr, errorPointer));
        jEnv->ReleaseStringUTFChars(jpEmoji, pStr);
        SetPointerField(jEnv, jThis, result);
    });
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tari_android_wallet_ffi_FFITariWalletAddress_jniGetEmojiId(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithError<jstring>(jEnv, error, [&](int *errorPointer) {
        auto pWalletAddress = GetPointerField<TariWalletAddress *>(jEnv, jThis);
        const char *pEmoji = tari_address_to_emoji_id(pWalletAddress, errorPointer);
        jstring result = jEnv->NewStringUTF(pEmoji);
        string_destroy(const_cast<char *>(pEmoji));
        return result;
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFITariWalletAddress_jniGetBytes(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithErrorAndCast<ByteVector *>(jEnv, error, [&](int *errorPointer) {
        auto pTariWalletAddress = GetPointerField<TariWalletAddress *>(jEnv, jThis);
        return tari_address_get_bytes(pTariWalletAddress, errorPointer);
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFITariWalletAddress_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    tari_address_destroy(GetPointerField<TariWalletAddress *>(jEnv, jThis));
    SetNullPointerField(jEnv, jThis);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_FFITariWalletAddress_jniGetNetwork(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithError<jint>(jEnv, error, [&](int *errorPointer) {
        auto pWalletAddress = GetPointerField<TariWalletAddress *>(jEnv, jThis);
        return static_cast<jint>(tari_address_network_u8(pWalletAddress, errorPointer));
    });
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_FFITariWalletAddress_jniGetFeatures(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithError<jint>(jEnv, error, [&](int *errorPointer) {
        auto pWalletAddress = GetPointerField<TariWalletAddress *>(jEnv, jThis);
        return static_cast<jint>(tari_address_features_u8(pWalletAddress, errorPointer));
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFITariWalletAddress_jniGetViewKey(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithErrorAndCast<TariPublicKey *>(jEnv, error, [&](int *errorPointer) {
        auto pWalletAddress = GetPointerField<TariWalletAddress *>(jEnv, jThis);
        return tari_address_view_key(pWalletAddress, errorPointer);
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFITariWalletAddress_jniGetSpendKey(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithErrorAndCast<TariPublicKey *>(jEnv, error, [&](int *errorPointer) {
        auto pWalletAddress = GetPointerField<TariWalletAddress *>(jEnv, jThis);
        return tari_address_spend_key(pWalletAddress, errorPointer);
    });
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_FFITariWalletAddress_jniGetChecksum(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithError<jint>(jEnv, error, [&](int *errorPointer) {
        auto pWalletAddress = GetPointerField<TariWalletAddress *>(jEnv, jThis);
        return static_cast<jint>(tari_address_checksum_u8(pWalletAddress, errorPointer));
    });
}