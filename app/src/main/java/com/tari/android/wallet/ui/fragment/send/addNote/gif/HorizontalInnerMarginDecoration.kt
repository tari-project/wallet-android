package com.tari.android.wallet.ui.fragment.send.addNote.gif

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class HorizontalInnerMarginDecoration(private val value: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.getChildLayoutPosition(view) > 0) outRect.left = value
    }
}