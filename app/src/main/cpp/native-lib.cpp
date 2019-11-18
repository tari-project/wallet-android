#include <jni.h>
#include <android/log.h>
#include <wallet.h>
#include <string>
#include <math.h>

#include "../../../../jniLibs/wallet.h"
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

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tari_android_wallet_ui_activity_MainActivity_privateKeyStringJNI(
        JNIEnv *pEnv,
        jobject pThis) {
    TariPrivateKey *pk = private_key_generate();
    ByteVector *bytes = private_key_get_bytes(pk);
    unsigned int bytes_length = byte_vector_get_length(bytes);
    std::string s = std::string("");
    for (int i = 0; i <= bytes_length; i++) {
        unsigned int byte = byte_vector_get_at(bytes, i);
        s += static_cast<char>(trunc(byte / 2)); //should be converted to hex,
                                                 // div by 2 to ensure utf8 valid
    }

    byte_vector_destroy(bytes);
    private_key_destroy(pk);

    return pEnv->NewStringUTF(s.c_str());
}