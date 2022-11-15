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
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    auto pByteVector = GetPointerField<ByteVector *>(jEnv, jByteVector);
    auto result = reinterpret_cast<jlong>(public_key_create(pByteVector, errorCodePointer));
    setErrorCode(jEnv, error, errorCode);
    SetPointerField(jEnv, jThis, result);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIPublicKey_jniFromHex(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jHexStr,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    const char *pStr = jEnv->GetStringUTFChars(jHexStr, JNI_FALSE);
    TariPublicKey *pPublicKey = public_key_from_hex(pStr, errorCodePointer);
    setErrorCode(jEnv, error, errorCode);
    jEnv->ReleaseStringUTFChars(jHexStr, pStr);
    SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(pPublicKey));
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIPublicKey_jniFromPrivateKey(
        JNIEnv *jEnv,
        jobject jThis,
        jobject jPrivateKey,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    auto pPrivateKey = GetPointerField<TariPrivateKey *>(jEnv, jPrivateKey);
    auto result = reinterpret_cast<jlong>(public_key_from_private_key(pPrivateKey, errorCodePointer));
    setErrorCode(jEnv, error, errorCode);
    SetPointerField(jEnv, jThis, result);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIPublicKey_jniFromEmojiId(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jpEmoji,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    const char *pStr = jEnv->GetStringUTFChars(jpEmoji, JNI_FALSE);
    auto result = reinterpret_cast<jlong>(emoji_id_to_public_key(pStr, errorCodePointer));
    jEnv->ReleaseStringUTFChars(jpEmoji, pStr);
    setErrorCode(jEnv, error, errorCode);
    SetPointerField(jEnv, jThis, result);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tari_android_wallet_ffi_FFIPublicKey_jniGetEmojiId(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    auto pPublicKey = GetPointerField<TariPublicKey *>(jEnv, jThis);
    const char *pEmoji = public_key_to_emoji_id(pPublicKey, errorCodePointer);
    setErrorCode(jEnv, error, errorCode);
    jstring result = jEnv->NewStringUTF(pEmoji);
    string_destroy(const_cast<char *>(pEmoji));
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIPublicKey_jniGetBytes(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    auto pPublicKey = GetPointerField<TariPublicKey *>(jEnv, jThis);
    auto result = reinterpret_cast<jlong>(public_key_get_bytes(pPublicKey, errorCodePointer));
    setErrorCode(jEnv, error, errorCode);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIPublicKey_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    public_key_destroy(GetPointerField<TariPublicKey *>(jEnv, jThis));
    SetNullPointerField(jEnv, jThis);
}
