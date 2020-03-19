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
package com.tari.android.wallet.ui.activity.debug

import android.os.Bundle
import androidx.viewpager2.widget.ViewPager2
import butterknife.BindString
import butterknife.BindView
import butterknife.OnClick
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.activity.BaseActivity
import com.tari.android.wallet.ui.activity.debug.adapter.DebugViewPagerAdapter

/**
 * Contains debug fragments.
 *
 * @author The Tari Development Team
 */
internal class DebugActivity : BaseActivity() {

    @BindView(R.id.debug_tab_layout)
    lateinit var tabLayout: TabLayout
    @BindView(R.id.debug_view_pager)
    lateinit var viewPager: ViewPager2

    @BindString(R.string.debug_log_files_title)
    lateinit var logFilesTitle: String
    @BindString(R.string.debug_base_node_title)
    lateinit var baseNodeTitle: String

    private lateinit var pagerAdapter: DebugViewPagerAdapter

    override val contentViewId = R.layout.activity_debug

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pagerAdapter = DebugViewPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->

            tab.text = when (position) {
                0 -> logFilesTitle
                1 -> baseNodeTitle
                else -> throw RuntimeException("Unexpected position: $position")
            }
        }.attach()
    }

    @OnClick(R.id.debug_btn_back)
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }

}