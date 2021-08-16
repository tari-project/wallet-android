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
Java_com_tari_android_wallet_ffi_FFICommsConfig_jniCreate(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jPublicAddress,
        jobject jTransport,
        jstring jDatabaseName,
        jstring jDatastorePath,
        jlong jDiscoveryTimeoutSec,
        jlong jSafDurationSec,
        jobject error) {
    const char *pControlServiceAddress = jEnv->GetStringUTFChars(
            jPublicAddress,
            JNI_FALSE);
    const char *pDatabaseName = jEnv->GetStringUTFChars(jDatabaseName, JNI_FALSE);
    const char *pDatastorePath = jEnv->GetStringUTFChars(jDatastorePath, JNI_FALSE);
    jlong lTransport = GetPointerField(jEnv, jTransport);
    auto *pTransport = reinterpret_cast<TariTransportType *>(lTransport);
    int i = 0;
    int *r = &i;
    if (jDiscoveryTimeoutSec < 0) {
        jDiscoveryTimeoutSec = abs(jDiscoveryTimeoutSec);
    }
    TariCommsConfig *pCommsConfig = comms_config_create(
            pControlServiceAddress,
            pTransport,
            pDatabaseName,
            pDatastorePath,
            static_cast<unsigned long long int>(jDiscoveryTimeoutSec),
            static_cast<unsigned long long int>(jSafDurationSec),
            r
    );
    jEnv->ReleaseStringUTFChars(jPublicAddress, pControlServiceAddress);
    jEnv->ReleaseStringUTFChars(jDatabaseName, pDatabaseName);
    jEnv->ReleaseStringUTFChars(jDatastorePath, pDatastorePath);
    setErrorCode(jEnv, error, i);
    SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(pCommsConfig));
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFICommsConfig_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    jlong lCommsConfig = GetPointerField(jEnv, jThis);
    comms_config_destroy(reinterpret_cast<TariCommsConfig *>(lCommsConfig));
    SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
}
