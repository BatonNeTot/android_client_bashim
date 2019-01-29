package com.notjuststudio.bashim.comics

import android.support.v4.view.PagerDateStrip
import android.support.v4.view.ViewPager
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.notjuststudio.bashim.App
import com.notjuststudio.bashim.R
import com.notjuststudio.bashim.common.Link
import com.notjuststudio.bashim.custom.LinkPagerAdapter
import com.notjuststudio.bashim.helper.ComicsHelper
import com.notjuststudio.bashim.helper.InflaterHelper
import com.notjuststudio.bashim.helper.ResourceHelper
import kotlinx.android.synthetic.main.quotes_layout.view.*
import javax.inject.Inject



class ComicsHeaderPagerAdapter(val pager: ViewPager) : LinkPagerAdapter(Link.COMICS) {

    @Inject
    lateinit var inflaterHelper: InflaterHelper

    @Inject
    lateinit var resourceHelper: ResourceHelper

    @Inject
    lateinit var comicsHelper: ComicsHelper

    inner class ViewHolder(collection: ViewGroup, yearIndex: Int) {

        val container: View

        init {
            container = inflaterHelper.inflate(R.layout.quotes_layout, collection)
            container.tag = yearIndex
            val recyclerAdapter = ComicsHeaderAdapter(yearIndex)
            container.recycler.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = recyclerAdapter
            }

            val load = {
                comicsHelper.loadYear(comicsHelper.getRef(yearIndex), onDone = {
                    container.refresher.isRefreshing = false
                    this@ComicsHeaderPagerAdapter.notifyDataSetChanged()
                    recyclerAdapter.update()
                }, onFail = {
                    container.refresher.isRefreshing = false
                    if (yearIndex == 0) {
                        this@ComicsHeaderPagerAdapter.notifyDataSetChanged()
                    }
                })
            }

            val onRefresh = {
                if (yearIndex == 0) {
                    comicsHelper.discardLoader()
                    this@ComicsHeaderPagerAdapter.notifyDataSetChanged()
                    this@ComicsHeaderPagerAdapter.hasToUpdate = true
                }
                load()
            }
            container.refresher.apply {
                setColorSchemeColors(resourceHelper.colorFromAttr(R.attr.refresherColor))
                setProgressBackgroundColorSchemeColor(resourceHelper.colorFromAttr(R.attr.refresherBackgroundColor))
                setOnRefreshListener(onRefresh)
            }

            container.refresher.isRefreshing = true
            load.invoke()
        }

    }


    init {
        App.instance().component.inject(this)
    }

    fun update(index: Int) {
        notifyDataSetChanged()

        val yearIndex = comicsHelper.getYearIndexByComicsIndex(index)
        pager.setCurrentItem(yearIndex, false)

        val container = pager.findViewWithTag<View?>(yearIndex)
        val monthIndex = comicsHelper.getMonthIndexByComicsIndex(index)

        container?.recycler?.scrollToPosition(monthIndex)
    }

    override fun instantiateItem(collection: ViewGroup, position: Int): Any {
        hasToUpdate = false
        val holder = ViewHolder(collection, position)
        (collection as ViewPager).addView(holder.container)
        return holder.container
    }

    override fun destroyItem(collection: ViewGroup, position: Int, view: Any) {
        (collection as ViewPager).removeView(view as View)
    }

    private var hasToUpdate = false

    override fun getItemPosition(`object`: Any): Int {
        return if (hasToUpdate) POSITION_NONE else POSITION_UNCHANGED
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean = (view == `object`)

    override fun getCount(): Int = comicsHelper.getRefsCount()

    override fun getPageTitle(position: Int): CharSequence? {
        return comicsHelper.getYearName(position)
    }
}