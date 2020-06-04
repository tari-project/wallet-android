# Tari Android mobile wallet

## Build instructions

This wallet requires the rust wallet library to be placed in the `jniLibs` folder
for the architecture you are targeting.

32-bit versions of the library are included in the git source tree, but they are
probably out of date and, well, for 32-bit systems.

To get 64-bit versions of the libraries, download the relevant archive (and hash checksum file) from
[the Tari website](https://tari.com/downloads), into the `jniLibs` folder
and extract them:

	.../jniLibs $ tar -xzvf arch.tar.gz

You should verify the binaries against their hashes:

	.../jniLibs $ sha256sum -c hashes.txt

	wallet.h: OK
        arm64-v8a/libsqlite3.a: OK
        arm64-v8a/libwallet_ffi.a: OK
        arm64-v8a/libzmq.a: OK
        armeabi-v7a/libsqlite3.a: OK
        armeabi-v7a/libwallet_ffi.a: OK
        armeabi-v7a/libzmq.a: OK
        x86/libsqlite3.a: OK
        x86/libwallet_ffi.a: OK
        x86/libzmq.a: OK
        x86_64/libsqlite3.a: OK
        x86_64/libwallet_ffi.a: OK
        x86_64/libzmq.a: OK

## Tracking private builds

To build and install a build that does not contain Matomo events tracking, Sentry crash reporting
and backend app interaction for iOS push notifications support and initial Tari enrollment, then,
assuming that:
  - the terminal's current working directory is the project's root;
  - at least Java 8 is installed on the build machine;
  - Tari native libraries are present in the working directory. Run 
  `./gradlew -PbinariesVersion=?.?.? jniBinaries` with the latest version provided instead of 
  `?.?.?` (it is available on [this page](https://www.tari.com/downloads/)) to fetch them;
  - `Install via USB` option is being enabled through the device's `Developer options` settings (you 
  can consult your vendor's documentation or just simply search for it with any search engine);
  - your phone is connected to the build machine via USB;
  - Follow instructions inside `app/build.gradle` file marked with `PRIVATE BUILD` labels;
  
type the following: `./gradlew installPrivacyRelease`. A release private build should be installed 
to your device afterwards.
