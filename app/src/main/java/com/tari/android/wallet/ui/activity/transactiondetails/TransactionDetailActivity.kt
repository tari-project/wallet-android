package com.tari.android.wallet.ui.activity.transactiondetails

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.tari.android.wallet.R
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.ui.activity.BaseActivity

class TransactionDetailActivity : BaseActivity() {

    override val contentViewId: Int
        get() = R.layout.activity_transaction_detail

    companion object {
        val TRANSACTION_EXTRA_KEY = "TRANSACTION_EXTRA_KEY"

        fun createIntent(context: Context, transaction: Tx): Intent{
            return Intent(context, TransactionDetailActivity::class.java)
                .apply {
                    putExtra(TRANSACTION_EXTRA_KEY, transaction)
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}
