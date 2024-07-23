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
Java_com_tari_android_wallet_ffi_FFIPublicKey_jniCreate(
        JNIEnv *jEnv,
        jobject jThis,
        jobject jByteVector,
        jobject error) {
    ExecuteWithError(jEnv, error, [&](int *errorPointer) {
        auto pByteVector = GetPointerField<ByteVector *>(jEnv, jByteVector);
        auto result = reinterpret_cast<jlong>(public_key_create(pByteVector, errorPointer));
        SetPointerField(jEnv, jThis, result);
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIPublicKey_jniFromHex(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jHexStr,
        jobject error) {
    ExecuteWithError(jEnv, error, [&](int *errorPointer) {
        const char *pStr = jEnv->GetStringUTFChars(jHexStr, JNI_FALSE);
        TariPublicKey *pPublicKey = public_key_from_hex(pStr, errorPointer);
        jEnv->ReleaseStringUTFChars(jHexStr, pStr);
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(pPublicKey));
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIPublicKey_jniFromPrivateKey(
        JNIEnv *jEnv,
        jobject jThis,
        jobject jPrivateKey,
        jobject error) {
    ExecuteWithError(jEnv, error, [&](int *errorPointer) {
        auto pPrivateKey = GetPointerField<TariPrivateKey *>(jEnv, jPrivateKey);
        auto result = reinterpret_cast<jlong>(public_key_from_private_key(pPrivateKey, errorPointer));
        SetPointerField(jEnv, jThis, result);
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIPublicKey_jniGetBytes(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithErrorAndCast<ByteVector *>(jEnv, error, [&](int *errorPointer) {
        auto pPublicKey = GetPointerField<TariPublicKey *>(jEnv, jThis);
        return public_key_get_bytes(pPublicKey, errorPointer);
    });
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tari_android_wallet_ffi_FFIPublicKey_jniGetEmojiId(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithError<jstring>(jEnv, error, [&](int *errorPointer) {
        auto pPublicKey = GetPointerField<TariPublicKey *>(jEnv, jThis);
        const char *pEmojiId = public_key_get_emoji_encoding(pPublicKey, errorPointer);
        jstring result = jEnv->NewStringUTF(pEmojiId);
        string_destroy(const_cast<char *>(pEmojiId));
        return result;
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIPublicKey_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    public_key_destroy(GetPointerField<TariPublicKey *>(jEnv, jThis));
    SetNullPointerField(jEnv, jThis);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_FFIPublicKeys_jniGetLength(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithError<int>(jEnv, error, [&](int *errorPointer) {
        auto pPublicKeys = GetPointerField<TariPublicKeys *>(jEnv, jThis);
        return public_keys_get_length(pPublicKeys, errorPointer);
    });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIPublicKeys_jniGetAt(
        JNIEnv *jEnv,
        jobject jThis,
        jint jIndex,
        jobject error) {
    return ExecuteWithErrorAndCast<TariPublicKey *>(jEnv, error, [&](int *errorPointer) {
        auto pPublicKeys = GetPointerField<TariPublicKeys *>(jEnv, jThis);
        return public_keys_get_at(pPublicKeys, static_cast<unsigned int>(jIndex), errorPointer);
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIPublicKeys_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    public_keys_destroy(GetPointerField<TariPublicKeys *>(jEnv, jThis));
    SetNullPointerField(jEnv, jThis);
}