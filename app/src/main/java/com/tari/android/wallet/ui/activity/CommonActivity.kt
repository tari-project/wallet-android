package com.tari.android.wallet.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tari.android.wallet.infrastructure.yat.adapter.YatAdapter
import com.tari.android.wallet.ui.extension.appComponent
import javax.inject.Inject

open class CommonActivity : AppCompatActivity() {

    @Inject
    lateinit var yatAdapter: YatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        yatAdapter.processDeeplink(this, intent)
    }
}