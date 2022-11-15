package com.tari.android.wallet.ui.fragment.send.addNote.gif

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.tari.android.wallet.ui.common.gyphy.placeholder.GifPlaceholder
import com.tari.android.wallet.ui.common.gyphy.repository.GIFItem
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.parcelable
import com.tari.android.wallet.ui.extension.visible

class GIFContainer(
    private val glide: RequestManager,
    private val gifContainerView: View,
    private val gifView: ImageView,
    thumbnailsContainer: View,
    state: Bundle?
) {

    private val transformation = RequestOptions().transform(RoundedCorners(10))
    private var animation = GIFsPanelAnimation(thumbnailsContainer)

    val isShown: Boolean
        get() = animation.isViewShown

    var gifItem: GIFItem? = null
        set(value) {
            field = value
            if (value == null) {
                glide.clear(gifView)
                showContainer()
            } else {
                showGIF()
                glide.asGif()
                    .placeholder(GifPlaceholder.color(value).asDrawable())
                    .apply(transformation)
                    .load(value.uri)
                    .transition(DrawableTransitionOptions.withCrossFade(250))
                    .into(gifView)
            }
        }

    init {
        gifItem = state?.parcelable(ThumbnailGIFsViewModel.KEY_GIF)
    }

    private fun showContainer() {
        animation.show()
        gifContainerView.gone()
    }

    private fun showGIF() {
        animation.hide()
        gifContainerView.visible()
    }

    fun save(bundle: Bundle) {
        gifItem?.run { bundle.putParcelable(ThumbnailGIFsViewModel.KEY_GIF, this) }
    }

    fun dispose() {
        animation.dispose()
    }

}