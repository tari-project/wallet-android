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
package com.tari.android.wallet.ui.activity.log

import android.os.Bundle
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.activity.BaseActivity
import com.tari.android.wallet.ui.fragment.log.DebugLogFilePickerFragment
import com.tari.android.wallet.ui.fragment.log.DebugLogFragment

/**
 * Debug screen activity.
 *
 * @author The Tari Development Team
 */
class DebugLogActivity : BaseActivity(), DebugLogFragment.DebugListener,
    DebugLogFilePickerFragment.Listener {

    override val contentViewId = R.layout.activity_debug_log

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.debug_frame_container,
                DebugLogFragment()
            ).commit()
    }

    override fun onFilePickerClick() {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.debug_frame_container,
                DebugLogFilePickerFragment()
            ).addToBackStack(null).commit()
    }

    override fun onLogFileSelected(position: Int) {
        supportFragmentManager.popBackStackImmediate()
        val fragment = supportFragmentManager.findFragmentById(R.id.debug_frame_container)
        if (fragment is DebugLogFragment) {
            fragment.refreshLogs(position)
        }
    }
}