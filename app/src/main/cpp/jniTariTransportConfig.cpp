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
Java_com_tari_android_wallet_ffi_FFITariTransportConfig_jniMemoryTransport(
        JNIEnv *jEnv,
        jobject jThis) {
    TariTransportConfig *pTransport = transport_memory_create();
    SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(pTransport));
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFITariTransportConfig_jniTCPTransport(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jpAddress,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;

    char *pAddress = const_cast<char *>(jEnv->GetStringUTFChars(jpAddress, JNI_FALSE));
    TariTransportConfig *pTransport = transport_tcp_create(pAddress, errorCodePointer);
    jEnv->ReleaseStringUTFChars(jpAddress, pAddress);
    setErrorCode(jEnv, error, errorCode);
    SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(pTransport));
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFITariTransportConfig_jniTorTransport(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jpControl,
        jobject jpTorCookie,
        jint jPort,
        jstring jpSocksUser,
        jstring jpSocksPass,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    char *pControl = const_cast<char *>(jEnv->GetStringUTFChars(jpControl, JNI_FALSE));
    auto pTorCookie = GetPointerField<ByteVector *>(jEnv, jpTorCookie);
    char *pSocksUsername = const_cast<char *>(jEnv->GetStringUTFChars(jpSocksUser, JNI_FALSE));
    char *pSocksPassword = const_cast<char *>(jEnv->GetStringUTFChars(jpSocksPass, JNI_FALSE));
    TariTransportConfig *transport = transport_tor_create(pControl, pTorCookie,
                                                          static_cast<unsigned short>(jPort),
                                                          false,
                                                          pSocksUsername, pSocksPassword, errorCodePointer);
    jEnv->ReleaseStringUTFChars(jpControl, pControl);
    jEnv->ReleaseStringUTFChars(jpSocksUser, pSocksUsername);
    jEnv->ReleaseStringUTFChars(jpSocksPass, pSocksPassword);
    setErrorCode(jEnv, error, errorCode);
    SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(transport));
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tari_android_wallet_ffi_FFITariTransportConfig_jniGetMemoryAddress(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    auto pTransport = GetPointerField<TariTransportConfig *>(jEnv, jThis);
    const char *pAddress = transport_memory_get_address(pTransport, errorCodePointer);
    setErrorCode(jEnv, error, errorCode);
    jstring result = jEnv->NewStringUTF(pAddress);
    string_destroy(const_cast<char *>(pAddress));
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFITariTransportConfig_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    transport_type_destroy(GetPointerField<TariTransportConfig *>(jEnv, jThis));
    SetNullPointerField(jEnv, jThis);
}
