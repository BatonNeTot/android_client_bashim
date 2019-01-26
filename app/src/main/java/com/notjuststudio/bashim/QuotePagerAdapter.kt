package com.notjuststudio.bashim

import android.support.v4.view.PagerDateStrip
import android.view.View
import android.support.v4.view.ViewPager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.notjuststudio.bashim.common.Link
import com.notjuststudio.bashim.common.TitleType
import com.notjuststudio.bashim.custom.LinkPagerAdapter
import com.notjuststudio.bashim.custom.OnScrollEndListener
import com.notjuststudio.bashim.helper.*
import com.notjuststudio.bashim.helper.QuoteHelper.Companion.FIRST_MONTH
import com.notjuststudio.bashim.helper.QuoteHelper.Companion.FIRST_YEAR
import com.notjuststudio.bashim.loader.FavoriteQuotesLoader
import com.notjuststudio.bashim.loader.OfflineQuotesLoader
import com.notjuststudio.bashim.loader.RegularQuoteLoader
import com.notjuststudio.bashim.proto.BaseLoader
import kotlinx.android.synthetic.main.quotes_layout.view.*
import java.util.*
import javax.inject.Inject


class QuotePagerAdapter(link: Link,
                        private val pager: ViewPager,
                        private val tab: PagerDateStrip) : LinkPagerAdapter(link) {

    companion object {
        private const val LOAD_SCROLL_DISTANCE = 200
    }

    inner class ViewHolder(collection: ViewGroup, val link: Link, position: Int) {

        val recyclerAdapter: QuoteAdapter
        val container = inflaterHelper.inflate(R.layout.quotes_layout, collection)

        val loader: BaseLoader

        init {
            container.tag = position

            loader = when (link) {
                Link.RANDOM_OFFLINE -> {
                    OfflineQuotesLoader()
                }
                Link.FAVORITE -> {
                    FavoriteQuotesLoader(quoteHelper.getFavoriteOffset())
                }
                else -> {
                    val defData = quoteHelper.getDefData()
                    val nextData = quoteHelper.getNextData()

                    val loader = RegularQuoteLoader(link, if (defData.isNotEmpty())
                        defData
                    else
                        quoteHelper.calculateDate(link, referenceDate, position))

                    if (nextData.isNotEmpty())
                        loader.nextData = nextData

                    loader
                }
            }

            val savedQuotes = quoteHelper.restoreQuotes()
            recyclerAdapter = QuoteAdapter(link.type)
            recyclerAdapter.add(savedQuotes)
            container.recycler.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = recyclerAdapter
                addOnScrollListener(object : OnScrollEndListener() {
                    override fun onScrollEnd(recyclerView: RecyclerView) {
                        if (link.loadOnScroll && !container.refresher.isRefreshing) {
                            container.refresher.isRefreshing = true
                            loader.loadQuotes(onFirstLoaded = {
                                container.recycler.smoothScrollBy(0, LOAD_SCROLL_DISTANCE)
                            }, onLoaded = {
                                recyclerAdapter.add(it)
                            }, onDoneLoading = {
                                container.refresher.isRefreshing = false
                            }, onFailed = {
                                App.error(R.string.quotes_load_error)
                            })
                        }
                    }
                })
            }

            val onRefresh = {
                loader.reset()
                loader.loadQuotes(onFirstLoaded = {
                    recyclerAdapter.clear()
                }, onLoaded = {
                    recyclerAdapter.add(it)
                }, onDoneLoading = {
                    container.refresher.isRefreshing = false
                }, onFailed = {
                    App.error(R.string.quotes_load_error)
                }, onNothingToLoad = {
                    when (link) {
                        Link.FAVORITE -> {
                            App.error(R.string.quotes_favorite_error)
                        }
                        Link.RANDOM_OFFLINE -> {
                            App.error(R.string.quotes_offline_error)
                        }
                        else -> {}
                    }
                })
            }
            container.refresher.apply {
                setColorSchemeColors(resourceHelper.colorFromAttr(R.attr.refresherColor))
                setProgressBackgroundColorSchemeColor(resourceHelper.colorFromAttr(R.attr.refresherBackgroundColor))
                setOnRefreshListener(onRefresh)
            }

            if (savedQuotes.isEmpty()) {
                container.refresher.isRefreshing = true
                onRefresh.invoke()
            }
        }
    }

    private var referenceDate: Calendar

    fun getRefDate() : Calendar {
        return referenceDate
    }

    @Inject
    lateinit var resourceHelper: ResourceHelper

    @Inject
    lateinit var inflaterHelper: InflaterHelper

    @Inject
    lateinit var quoteHelper: QuoteHelper

    init {
        App.instance().component.inject(this)
        val date = quoteHelper.getDate()
        referenceDate =  date ?: Calendar.getInstance()

        tab.setOnTabListener{when(link.title) {
            TitleType.BEST_MONTH -> {
                val first = Calendar.getInstance()

                first.set(Calendar.YEAR, FIRST_YEAR)
                first.set(Calendar.MONTH, FIRST_MONTH)
                first.set(Calendar.DAY_OF_MONTH, 1)

                val current = Calendar.getInstance()
                current.time = referenceDate.time

                current.add(Calendar.MONTH, -pager.currentItem)

                quoteHelper.dateDialog(referenceDate, current, first, Calendar.MONTH) {
                    pager.setCurrentItem(it, true)
                }
            }
            TitleType.BEST_YEAR -> {
                val first = Calendar.getInstance()

                first.set(Calendar.YEAR, FIRST_YEAR)
                first.set(Calendar.MONTH, FIRST_MONTH)
                first.set(Calendar.DAY_OF_MONTH, 1)

                val current = Calendar.getInstance()
                current.time = referenceDate.time

                current.add(Calendar.YEAR, -pager.currentItem)

                quoteHelper.dateDialog(referenceDate, current, first, Calendar.YEAR) {
                    pager.setCurrentItem(it, true)
                }
            }
            TitleType.ABYSS_BEST -> {
                val first = Calendar.getInstance()
                first.time = referenceDate.time

                first.add(Calendar.DATE, -365)

                val current = Calendar.getInstance()
                current.time = referenceDate.time

                current.add(Calendar.DATE, -pager.currentItem)

                quoteHelper.dateDialog(referenceDate, current, first, Calendar.DAY_OF_MONTH) {
                    pager.setCurrentItem(it, true)
                }
            }
            else -> {}
        }}
    }

    private val holderMap: MutableMap<Int, ViewHolder> = mutableMapOf()

    fun getHolder(index: Int) : ViewHolder? {
        return holderMap[index]
    }

    override fun instantiateItem(collection: ViewGroup, position: Int): Any {
        val holder = ViewHolder(collection, link, position)
        (collection as ViewPager).addView(holder.container)
        holderMap.put(position, holder)
        return holder.container
    }

    override fun destroyItem(collection: ViewGroup, position: Int, view: Any) {
        (collection as ViewPager).removeView(view as View)
        holderMap.remove(position)?.loader?.cancel()
    }

    override fun getCount(): Int {
        when (link.title) {
            TitleType.BEST_MONTH -> { return (12 - FIRST_MONTH) + ((referenceDate.get(Calendar.YEAR) - FIRST_YEAR - 1) * 12) + referenceDate.get(Calendar.MONTH) + 1 }
            TitleType.BEST_YEAR -> { return referenceDate.get(Calendar.YEAR) - FIRST_YEAR + 1 }
            TitleType.ABYSS_BEST -> { return 366 }
            else -> { return 1 }
        }
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean = (view == `object`)

    override fun getPageTitle(position: Int): CharSequence? {
        when (link.title) {
            TitleType.BEST_MONTH -> {
                val month = referenceDate.get(Calendar.MONTH)
                val year = referenceDate.get(Calendar.YEAR) - (position + (11 - month)) / 12
                val index = mod(month - position, 12)
                if (year == referenceDate.get(Calendar.YEAR))
                    return resourceHelper.stringArray(R.array.date_month)[index]
                else
                    return "${resourceHelper.stringArray(R.array.date_month_short)[index]} ${year.toString().substring(2)}"
            }
            TitleType.BEST_YEAR -> {
                val year = referenceDate.get(Calendar.YEAR)
                return (year - position).toString()
            }
            TitleType.ABYSS_BEST -> {
                if (position == 0) {
                    return resourceHelper.string(R.string.date_today)
                } else {
                    val cal = Calendar.getInstance()
                    cal.time = referenceDate.time
                    cal.add(Calendar.DATE, -position)

                    val year = cal.get(Calendar.YEAR)
                    val month = cal.get(Calendar.MONTH)
                    val day = cal.get(Calendar.DAY_OF_MONTH)

                    if (year == referenceDate.get(Calendar.YEAR))
                        return "$day ${resourceHelper.stringArray(R.array.date_month_short)[month]}"
                    else
                        return "$day ${resourceHelper.stringArray(R.array.date_month_short)[month]} $year"
                }
            }
            else -> {return ""}
        }
    }

    private fun mod(x: Int, y: Int): Int {
        var result = (x).rem(y)
        if (result < 0) result += y
        return result
    }
}