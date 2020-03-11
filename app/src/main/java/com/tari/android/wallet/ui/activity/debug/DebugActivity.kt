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