package com.tari.android.wallet.ui.common.recyclerView

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class CommonAdapter<T : CommonViewHolderItem>(): RecyclerView.Adapter<CommonViewHolder<T, ViewBinding>>() {
    private var items : MutableList<T> = mutableListOf()
    private var onClickListener : ItemClickListener<T>? = null

    abstract var viewHolderBuilders : List<ViewHolderBuilder>

    fun update(newItems : MutableList<T>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }

    fun setClickListener(onClickListener : ItemClickListener<T>?) {
        this.onClickListener = onClickListener
    }

    override fun onBindViewHolder(holder: CommonViewHolder<T, ViewBinding>, position: Int) = holder.bind(items[position])

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        val builder = viewHolderBuilders.firstOrNull { it.itemJavaClass == item.javaClass }

        builder ?: throw Exception("Нет такого билдера \nнужный: ${item.javaClass} \nposition: $position \n$viewHolderBuilders \n${item.javaClass}")

        return viewHolderBuilders.indexOf<ViewHolderBuilder?>(builder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommonViewHolder<T, ViewBinding> {

        val builder = viewHolderBuilders[viewType]

        val viewBinding = builder.createView.invoke(LayoutInflater.from(parent.context), parent, false)

        val generatedViewHolder = builder.createViewHolder.invoke(viewBinding)
        val viewHolder : CommonViewHolder<T, ViewBinding>? = generatedViewHolder as? CommonViewHolder<T, ViewBinding>

        viewHolder ?: throw Exception("Не тот тип вью холдера текущий: ${generatedViewHolder?.javaClass} нужный : ${viewHolder?.javaClass}")

        (viewHolder.clickView ?: viewHolder.itemView).setOnClickListener { v ->
            viewHolder.item?.let { onClickListener?.doOnClick?.invoke(it) }
        }
        return viewHolder
    }

    override fun getItemCount(): Int = items.size

    open class ItemClickListener<T>(val doOnClick : (T) -> Unit)
}