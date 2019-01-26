package com.notjuststudio.bashim.comics

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.notjuststudio.bashim.App
import com.notjuststudio.bashim.R
import com.notjuststudio.bashim.common.ComicsHeader
import com.notjuststudio.bashim.helper.*
import kotlinx.android.synthetic.main.comics_header_item.view.*
import kotlinx.android.synthetic.main.comics_month_header_item.view.*
import javax.inject.Inject

class ComicsHeaderAdapter(val yearIndex: Int) : RecyclerView.Adapter<ComicsHeaderAdapter.ViewHolder>() {

    @Inject
    lateinit var imageLoaderHelper: ImageLoaderHelper

    @Inject
    lateinit var comicsHelper: ComicsHelper

    @Inject
    lateinit var inflaterHelper: InflaterHelper

    @Inject
    lateinit var resourceHelper: ResourceHelper

    @Inject
    lateinit var quoteHelper: QuoteHelper

    init {
        App.instance().component.inject(this)
    }

    fun update() {
        this.notifyDataSetChanged()
    }

    class ViewHolder(val container: View) : RecyclerView.ViewHolder(container) {
        lateinit var headers: List<ComicsHeader>
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // create a new view
        val container = LayoutInflater.from(parent.context)
                .inflate(R.layout.comics_month_header_item, parent, false)
        // set the view's size, margins, paddings and layout parameters
        return ViewHolder(container)
    }

    private fun discard(container: View) {
        container.headers.removeAllViews()

        val count = resourceHelper.int(R.integer.comics_header_grid_count)
        for (i in 0 until count) {
            inflaterHelper.inflate(R.layout.common_grid_empty_item, container.headers, true)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.container.tag = position

        discard(holder.container)

        val month = comicsHelper.getMonth(yearIndex, position)
        holder.container.month.text = month.name

        for (comics in month.comics) {
            val comicsContainer = inflaterHelper.inflate(R.layout.comics_header_item, holder.container.headers, true)
            comicsContainer.header.setOnClickListener {
                quoteHelper.goToComics(comicsHelper.getIndex(comics))
            }

            imageLoaderHelper.loadImage(comics.header.url, {
                comicsContainer.header.setImageBitmap(it)
                comicsContainer.loader.visibility = View.GONE
            }, {
                comicsContainer.visibility = View.GONE
            })
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)

        discard(holder.container)
    }

    override fun getItemCount() = comicsHelper.getMonthCount(yearIndex)

}