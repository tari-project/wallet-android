package com.tari.android.wallet.ui.screen.send.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.FragmentTransferBinding
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.screen.contactBook.add.SelectUserContactFragment
import com.tari.android.wallet.ui.screen.send.requestTari.RequestTariFragment
import com.tari.android.wallet.ui.screen.send.requestTari.RequestTariViewModel

class TransferFragment : CommonFragment<FragmentTransferBinding, RequestTariViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentTransferBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: RequestTariViewModel by viewModels()
        bindViewModel(viewModel)

        setupUI()
    }

    fun setupUI() {
        val viewPager = TransferPagerAdapter(requireActivity())
        ui.viewPager.adapter = viewPager

        ui.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                ui.toolbar.ui.toolbarTitle.setText(
                    when (position) {
                        0 -> R.string.send_tari_title
                        else -> R.string.request_tari_title
                    }
                )
            }
        })

        TabLayoutMediator(ui.viewPagerIndicators, ui.viewPager) { tab, position ->
            tab.setText(
                when (position) {
                    0 -> R.string.send_tari_subtitle
                    else -> R.string.request_tari_subtitle
                }
            )
        }.attach()
    }

    class TransferPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> SelectUserContactFragment.newInstance(false)
            1 -> RequestTariFragment.newInstance(false)
            else -> throw IllegalArgumentException("Invalid position $position")
        }

    }
}