# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)

set(libs_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../libwallet)

add_library(
        ssl_crypto
        STATIC
        IMPORTED
)
set_target_properties(
        ssl_crypto
        PROPERTIES
        IMPORTED_LOCATION ${libs_DIR}/${ANDROID_ABI}/libcrypto.a
)

add_library(
        ssl
        STATIC
        IMPORTED
)
set_target_properties(
        ssl
        PROPERTIES
        IMPORTED_LOCATION ${libs_DIR}/${ANDROID_ABI}/libssl.a
        IMPORTED_LINK_INTERFACE_LIBRARIES "ssl_crypto"
)

add_library(
        sqlite3
        STATIC
        IMPORTED
)
set_target_properties(
        sqlite3
        PROPERTIES
        IMPORTED_LOCATION ${libs_DIR}/${ANDROID_ABI}/libsqlite3.a
)

add_library(
        wallet
        STATIC
        IMPORTED
)
set_target_properties(
        wallet
        PROPERTIES
        IMPORTED_LOCATION ${libs_DIR}/${ANDROID_ABI}/libtari_wallet_ffi.a
        IMPORTED_LINK_INTERFACE_LIBRARIES "ssl;sqlite3"
)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

add_library(
        native-lib SHARED
        jniCommon.cpp
        jniByteVector.cpp
        jniTransportType.cpp
        jniPrivateKey.cpp
        jniPublicKey.cpp
        jniContact.cpp
        jniCommsConfig.cpp
        jniCompletedTransaction.cpp
        jniCompletedTransactionKernel.cpp
        jniPendingInboundTransaction.cpp
        jniPendingOutboundTransaction.cpp
        jniCollections.cpp
        jniWallet.cpp
        jniSeedWords.cpp
        jniEmojiSet.cpp
        jniUtil.cpp
)

find_library(
        log-lib
        log
)

target_include_directories(
        native-lib PRIVATE ${libs_DIR}/
)

target_link_libraries(
        native-lib
        android
        wallet
        ${log-lib}
        "-Wl,--allow-multiple-definition"
)
