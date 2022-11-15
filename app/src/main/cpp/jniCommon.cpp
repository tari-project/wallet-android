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
#include <string>
#include <cmath>
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

inline jlong GetPointerField(JNIEnv *jEnv, jobject jThis) {
    jclass cls = jEnv->GetObjectClass(jThis);
    jfieldID fid = jEnv->GetFieldID(cls, "pointer", "J");
    jlong lByteVector = jEnv->GetLongField(jThis, fid);
    return lByteVector;
}

template <typename T>
inline T GetPointerField(JNIEnv *jEnv, jobject jThis) {
    return reinterpret_cast<T>(GetPointerField(jEnv, jThis));
}

inline void SetPointerField(JNIEnv *jEnv, jobject jThis, jlong jPointer) {
    jclass cls = jEnv->GetObjectClass(jThis);
    jfieldID fid = jEnv->GetFieldID(cls, "pointer", "J");
    jEnv->SetLongField(jThis, fid, jPointer);
}

inline void SetNullPointerField(JNIEnv *jEnv, jobject jThis) {
    SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
}

template <typename T>
inline void SetNullPointerField(JNIEnv *jEnv, jobject jThis, T pointer) {
    SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(pointer));
}

// function included in multiple source files must be inline
inline jbyteArray getBytesFromUnsignedLongLong(JNIEnv *jEnv, unsigned long long value) {
    const size_t size = sizeof(unsigned long long int);
    jbyteArray result = jEnv->NewByteArray((jsize) size);
    if (result != nullptr) {
        jbyte *cBytes = jEnv->GetByteArrayElements(result, nullptr);
        if (cBytes != nullptr) {
            int i;
            for (i = (int) (size - 1); i >= 0; i--) {
                cBytes[i] = (jbyte) (value & 0xFF);
                value >>= 8;
            }
            jEnv->ReleaseByteArrayElements(result, cBytes, 0);
        }
    }
    return result;
}

inline jboolean setErrorCode(JNIEnv *jEnv, jobject error, jint value) {
    jclass errorClass = jEnv->GetObjectClass(error);
    if (errorClass == nullptr)
        return static_cast<jboolean>(false);
    jfieldID errorField = jEnv->GetFieldID(errorClass, "code", "I");
    if (errorField == nullptr)
        return static_cast<jboolean>(false);
    jEnv->SetIntField(error, errorField, value);
    return static_cast<jboolean>(true);
}

template <typename G>
inline G ExecuteWithError(JNIEnv *jEnv, jobject error, std::function<G(int*)> fun) {
    int errorCode = 0;
    G result = fun(&errorCode);
    setErrorCode(jEnv, error, errorCode);
    return result;
}

inline void ExecuteWithError(JNIEnv *jEnv, jobject error, std::function<void(int*)> fun) {
    int errorCode = 0;
    fun(&errorCode);
    setErrorCode(jEnv, error, errorCode);
}

template <typename G>
inline jlong ExecuteWithErrorAndCast(JNIEnv *jEnv, jobject error, std::function<G(int*)> fun) {
    G result = ExecuteWithError(jEnv, error, fun);
    return reinterpret_cast<jlong>(result);
}