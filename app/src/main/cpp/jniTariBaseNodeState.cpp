#include <jni.h>
#include "jniCommon.cpp"
#include <wallet.h>


extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_tari_android_wallet_ffi_FFITariBaseNodeState_jniGetHeightOfLongestChain(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithError<jbyteArray>(jEnv, error, [&](int *errorPointer) {
        auto pTariBaseNodeState = GetPointerField<TariBaseNodeState *>(jEnv, jThis);
        return getBytesFromUnsignedLongLong(jEnv, basenode_state_get_height_of_the_longest_chain(pTariBaseNodeState, errorPointer));
    });
}


extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFITariBaseNodeState_jniGetNodeId(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    return ExecuteWithErrorAndCast<ByteVector *>(jEnv, error, [&](int *errorPointer) {
        auto pTariBaseNodeState = GetPointerField<TariBaseNodeState *>(jEnv, jThis);
        return basenode_state_get_node_id(pTariBaseNodeState, errorPointer);
    });
}