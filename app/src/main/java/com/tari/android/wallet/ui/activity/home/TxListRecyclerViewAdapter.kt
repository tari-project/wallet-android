/**
 * Copyright 2019 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:

 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.

 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.

 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.ui.activity.home

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import butterknife.*
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.DummyTx
import org.joda.time.DateTime
import org.joda.time.LocalDate
import java.lang.ref.WeakReference

/**
 * Recycler view adapter.
 *
 * @author The Tari Development Team
 */
class TxListRecyclerViewAdapter(transactions: List<DummyTx>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val headerViewType = 0
    private val transactionViewType = 1

    // items (headers and txs)
    private val items: ArrayList<Any> = ArrayList()

    init {
        // sort array & prepare item list
        var currentDate: LocalDate? = null
        for (transaction in transactions.sortedByDescending { it.timestamp }) {
            val transactionDate = DateTime(transaction.timestamp * 1000L).toLocalDate()
            if (currentDate == null || !transactionDate.isEqual(currentDate)) {
                currentDate = transactionDate
                items.add(currentDate)
            }
            items.add(transaction)
        }
    }

    /**
     * Defines the view type - header or transaction.
     */
    override fun getItemViewType(position: Int): Int {
        return if (items[position] is DummyTx) {
            transactionViewType
        } else {
            headerViewType
        }
    }

    /**
     * Create the view holder instance.
     */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return if (viewType == headerViewType) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.home_tx_list_header, parent, false)
            HeaderViewHolder(
                view
            )
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.home_tx_list_item, parent, false)
            TransactionViewHolder(
                view
            )
        }
    }

    /**
     * Bind & display header or transaction.
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TransactionViewHolder) {
            holder.bind(items[position] as DummyTx)
        } else {
            (holder as HeaderViewHolder).bind(items[position] as LocalDate, position)
        }
    }

    /**
     * Item count.
     */
    override fun getItemCount() = items.size //transactions.size

    /**
     * Section header view holder.
     */
    class HeaderViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        private val dateFormat = "MMMM dd, yyyy"

        @BindView(R.id.home_tx_list_header_vw_separator)
        lateinit var separatorView: View
        @BindView(R.id.home_tx_list_header_txt_title)
        lateinit var titleTextView: TextView
        @BindString(R.string.home_today)
        lateinit var todayString: String
        @BindString(R.string.home_yesterday)
        lateinit var yesterdayString: String

        private lateinit var date: LocalDate

        init {
            ButterKnife.bind(this, view)
        }

        fun bind(date: LocalDate, position: Int) {
            if (position == 0) {
                separatorView.visibility = View.GONE
            } else {
                separatorView.visibility = View.VISIBLE
            }
            this.date = date
            val todayDate = LocalDate.now()
            val yesterdayDate = todayDate.minusDays(1)
            when {
                date.isEqual(todayDate) -> {
                    titleTextView.text = todayString
                }
                date.isEqual(yesterdayDate) -> {
                    titleTextView.text = yesterdayString
                }
                else -> {
                    titleTextView.text = date.toString(dateFormat)
                }
            }

        }
    }

    /**
     * Transaction view holder class.
     *
     * @author The Tari Development Team
     */
    class TransactionViewHolder(view: View) :
        RecyclerView.ViewHolder(view),
        View.OnClickListener {

        @BindView(R.id.home_tx_list_item_img_icon)
        lateinit var iconImageView: ImageView
        @BindView(R.id.home_tx_list_item_txt_contact_alias)
        lateinit var contactAliasTextView: TextView
        @BindView(R.id.home_tx_list_item_txt_message)
        lateinit var messageTextView: TextView
        @BindView(R.id.home_tx_list_item_txt_value)
        lateinit var valueTextView: TextView
        @BindDrawable(R.drawable.home_tx_value_positive_bg)
        lateinit var positiveBgDrawable: Drawable
        @BindColor(R.color.home_tx_value_positive)
        @JvmField
        var positiveColor: Int = 0
        @BindDrawable(R.drawable.home_tx_value_negative_bg)
        lateinit var negativeBgDrawable: Drawable
        @BindColor(R.color.home_tx_value_negative)
        @JvmField
        var negativeColor: Int = 0

        private lateinit var transactionWR: WeakReference<DummyTx>

        init {
            ButterKnife.bind(this, view)
        }

        @OnClick(R.id.home_tx_list_item_vw_root)
        override fun onClick(view: View) {
            UiUtil.temporarilyDisableClick(view)
            Logger.d("Tx clicked.")
        }

        fun bind(transaction: DummyTx) {
            transactionWR = WeakReference(transaction)
            // display contact alias
            contactAliasTextView.text = transaction.contactAlias
            // display message
            messageTextView.text = transaction.message
            // display value
            if (transaction.value > 0) {
                val formattedValue = "+%1$,.2f".format(transaction.value)
                valueTextView.text = formattedValue
                valueTextView.setTextColor(positiveColor)
                valueTextView.background = positiveBgDrawable
            } else {
                val formattedValue = "%1$,.2f".format(transaction.value)
                valueTextView.text = formattedValue
                valueTextView.setTextColor(negativeColor)
                valueTextView.background = negativeBgDrawable
            }
        }
    }

}