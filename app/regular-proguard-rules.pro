# GSON-processed models
#-keep class com.tari.android.wallet.service.faucet.TestnetFaucetRESTGateway { *; }
#-keep class com.tari.android.wallet.service.faucet.TestnetTariAllocateMaxResponse { *; }
#-keep class com.tari.android.wallet.service.faucet.TestnetTariAllocateRequest { *; }
#-keep class com.tari.android.wallet.service.faucet.TestnetTariAllocateResponse { *; }
#-keep class com.tari.android.wallet.service.notification.PushNotificationRequestBody { *; }
#-keep class com.tari.android.wallet.service.notification.PushNotificationResponseBody { *; }
#-keep class com.tari.android.wallet.service.notification.PushNotificationRESTGateway { *; }

# Google Drive v3 SDK
-keep class * extends com.google.api.client.json.GenericJson { *; }
-keep class com.google.** { *; }
-keep class com.fasterxml.** { *; }
