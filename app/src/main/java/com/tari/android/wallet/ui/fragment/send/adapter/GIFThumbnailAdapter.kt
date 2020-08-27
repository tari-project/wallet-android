package com.tari.android.wallet.ui.fragment.send.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.tari.android.wallet.R
import com.tari.android.wallet.extension.repopulate
import com.tari.android.wallet.ui.fragment.send.adapter.GIFThumbnailAdapter.GIFThumbnailViewHolder
import com.tari.android.wallet.ui.presentation.gif.GIF
import com.tari.android.wallet.ui.presentation.gif.Placeholder

class GIFThumbnailAdapter(
    private val glide: RequestManager,
    private val onShowMoreClick: () -> Unit,
    private val onGifClick: (GIF) -> Unit
) : RecyclerView.Adapter<GIFThumbnailViewHolder>() {

    private val gifs = mutableListOf<GIF>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GIFThumbnailViewHolder =
        if (viewType == VIEW_TYPE_MORE)
            parent.inflate(R.layout.holder_show_more_gifs)
                .run { ShowMoreViewHolder(this, onShowMoreClick) }
        else parent.inflate(R.layout.holder_thumbnail_gif)
            .run { GIFViewHolder(this, glide, onGifClick) }

    private fun ViewGroup.inflate(id: Int) = LayoutInflater.from(context)
        .inflate(id, this, false)

    override fun onBindViewHolder(holder: GIFThumbnailViewHolder, position: Int) =
        holder.bind(position, gifs)

    override fun getItemCount(): Int = if (gifs.size == 0) 0 else gifs.size + 1

    override fun getItemViewType(position: Int) =
        if (position == gifs.size) VIEW_TYPE_MORE else VIEW_TYPE_GIF

    fun repopulate(gifs: Iterable<GIF>) {
        this.gifs.repopulate(gifs)
        super.notifyDataSetChanged()
    }

    private companion object {
        private const val VIEW_TYPE_GIF = 0
        private const val VIEW_TYPE_MORE = 1
    }

    abstract class GIFThumbnailViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun bind(position: Int, gifs: List<GIF>)
    }

    class GIFViewHolder(
        view: View,
        private val glide: RequestManager,
        private val onGifClick: (GIF) -> Unit
    ) :
        GIFThumbnailViewHolder(view) {

        private val imageView = view.findViewById<ImageView>(R.id.image_view)

        override fun bind(position: Int, gifs: List<GIF>) {
            val gif = gifs[position]
            itemView.setOnClickListener { onGifClick(gif) }
            glide.asGif()
                .load(gif.uri)
                .placeholder(Placeholder.color(position).asDrawable())
                .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(24)))
                .transition(DrawableTransitionOptions.withCrossFade(250))
                .into(imageView)
        }
    }

    class ShowMoreViewHolder(view: View, onClick: () -> Unit) : GIFThumbnailViewHolder(view) {

        init {
            view.setOnClickListener { onClick() }
        }

        override fun bind(position: Int, gifs: List<GIF>) {

        }

    }

}
