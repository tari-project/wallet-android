package com.tari.android.wallet.ui.screen.tx.details.gif

import androidx.lifecycle.LifecycleOwner
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ViewTxDetailsGifContainerBinding
import com.tari.android.wallet.ui.common.giphy.repository.GifItem
import com.tari.android.wallet.util.extension.dimenPx
import com.tari.android.wallet.util.extension.gone
import com.tari.android.wallet.util.extension.setTopMargin
import com.tari.android.wallet.util.extension.visible

class GifView(
    private val ui: ViewTxDetailsGifContainerBinding,
    private val glide: RequestManager,
    private val viewModel: GifViewModel,
    private val owner: LifecycleOwner
) {

    fun displayGif() {
        viewModel.gif.observe(owner) {
            when {
                it.isError -> onFailure()
                it.isSuccessful -> downloadMedia(it.gifItem!!)
                it.isProcessing -> showDownloadingState()
                else -> throw IllegalStateException()
            }
        }
        ui.retryLoadingGifTextView.setOnClickListener {
            val currentState = viewModel.currentState
            when {
                currentState.isError -> viewModel.onGIFFetchRequested()
                currentState.isSuccessful -> downloadMedia(currentState.gifItem!!)
                else -> throw IllegalStateException()
            }
        }
    }

    private fun showDownloadingState() {
        shrinkGIF()
        hideErrorUI()
        showDownloadingUI()
    }

    private fun showErrorUI() {
        ui.gifStatusContainer.visible()
        ui.retryLoadingGifTextView.visible()
    }

    private fun hideErrorUI() {
        ui.gifStatusContainer.gone()
        ui.retryLoadingGifTextView.gone()
    }

    private fun showDownloadingUI() {
        ui.gifStatusContainer.visible()
        ui.loadingGifTextView.visible()
        ui.loadingGifProgressBar.visible()
    }

    private fun hideDownloadingUI() {
        ui.gifStatusContainer.gone()
        ui.loadingGifTextView.gone()
        ui.loadingGifProgressBar.gone()
    }

    private fun downloadMedia(gifItem: GifItem) {
        glide.asGif()
            .load(gifItem.uri)
            .apply(RequestOptions().transform(RoundedCorners(10)))
            .listener(UIUpdateListener())
            .into(ui.gifContainerView)
    }

    private fun onFailure() {
        shrinkGIF()
        hideDownloadingUI()
        showErrorUI()
    }

    private fun shrinkGIF() {
        // Glide won't call the listener's methods if ImageView is gone
        ui.gifContainerView.setTopMargin(0)
    }

    private fun expandGIF() {
        val margin = ui.root.dimenPx(R.dimen.tx_list_item_gif_container_top_margin)
        ui.gifContainerView.setTopMargin(margin)
    }

    private inner class UIUpdateListener : RequestListener<GifDrawable> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<GifDrawable>,
            isFirstResource: Boolean
        ): Boolean {
            onFailure()
            return true
        }

        override fun onResourceReady(
            resource: GifDrawable,
            model: Any,
            target: Target<GifDrawable>?,
            dataSource: DataSource,
            isFirstResource: Boolean
        ): Boolean {
            hideDownloadingUI()
            hideErrorUI()
            expandGIF()
            return false
        }
    }

}