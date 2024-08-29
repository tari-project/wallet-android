package com.tari.android.wallet.ui.common.gyphy.presentation

import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

class GlideGifListener(private val consumer: GifStateConsumer) : RequestListener<GifDrawable> {

    override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: Target<GifDrawable>,
        isFirstResource: Boolean
    ): Boolean {
        consumer.onGifErrorState()
        return true
    }

    override fun onResourceReady(
        resource: GifDrawable,
        model: Any,
        target: Target<GifDrawable>?,
        dataSource: DataSource,
        isFirstResource: Boolean
    ): Boolean {
        consumer.onGifResourceReady()
        return false
    }
}