package com.notjuststudio.bashim

import android.support.v7.widget.RecyclerView
import android.view.*
import com.notjuststudio.bashim.common.Quote
import com.notjuststudio.bashim.common.QuoteType
import com.notjuststudio.bashim.helper.*
import javax.inject.Inject


open class QuoteAdapter(val type: QuoteType) : RecyclerView.Adapter<QuoteAdapter.ViewHolder>() {

    private val dataset: MutableList<Quote> = mutableListOf()

    @Inject
    lateinit var quoteHelper: QuoteHelper

    init {
        App.instance().component.inject(this)
    }

    fun add(dataset: List<Quote>) {
        this.dataset.addAll(dataset)
        this.notifyItemRangeInserted(this.dataset.size - dataset.size, this.dataset.size)
    }

    fun get() : List<Quote> {
        return dataset.toList()
    }

    fun clear() {
        val size = this.dataset.size
        this.dataset.clear()
        this.notifyItemRangeRemoved(0, size)
    }

    open fun onFavorite(id: String){

    }

    open fun onUnfavorite(id: String) {

    }

    class ViewHolder(val container: View) : RecyclerView.ViewHolder(container)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // create a new view
        val container = LayoutInflater.from(parent.context)
                .inflate(R.layout.quote_item, parent, false)
        // set the view's size, margins, paddings and layout parameters
        return ViewHolder(container)
    }

    override fun onBindViewHolder(holder: ViewHolder, index: Int) {
        // - get element from your dataset at this index
        // - replace the contents of the view with that element
        val quote = dataset[index]

        quoteHelper.setupQuote(index, type, holder.container, quote, {onFavorite(it)}, {onUnfavorite(it)})

    }

    override fun getItemCount() = dataset.size

}