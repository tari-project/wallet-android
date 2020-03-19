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
package com.tari.android.wallet.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import butterknife.ButterKnife
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.ui.fragment.debug.BaseNodeConfigFragment
import com.tari.android.wallet.ui.fragment.debug.DebugLogFragment
import com.tari.android.wallet.ui.fragment.onboarding.CreateWalletFragment
import com.tari.android.wallet.ui.fragment.onboarding.IntroductionFragment
import com.tari.android.wallet.ui.fragment.onboarding.LocalAuthFragment
import com.tari.android.wallet.ui.fragment.send.AddAmountFragment
import com.tari.android.wallet.ui.fragment.send.AddNoteAndSendFragment
import com.tari.android.wallet.ui.fragment.send.AddRecipientFragment
import com.tari.android.wallet.ui.fragment.send.SendTxSuccessfulFragment

/**
 * Base for all fragment classes.
 *
 * @author The Tari Development Team
 */
abstract class BaseFragment : Fragment() {

    abstract val contentViewId: Int

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // DI inject
        val component = (activity!!.application as TariWalletApplication).appComponent
        when (this) {
            is IntroductionFragment -> component.inject(this)
            is CreateWalletFragment -> component.inject(this)
            is AddRecipientFragment -> component.inject(this)
            is AddAmountFragment -> component.inject(this)
            is AddNoteAndSendFragment -> component.inject(this)
            is SendTxSuccessfulFragment -> component.inject(this)
            is LocalAuthFragment -> component.inject(this)
            is DebugLogFragment -> component.inject(this)
            is BaseNodeConfigFragment -> component.inject(this)
        }
        // bind views
        val view = inflater.inflate(contentViewId, container, false)
        ButterKnife.bind(this, view)
        return view
    }

}

