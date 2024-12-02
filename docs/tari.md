[//]: # (TODO remove or update this file)

# Build system
Expect standart android system we have script `downloadLibwallet` which downloading ffi side from github release page to local. It will appear in `libwallet` folder. It downloading `wallet.h` and `libminotari_wallet_ffi.a` for each architecture.
If you want to test some local build you should turn off this download and put files manually

versionNumber is version of our app. It should be aligned with iOS version
buildNumber is build version, it should be incremented manually before the release
libwalletVersion is ffi version. It should be updated after ffi update and have release on tari github page





# FFI integration
FFI written in Rust and built for android for diffrent architectures. We supported only arm64-v8a and x86_64 which is actual devices. Emulator has a diffrent and we stopped maintain it during 2023 year.
FFI lib files should be placed in right folder with rigth names. Now it's
libwallet/wallet.h
libwallet/arm64-v8a/libminotari_wallet_ffi.a
libwallet/x86_64/libminotari_wallet_ffi.a
Cmake files which build it very strict about it
Also for right functuoning it should contains
* `libcrypto.a` - ssl
* libsqlite3.a - sqlLite
* libssl.a - ssl
If you need to update it then ssl located here:
https://github.com/217heidai/openssl_for_android/releases





# FFI Wrapper
We have 2 wrappers, in C++ and in kotlin
For C++ you should go to `\app\src\main\cpp`
It has CMake file which is responsible for C++ build. If you adding new cpp file you should include it to build list in this file
C++ files contains code for method injection from native code which descirbed in `wallet.h`
You should name methods like this
`Java_com_tari_android_wallet_ffi_FFIContacts_jniGetLength`
whele `Java_com_tari_android_wallet_ffi` is path, `FFIContacts` is kotlin wrapper file and `jniGetLength` experted method.
Inside methods you should use methods from `wallet.h`
jniCommon.cpp contains utils method for enligth work with C++

kotlin wrappers have `FFI` prefix. They should only contains logic related to wrapping entities and methods
`private external fun jniGetBalance(libError: FFIError): FFIPointer`
This is example of external method which connected to method in C++ wrapper
All FFI entities have pointer to C++ entity which is located in FFIBase



# Wallet service





# Yat integration
https://yat.fyi/ - sandbox
https://y.at/ - prod
https://github.com/yat-labs/yat-lib-android - yat lib
https://jitpack.io/#yat-labs/yat-lib-android - yat build
But last build was included manually because jitpack wont build for android 14. Needed to fix
In project for all interracition with Yat responsible `YatAdapter`
We have those features:
* Connecting existing Yat
* Showing connected Yat in wallet frament
* Searching wallet address by Yat via Yat lib 




# BLE
Common:
Checking permissions for bluetooth_ and locations
Checking wheither bluetooth is turned on

Then server:
* Searching for device with our service inside
* Needed RSSI < -55db, which is about 20cm
* Pair to this device
* Discovering our service in BLE protocol
* Searching for exact characteristics in that service. We have two for sharing and getting contact in BLE payment
* Then read or write by small chunks

Then client:
* Creating service + both characteristics + callbacks
* Laucnching advetiser
* Waiting for connection

For working with BLE we are using 
https://github.com/weliem/blessed-android
It close to native but easier to use





# Release policy




# Github policy