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



extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_PrivateKey_jniCreate(
        JNIEnv *jEnv,
        jclass jClass,
        jlong jpByteVector,
        jobject error) {
    int i = 0;
    int* r = &i;
    ByteVector *pByteVector = reinterpret_cast<ByteVector *>(jpByteVector);
    jlong result = reinterpret_cast<jlong>(private_key_create(pByteVector,r));
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_PrivateKey_jniGenerate(
        JNIEnv *jEnv,
        jclass jClass) {
     return reinterpret_cast<jlong>(private_key_generate());
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_PrivateKey_jniFromHex(
        JNIEnv *jEnv,
        jclass jClass,
        jstring jHexStr,
        jobject error) {
    int i = 0;
    int* r = &i;
    const char *pStr = jEnv->GetStringUTFChars(jHexStr, JNI_FALSE);
    TariPrivateKey *pPrivateKey = private_key_from_hex(pStr,r);
    setErrorCode(jEnv,error,i);
    jEnv->ReleaseStringUTFChars(jHexStr, pStr);
    return reinterpret_cast<jlong>(pPrivateKey);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_PrivateKey_jniGetBytes(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpPrivateKey,
        jobject error) {
    int i = 0;
    int* r = &i;
    TariPrivateKey  *pPrivateKey = reinterpret_cast<TariPrivateKey *>(jpPrivateKey);
    jlong result = reinterpret_cast<jlong>(private_key_get_bytes(pPrivateKey,r));
    setErrorCode(jEnv,error,i);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_PrivateKey_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis,
        jlong jpPrivateKey) {
    private_key_destroy(reinterpret_cast<TariPrivateKey *>(jpPrivateKey));
}
