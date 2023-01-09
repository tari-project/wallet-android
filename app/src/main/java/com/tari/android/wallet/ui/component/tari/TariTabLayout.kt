package com.tari.android.wallet.ui.component.tari

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.tabs.TabLayout

class TariTabLayout(context: Context, val attrs: AttributeSet) : TabLayout(context, attrs) {

    override fun addTab(tab: Tab) {
        super.addTab(tab)
        applyFont(tab)
    }

    override fun addTab(tab: Tab, position: Int) {
        super.addTab(tab, position)
        applyFont(tab)
    }

    override fun addTab(tab: Tab, setSelected: Boolean) {
        super.addTab(tab, setSelected)
        applyFont(tab)
    }

    override fun addTab(tab: Tab, position: Int, setSelected: Boolean) {
        super.addTab(tab, position, setSelected)
        applyFont(tab)
    }

    private fun applyFont(tab: Tab) {
        val font = TariFont.AVENIR_LT_STD_HEAVY.asTypeface(context)
        val mainView = getChildAt(0) as ViewGroup
        val tabView = mainView.getChildAt(tab.position) as ViewGroup
        val tabViewChild: View = tabView.getChildAt(1)
        (tabViewChild as TextView).setTypeface(font, Typeface.NORMAL)
    }
}