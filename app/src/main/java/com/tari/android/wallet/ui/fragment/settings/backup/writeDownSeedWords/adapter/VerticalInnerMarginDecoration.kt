package com.tari.android.wallet.ui.fragment.settings.backup.writeDownSeedWords.adapter

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class VerticalInnerMarginDecoration(private val value: Int, private val spans: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.getChildLayoutPosition(view) < spans) {
            outRect.top = value
        }
        outRect.bottom = value
    }
}