/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
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
package com.tari.android.wallet.ui.fragment.send.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tari.android.wallet.R
import com.tari.android.wallet.model.Contact
import com.tari.android.wallet.model.User
import com.tari.android.wallet.model.yat.EmojiSet
import com.tari.android.wallet.ui.extension.temporarilyDisableClick

/**
 * Recipient user list recycler view adapter.
 *
 * @author The Tari Development Team
 */
internal class RecipientListAdapter(
	private val yatEmojiSet: EmojiSet,
    private val listener: (User) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    enum class Mode {
        LIST,
        SEARCH
    }

    private val recentHeaderViewType = 0
    private val headerViewType = 1
    private val userViewType = 2

    // items (headers and contacts)
    private val items = ArrayList<Any>()
    // mode
    private lateinit var mode: Mode

    fun displayList(
        recentTxUsers: List<User>,
        allContacts: List<Contact>
    ) {
        mode = Mode.LIST
        items.clear()
        // recent tx contacts
        if (recentTxUsers.isNotEmpty()) {
            items.add(recentHeaderViewType)
            items.addAll(recentTxUsers)
        }
        // all contacts
        if (allContacts.isNotEmpty()) {
            items.add(headerViewType)
            items.addAll(allContacts)
        }
        notifyDataSetChanged()
    }

    fun displaySearchResult(searchResult: List<User>) {
        mode = Mode.SEARCH
        items.clear()
        items.addAll(searchResult)
        notifyDataSetChanged()
    }

    /**
     * Item count.
     */
    override fun getItemCount() = items.size

    /**
     * Defines the view type - header or transaction.
     */
    override fun getItemViewType(position: Int): Int {
        return when (mode) {
            Mode.LIST -> if (items[position] is Int) { // header
                items[position] as Int
            } else {
                userViewType
            }
            Mode.SEARCH -> userViewType
        }
    }

    /**
     * Create the view holder instance.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            recentHeaderViewType -> RecipientHeaderViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.add_recipient_list_header, parent, false),
                RecipientHeaderViewHolder.Type.RECENT_CONTACTS
            )
            headerViewType -> RecipientHeaderViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.add_recipient_list_header, parent, false),
                RecipientHeaderViewHolder.Type.MY_CONTACTS
            )
            userViewType -> RecipientViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.add_recipient_list_item, parent, false), yatEmojiSet)
            else -> throw IllegalArgumentException("Unexpected view type $viewType.")
        }
    }

    /**
     * Bind & display header or transaction.
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when {
            holder is RecipientViewHolder -> {
                val user = items[position] as User
                holder.bind(user)
                holder.itemView.setOnClickListener {
                    it.temporarilyDisableClick()
                    listener(user)
                }
            }
            getItemViewType(position) == headerViewType -> {
                (holder as RecipientHeaderViewHolder).bind(position)
            }
            getItemViewType(position) == recentHeaderViewType -> {
                (holder as RecipientHeaderViewHolder).bind(position)
            }
        }
    }

}