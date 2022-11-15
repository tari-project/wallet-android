package com.tari.android.wallet.ui.fragment.send.makeTransaction

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
import com.tari.android.wallet.databinding.FragmentMakeTransactionBinding
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.extension.setOnThrottledClickListener
import com.tari.android.wallet.ui.fragment.send.addRecepient.AddRecipientFragment
import com.tari.android.wallet.ui.fragment.send.requestTari.RequestTariFragment

class MakeTransactionFragment : CommonFragment<FragmentMakeTransactionBinding, MakeTransactionViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentMakeTransactionBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: MakeTransactionViewModel by viewModels()
        bindViewModel(viewModel)

        setupUI()

        subscribeUI()
    }

    private fun subscribeUI() = Unit

    private fun setupUI() {
        ui.backButton.setOnThrottledClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        ui.viewPager.adapter = MakeTransactionAdapter(requireActivity())
        ui.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                ui.titleTextView.setText(
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

    private inner class MakeTransactionAdapter(fm: FragmentActivity) : FragmentStateAdapter(fm) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> AddRecipientFragment()
                else -> RequestTariFragment()
            }
        }
    }
}

