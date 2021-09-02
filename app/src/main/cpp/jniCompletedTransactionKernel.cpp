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
JNIEXPORT jstring JNICALL
Java_com_tari_android_wallet_ffi_FFICompletedTxKernel_jniGetExcess(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lKernel = GetPointerField(jEnv, jThis);
    auto *pKernel = reinterpret_cast<TariTransactionKernel *>(lKernel);
    const char *pStr = transaction_kernel_get_excess_hex(pKernel, r);
    setErrorCode(jEnv, error, i);
    jstring result = jEnv->NewStringUTF(pStr);
    string_destroy(const_cast<char *>(pStr));
    return result;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tari_android_wallet_ffi_FFICompletedTxKernel_jniGetExcessPublicNonce(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lKernel = GetPointerField(jEnv, jThis);
    auto *pKernel = reinterpret_cast<TariTransactionKernel *>(lKernel);
    const char *pStr = transaction_kernel_get_excess_public_nonce_hex(pKernel, r);
    setErrorCode(jEnv, error, i);
    jstring result = jEnv->NewStringUTF(pStr);
    string_destroy(const_cast<char *>(pStr));
    return result;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tari_android_wallet_ffi_FFICompletedTxKernel_jniGetExcessSignature(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int i = 0;
    int *r = &i;
    jlong lKernel = GetPointerField(jEnv, jThis);
    auto *pKernel = reinterpret_cast<TariTransactionKernel *>(lKernel);
    const char *pStr = transaction_kernel_get_excess_signature_hex(pKernel, r);
    setErrorCode(jEnv, error, i);
    jstring result = jEnv->NewStringUTF(pStr);
    string_destroy(const_cast<char *>(pStr));
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFICompletedTxKernel_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    jlong lKernel = GetPointerField(jEnv, jThis);
    auto *pKernel = reinterpret_cast<TariTransactionKernel *>(lKernel);
    transaction_kernel_destroy(pKernel);
    SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
}