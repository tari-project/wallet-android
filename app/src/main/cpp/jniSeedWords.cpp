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
Java_com_tari_android_wallet_ffi_FFISeedWords_jniCreate(
        JNIEnv *jEnv,
        jobject jThis) {
    TariSeedWords *pSeedWords = seed_words_create();
    SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(pSeedWords));
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFISeedWords_jniGetMnemonicWordListForLanguage(
        JNIEnv *jEnv,
        jobject jThis,
        jstring language,
        jobject error) {
    ExecuteWithError(jEnv, error, [&](int *errorPointer) {
        const char *pLanguage = jEnv->GetStringUTFChars(language, JNI_FALSE);
        TariSeedWords *pSeedWords = seed_words_get_mnemonic_word_list_for_language(pLanguage, errorPointer);
        SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(pSeedWords));
    });
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_FFISeedWords_jniPushWord(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jWord,
        jobject error) {
    return ExecuteWithError<jint>(jEnv, error, [&](int *errorPointer) {
        auto pSeedWords = GetPointerField<TariSeedWords *>(jEnv, jThis);
        const char *pWord = jEnv->GetStringUTFChars(jWord, JNI_FALSE);
        jint result = seed_words_push_word(pSeedWords, pWord, errorPointer);
        jEnv->ReleaseStringUTFChars(jWord, pWord);
        return result;
    });
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_tari_android_wallet_ffi_FFISeedWords_jniGetLength(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithError<jint>(jEnv, error, [&](int *errorPointer) {
        auto pSeedWords = GetPointerField<TariSeedWords *>(jEnv, jThis);
        return seed_words_get_length(pSeedWords, errorPointer);
    });
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tari_android_wallet_ffi_FFISeedWords_jniGetAt(
        JNIEnv *jEnv,
        jobject jThis,
        jint index,
        jobject error) {
    return ExecuteWithError<jstring>(jEnv, error, [&](int *errorPointer) {
        auto pSeedWords = GetPointerField<TariSeedWords *>(jEnv, jThis);
        const char *pWord = seed_words_get_at(pSeedWords, static_cast<unsigned int>(index), errorPointer);
        jstring result = jEnv->NewStringUTF(pWord);
        string_destroy(const_cast<char *>(pWord));
        return result;
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFISeedWords_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    seed_words_destroy(GetPointerField<TariSeedWords *>(jEnv, jThis));
    SetNullPointerField(jEnv, jThis);
}