package com.tari.android.wallet.ui.common.recyclerView

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.viewbinding.ViewBinding

abstract class CommonAdapter<T : CommonViewHolderItem> : ListAdapter<T, CommonViewHolder<T, ViewBinding>>(DefaultDiffCallback()) {
    private var onClickListener: ItemClickListener<T> = ItemClickListener()
    private var onLongClickListener: ItemLongClickListener<T> = ItemLongClickListener()

    abstract var viewHolderBuilders: List<ViewHolderBuilder>

    fun update(newItems: List<T>) {
        submitList(newItems.toList())
    }

    fun setClickListener(onClickListener: ItemClickListener<T>?) {
        this.onClickListener = onClickListener ?: ItemClickListener()
    }

    fun setClickListener(onClickListener: (T) -> Unit) {
        this.onClickListener = ItemClickListener(onClickListener)
    }

    fun setLongClickListener(onLongClickListener: ItemLongClickListener<T>?) {
        this.onLongClickListener = onLongClickListener ?: ItemLongClickListener()
    }

    override fun onBindViewHolder(holder: CommonViewHolder<T, ViewBinding>, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: CommonViewHolder<T, ViewBinding>, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        val builder = viewHolderBuilders.firstOrNull { it.itemJavaClass == item.javaClass || isFitSuperclass(item.javaClass, it.itemJavaClass) }

        builder ?: throw Exception("Invalid builder class \nrequired: ${item.javaClass} \nposition: $position \n$viewHolderBuilders \n${item.javaClass}")

        return viewHolderBuilders.indexOf<ViewHolderBuilder?>(builder)
    }

    private fun isFitSuperclass(startClass: Class<*>, superClass: Class<*>): Boolean {
        var currentClass: Class<*>? = startClass
        while (currentClass != null) {
            if (currentClass == superClass) {
                return true
            }
            currentClass = currentClass.superclass
        }
        return false
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommonViewHolder<T, ViewBinding> {

        val builder = viewHolderBuilders[viewType]

        val viewBinding = builder.createView.invoke(LayoutInflater.from(parent.context), parent, false)

        val generatedViewHolder = builder.createViewHolder.invoke(viewBinding)
        val viewHolder: CommonViewHolder<T, ViewBinding>? = generatedViewHolder as? CommonViewHolder<T, ViewBinding>

        viewHolder ?: throw Exception("Не тот тип вью холдера текущий: ${generatedViewHolder.javaClass} нужный : ${viewHolder?.javaClass}")

        (viewHolder.clickView ?: viewHolder.itemView).setOnClickListener {
            viewHolder.item?.let { onClickListener.doOnClick.invoke(it) }
        }
        (viewHolder.clickView ?: viewHolder.itemView).setOnLongClickListener {
            viewHolder.item?.let { onLongClickListener.doOnLongClick.invoke(it) } ?: false
        }
        return viewHolder
    }

    open class ItemClickListener<T>(val doOnClick: (T) -> Unit = {})

    open class ItemLongClickListener<T>(val doOnLongClick: (T) -> Boolean = { false })
}