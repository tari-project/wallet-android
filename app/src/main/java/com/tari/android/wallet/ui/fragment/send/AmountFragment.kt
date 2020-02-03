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
package com.tari.android.wallet.ui.fragment.send

import android.os.Bundle
import android.view.View
import android.widget.TextView
import butterknife.BindView
import butterknife.OnClick
import com.tari.android.wallet.R
import com.tari.android.wallet.model.Contact
import com.tari.android.wallet.model.User
import com.tari.android.wallet.ui.fragment.BaseFragment
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.EmojiUtil

/**
 * Placeholder amount entry fragment.
 *
 * @author The Tari Development Team
 */
class AmountFragment : BaseFragment() {

    @BindView(R.id.amount_txt_title)
    lateinit var titleTextView: TextView

    override val contentViewId: Int = R.layout.fragment_amount

    companion object {

        fun newInstance(): AmountFragment {
            return AmountFragment()
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bundle = arguments
        if (bundle != null) {
            val emojiId = bundle.getString("emojiId")
            val user = bundle.getParcelable<User>("user")
            if (emojiId != null) {
                val shortEmojiId = emojiId.take(6) + "..." + emojiId.takeLast(6)
                titleTextView.text = shortEmojiId
            } else if (user != null) {
                if (user is Contact) {
                    titleTextView.text = user.alias
                } else {
                    val userEmojiId = EmojiUtil.getEmojiIdForPublicKeyHexString(user.publicKeyHexString)
                    val shortEmojiId = userEmojiId.take(6) + "..." + userEmojiId.takeLast(6)
                    titleTextView.text = shortEmojiId
                }
            }
        }
    }

    @OnClick(R.id.amount_btn_back)
    fun onBackButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        val mActivity = activity ?: return
        UiUtil.hideKeyboard(mActivity)
        mActivity.onBackPressed()
    }

}