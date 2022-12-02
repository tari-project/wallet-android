package com.tari.android.wallet.infrastructure.backup.dropbox

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2

object DropboxClientFactory {
    private var sDbxClient: DbxClientV2? = null
    fun init(accessToken: String?, config: DbxRequestConfig) {
        if (sDbxClient == null) {
            sDbxClient = DbxClientV2(config, accessToken)
        }
    }

    fun init(credential: DbxCredential, config: DbxRequestConfig) {
        val newCreds = DbxCredential(credential.accessToken, -1L, credential.refreshToken, credential.appKey)
        if (sDbxClient == null) {
            sDbxClient = DbxClientV2(config, newCreds)
        }
    }

    val client: DbxClientV2
        get() {
            checkNotNull(sDbxClient) { "Client not initialized." }
            return sDbxClient!!
        }
}