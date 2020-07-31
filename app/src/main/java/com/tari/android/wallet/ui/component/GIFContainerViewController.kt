/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.ui.component

import android.graphics.drawable.Animatable
import android.view.ViewGroup
import com.facebook.imagepipeline.image.ImageInfo
import com.giphy.sdk.core.models.Media
import com.giphy.sdk.ui.views.GifView
import com.tari.android.wallet.databinding.GifContainerBinding
import com.tari.android.wallet.infrastructure.GiphyEcosystem
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.setHeight
import com.tari.android.wallet.ui.extension.setTopMargin
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.presentation.TxNote
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * Displays the transaction note GIF.
 *
 * @author The Tari Development Team
 */
internal class GIFContainerViewController(
    private val ui: GifContainerBinding,
    private val tx: Tx,
    private val gifContainerTopMargin: Int
) : GifView.GifCallback {

    companion object {
        private const val gifDownloadTimeoutSec = 5L
    }

    private var gifDownloadTimeoutTimer: Disposable? = null
    private var downloadIsInProgress = false

    init {
        ui.gifView.gifCallback = this
    }

    fun onRetryClick(action: () -> Unit) =
        ui.retryLoadingGifTextView.setOnClickListener { action() }

    fun detach() {
        disposeGIFDownloadTimeoutTimer()
        downloadIsInProgress = false
        ui.gifView.gifCallback = null
        ui.gifView.onPingbackGifLoadSuccess = null
    }

    fun displayGIF() {
        val note = TxNote.fromNote(tx.message)
        if (note.gifUrl == null) {
            ui.gifContainerView.gone()
            ui.gifStatusContainer.gone()
        } else {
            downloadMedia(note.gifId!!)
        }
    }

    fun displayGIFUsingCache() {
        val note = TxNote.fromNote(tx.message)
        if (note.gifUrl == null) {
            ui.gifContainerView.gone()
            ui.gifStatusContainer.gone()
        } else {
            val id = note.gifId!!
            val cachedMedia = GiphyEcosystem.getCachedMedia(id)
            if (cachedMedia == null) {
                downloadMedia(id) {
                    GiphyEcosystem.cacheMedia(id, ui.gifView.media!!)
                }
            } else {
                showCachedMedia(cachedMedia)
            }
        }
    }

    private fun downloadMedia(id: String, onDownloaded: (Media) -> Unit = {}) {
        if (downloadIsInProgress) return
        downloadIsInProgress = true
        ui.gifStatusContainer.visible()
        ui.loadingGifTextView.visible()
        ui.retryLoadingGifTextView.gone()
        ui.loadingGifProgressBar.visible()
        ui.gifContainerView.visible()
        ui.gifContainerView.setHeight(0)
        ui.gifContainerView.setTopMargin(0)
        ui.gifView.onPingbackGifLoadSuccess = {
            downloadIsInProgress = false
            disposeGIFDownloadTimeoutTimer()
            onDownloaded(ui.gifView.media!!)
            ui.gifStatusContainer.gone()
            ui.gifContainerView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            ui.gifContainerView.setTopMargin(gifContainerTopMargin)
        }
        ui.gifView.setMediaWithId(id)
        startGIFDownloadTimeoutTimer()
    }

    private fun showCachedMedia(cachedMedia: Media) {
        ui.gifContainerView.visible()
        ui.gifView.setMedia(cachedMedia)
        ui.gifStatusContainer.gone()
        ui.gifContainerView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        ui.gifContainerView.setTopMargin(gifContainerTopMargin)
    }

    override fun onFailure(throwable: Throwable?) {
        ui.gifStatusContainer.visible()
        ui.loadingGifProgressBar.gone()
        ui.loadingGifTextView.gone()
        ui.retryLoadingGifTextView.visible()
        downloadIsInProgress = false
        disposeGIFDownloadTimeoutTimer()
    }

    override fun onImageSet(
        imageInfo: ImageInfo?,
        anim: Animatable?,
        loopDuration: Long,
        loopCount: Int
    ) {
        // no-op
        // using onPingbackGifLoadSuccess for success case
    }

    private fun startGIFDownloadTimeoutTimer() {
        disposeGIFDownloadTimeoutTimer() // dispose existing timer
        gifDownloadTimeoutTimer =
            Observable
                .timer(gifDownloadTimeoutSec, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    onFailure(null)
                }
    }

    private fun disposeGIFDownloadTimeoutTimer() {
        gifDownloadTimeoutTimer?.dispose()
        gifDownloadTimeoutTimer = null
    }

}
