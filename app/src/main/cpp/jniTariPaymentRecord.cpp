#include <jni.h>
#include <wallet.h>
#include "jniCommon.cpp"

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFITariPaymentRecord_jniLoadData(
        JNIEnv *jEnv,
        jobject jThis) {
    jclass dataClass = jEnv->GetObjectClass(jThis);
    auto outputs = GetPointerField<TariPaymentRecord *>(jEnv, jThis);

    // Create a Java byte array for the unsigned char[32] payment_reference.
    jbyteArray jPaymentReference = jEnv->NewByteArray(32);
    jEnv->SetByteArrayRegion(jPaymentReference, 0, 32, reinterpret_cast<jbyte *>(outputs->payment_reference));
    jfieldID paymentReferenceField = jEnv->GetFieldID(dataClass, "paymentReference", "[B");
    jEnv->SetObjectField(jThis, paymentReferenceField, jPaymentReference);

    jfieldID amountField = jEnv->GetFieldID(dataClass, "amount", "J");
    auto amount = (long) (outputs->amount);
    jEnv->SetLongField(jThis, amountField, amount);

    jfieldID blockHeightField = jEnv->GetFieldID(dataClass, "blockHeight", "J");
    auto blockHeight = (long) (outputs->block_height);
    jEnv->SetLongField(jThis, blockHeightField, blockHeight);

    jfieldID minedTimestampField = jEnv->GetFieldID(dataClass, "minedTimestamp", "J");
    auto minedTimestamp = (long) (outputs->mined_timestamp);
    jEnv->SetLongField(jThis, minedTimestampField, minedTimestamp);

    jfieldID directionField = jEnv->GetFieldID(dataClass, "direction", "I");
    auto statusValue = (jbyte) (outputs->direction);
    jEnv->SetIntField(jThis, directionField, statusValue);
}