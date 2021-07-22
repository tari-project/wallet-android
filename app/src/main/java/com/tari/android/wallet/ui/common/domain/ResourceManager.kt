package com.tari.android.wallet.ui.common.domain

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

class ResourceManager(val context: Context) {
    fun getString(@StringRes id: Int): String = context.getString(id)

    fun getString(@StringRes id: Int, vararg formatArgs: Any): String = context.getString(id, *formatArgs)

    fun getColor(@ColorRes id: Int): Int = ContextCompat.getColor(context, id)

    fun getDimenInPx(@DimenRes id: Int): Int = context.resources.getDimensionPixelSize(id)

    fun getDimen(@DimenRes id: Int): Float = context.resources.getDimension(id)

    fun getDrawable(@DrawableRes id: Int): Drawable? = ContextCompat.getDrawable(context, id)
}