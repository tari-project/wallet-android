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
Java_com_tari_android_wallet_ffi_FFIOutputFeatures_jniCreate(
        JNIEnv *jEnv,
        jobject jThis,
        unsigned char version,
        unsigned char flags,
        unsigned long long maturity,
        unsigned char recovery_byte,
        jobject metadata,
        jobject unique_id,
        jobject parent_public_key,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;

    jlong lMetadata = GetPointerField(jEnv, metadata);
    auto *pMetadata = reinterpret_cast<ByteVector *>(lMetadata);

    jlong lUniqueId = GetPointerField(jEnv, unique_id);
    auto *pUniqueId = reinterpret_cast<ByteVector *>(lUniqueId);

    jlong lParentPublicKey = GetPointerField(jEnv, parent_public_key);
    auto *pPublicKey = reinterpret_cast<ByteVector *>(lParentPublicKey);

    TariOutputFeatures *pOutputFeatures = output_features_create_from_bytes(
            version,
            flags,
            maturity,
            recovery_byte,
            pMetadata,
            pUniqueId,
            pPublicKey,
            errorCodePointer);
    SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(pOutputFeatures));
    setErrorCode(jEnv, error, errorCode);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIOutputFeatures_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    jlong lCovenant = GetPointerField(jEnv, jThis);
    output_features_destroy(reinterpret_cast<TariOutputFeatures *>(lCovenant));
    SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
}
