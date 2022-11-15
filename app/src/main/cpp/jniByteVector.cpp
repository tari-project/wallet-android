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
Java_com_tari_android_wallet_ffi_FFIByteVector_jniCreate(
        JNIEnv *jEnv,
        jobject jThis,
        jbyteArray array,
        jobject error) {
    auto *buffer = reinterpret_cast<unsigned char *>(jEnv->GetByteArrayElements(array, JNI_FALSE));
    jsize size = jEnv->GetArrayLength(array);
    for (int i = 0; i < size; i++) {
        printf("%hhx", buffer[i]);
    }
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    ByteVector *pByteVector = byte_vector_create(buffer, static_cast<unsigned int>(size), errorCodePointer);
    setErrorCode(jEnv, error, errorCode);
    jEnv->ReleaseByteArrayElements(array, reinterpret_cast<jbyte *>(buffer), JNI_ABORT);
    SetNullPointerField(jEnv, jThis, pByteVector);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_FFIByteVector_jniGetLength(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    auto pByteVector = GetPointerField<ByteVector *>(jEnv, jThis);
    jint length = byte_vector_get_length(pByteVector, errorCodePointer);
    setErrorCode(jEnv, error, errorCode);
    return length;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_FFIByteVector_jniGetAt(
        JNIEnv *jEnv,
        jobject jThis,
        jint index,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    auto pByteVector = GetPointerField<ByteVector *>(jEnv, jThis);
    jint byte = byte_vector_get_at(pByteVector, static_cast<unsigned int>(index), errorCodePointer);
    setErrorCode(jEnv, error, errorCode);
    return byte;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIByteVector_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    byte_vector_destroy(GetPointerField<ByteVector *>(jEnv, jThis));
    SetNullPointerField(jEnv, jThis);
}
