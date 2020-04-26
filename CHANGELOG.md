# Tari Aurora Changelog
## [v0.1.8-jniLibs-0.9.0] - 2020-04-26
An issue with nodes not deduping messages properly has been fixed.
This is a network-breaking change. Older versions will no longer be able to
communicate with the network.

## [v0.1.7-jniLibs-0.8.0] - 2020-04-25
Upgrade jniLibs to v0.8.0.
The app is now in sync with v0.1.0; Rincewind release

## [0.1.6-jniLibs-0.7.4] - 2020-04-24
- Updated jniLibs to 0.7.4
- Give better user feedback on store and forwarded messages
- TThe transaction status page now gives more detail about what your transaction state is.
- Pull-to-refresh updates
- Updated app icons
- Various smaller UI fixes.

## [0.1.5-jniLibs-0.7.2] - 2020-04-21
- Send Tari Flow service layer and UI updates for store and forward support.
- More and improved tests
- Update jniLibs to 0.7.2

## [0.1.4-jniLibs-0.7.1] - 2020-04-19
- Updated FFI libraries with improved Store and forward handling

## [0.1.2-jniLibs-0.6.0] - 2020-04-15
- Add Store and forward functionality
- Update to jniLibs 0.6.0. This is a network-breaking change. Older wallets will no longer be able to communicate with the Tari network.

## [0.1.1-jniLibs-0.5.1] - 2020-04-03
- Fix frozen "Sending Tari" screen oon some transactions
- New Aurora logo

## [0.1.0-jniLibs-0.5.1] - 2020-04-03
- Sets base node to a random pick from a list of base nodes.
- Applies splash copy updates.

## [0.0.23-jniLibs-0.5.0] - 2020-04-01
- Update JNIlibs to 0.5.0

## [0.0.22-jniLibs-0.4.1] - 2020-03-31
* Emoji Id clipboard handling and visual improvements - [#242](https://github.com/tari-project/wallet-android/issues/242), [#206](https://github.com/tari-project/wallet-android/issues/206)
* UI tweaks - [#241](https://github.com/tari-project/wallet-android/issues/241), [#240](https://github.com/tari-project/wallet-android/issues/240)
* Log file mailing - [#206](https://github.com/tari-project/wallet-android/issues/206)

## [0.0.21-jniLibs-0.4.1] - 2020-03-29
- FFI fixes

## [v0.0.20-jniLibs-0.4.0] - 2020-03-29
- jniLibs updated to 0.4.0. This is a breaking change and old wallets won't be able to connect to
  the network.

## [0.0.19-jniLibs-0.3.5] - 2020-03-29
- [#71](https://github.com/tari-project/wallet-android/issues/71) - Transaction fee tooltip in transaction details and add amount screens.
- [#160](https://github.com/tari-project/wallet-android/issues/160) - Store icon & modal.
- [#161](https://github.com/tari-project/wallet-android/issues/161) - Pop store modal after first send.
- [#227](https://github.com/tari-project/wallet-android/issues/227) - Testnet UTXOs imported separately.
- [#231](https://github.com/tari-project/wallet-android/issues/231) - Improve numeric pad responsiveness.
- [#237](https://github.com/tari-project/wallet-android/issues/237) - Adaptive app icon.

## [0.0.18-jniLibs-0.3.4] - 2020-03-27
- [#159](https://github.com/tari-project/wallet-android/issues/159) - User agreement & privacy policy links.
- [#174](https://github.com/tari-project/wallet-android/issues/174) - Wallet setup interstitial @ home screen.
- [#176](https://github.com/tari-project/wallet-android/issues/176) - Final copy changes.
- [#179](https://github.com/tari-project/wallet-android/issues/179) - Removes RTL support.
- [#217](https://github.com/tari-project/wallet-android/issues/217) - A guessed solution for the Play Store showing in-app purchase support.
- [#219](https://github.com/tari-project/wallet-android/issues/219) - Self emoji ID paste warning text updated.
- [#222](https://github.com/tari-project/wallet-android/issues/222) - Sending screen background video added.
- [#224](https://github.com/tari-project/wallet-android/issues/224) - Wallet info screen made scrollable.
- [#226](https://github.com/tari-project/wallet-android/issues/226) - Changed the type of `fee` in pending outbound and completed transactions to `MicroTari` & fixed the casting bug.
- [#230](https://github.com/tari-project/wallet-android/issues/230) - Tari spinning logo freeze fixed.
- [#233](https://github.com/tari-project/wallet-android/issues/233) - Duplicate of [#226](https://github.com/tari-project/wallet-android/issues/226).

## [0.0.17-jniLibs-0.3.3] - 2020-03-25
- [#184](https://github.com/tari-project/wallet-android/issues/184) - Received tx callback crash fixed.
- A fix for a crash in the add recipient screen on older Android versions.
- [#194](https://github.com/tari-project/wallet-android/issues/194) - Turned off app backup.
- [#195](https://github.com/tari-project/wallet-android/issues/195) - Transaction details outgoing transaction "To" text fix.
- [#197](https://github.com/tari-project/wallet-android/issues/197) - "Copied" animation views for emoji ID views.
- [#198](https://github.com/tari-project/wallet-android/issues/198) - Outgoing transaction details now shows fee.
- [#199](https://github.com/tari-project/wallet-android/issues/199) - Updated profile icon.
- [#200](https://github.com/tari-project/wallet-android/issues/200) - Add note font vertical spacing fix.
- [#201](https://github.com/tari-project/wallet-android/issues/201) - Onboarding screen broken character.
- [#202](https://github.com/tari-project/wallet-android/issues/202) - Local auth prompt updates in onboarding auth screen.
- [#203](https://github.com/tari-project/wallet-android/issues/203) - Update message for self emoji ID input.
- [#205](https://github.com/tari-project/wallet-android/issues/205) - Broken UI on paste self emoji ID.
- [#208](https://github.com/tari-project/wallet-android/issues/208) - Check trimmed clipboard text for valid emoji ID @ add recipient.
- [#209](https://github.com/tari-project/wallet-android/issues/209) - Add recipient text input now closes paste-emoji-ID views.
- [#210](https://github.com/tari-project/wallet-android/issues/210) - Multiple faucet UTXOs.
