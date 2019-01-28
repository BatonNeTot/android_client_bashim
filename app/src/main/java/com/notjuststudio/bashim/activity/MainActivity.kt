package com.notjuststudio.bashim.activity

import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.main_activity.*
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.view.*
import android.content.Intent
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import com.notjuststudio.bashim.*
import com.notjuststudio.bashim.comics.ComicsHeaderPagerAdapter
import com.notjuststudio.bashim.common.Link
import com.notjuststudio.bashim.common.TitleType
import com.notjuststudio.bashim.custom.LinkPagerAdapter
import com.notjuststudio.bashim.helper.ResourceHelper
import javax.inject.Inject
import android.support.v7.widget.RecyclerView
import com.notjuststudio.bashim.helper.QuoteHelper
import com.notjuststudio.bashim.proto.BaseActivity
import android.os.Build
import com.notjuststudio.bashim.helper.QuoteHelper.Companion.FIRST_MONTH
import com.notjuststudio.bashim.helper.QuoteHelper.Companion.FIRST_YEAR
import com.notjuststudio.bashim.loader.FavoriteQuotesLoader
import com.notjuststudio.bashim.loader.RegularQuoteLoader
import java.lang.Math.max
import java.lang.Math.min
import java.util.*


class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object {

        const val ACTION_LINK = "link"

        const val SETTINGS = 0
        const val COMICS = 1

    }

    @Inject
    lateinit var resourceHelper: ResourceHelper

    @Inject
    lateinit var quoteHelper: QuoteHelper

    private fun link() : Link {
        return (viewPager.adapter as? LinkPagerAdapter)?.link ?: Link.NONE
    }

    private fun recycler() : RecyclerView? {
        val container = viewPager.findViewWithTag<View?>(viewPager.currentItem)
        val result = container?.findViewById<RecyclerView?>(R.id.recycler)
        return result
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        app.component.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = if (sharedPrefHelper.isDarkTheme())
                window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            else
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

            window.statusBarColor = if (sharedPrefHelper.isDarkTheme())
                resourceHelper.color(R.color.barDark)
            else
                resourceHelper.color(R.color.barLight)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = if (sharedPrefHelper.isDarkTheme())
                resourceHelper.color(R.color.darkBarDark)
            else
                resourceHelper.color(R.color.darkBarLight)
        }

        setSupportActionBar(toolbar)
        toolbar.setOnClickListener {
            recycler()?.scrollToPosition(0)
        }

        if (!resourceHelper.bool(R.bool.is_drawer_fixed)) {
            val toggle = ActionBarDrawerToggle(
                    this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
            )
            drawerLayout.addDrawerListener(toggle)
            toggle.syncState()
        }

        navigationView.setNavigationItemSelectedListener(this)
        navigationView.menu.apply {
            findItem(R.id.navRandomTitle).isCheckable = false
            findItem(R.id.navBestTitle).isCheckable = false
            findItem(R.id.navAbyssTitle).isCheckable = false

            findItem(R.id.navRandomOnline).isVisible = false
            findItem(R.id.navRandomOffline).isVisible = false

            findItem(R.id.navBestToday).isVisible = false
            findItem(R.id.navBestMonth).isVisible = false
            findItem(R.id.navBestYear).isVisible = false
            findItem(R.id.navBestAll).isVisible = false

            findItem(R.id.navAbyssNew).isVisible = false
            findItem(R.id.navAbyssTop).isVisible = false
            findItem(R.id.navAbyssBest).isVisible = false
        }

        viewPager.offscreenPageLimit = 2
        pagerDateStrip.visibility = View.GONE
//        pagerDateStrip.drawFullUnderline = false

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (!resourceHelper.bool(R.bool.is_drawer_fixed))
            drawerLayout.closeDrawer(GravityCompat.START)

        addSheludePost {
            quoteHelper.restoreQuoteDialog(this)
        }

        //TODO доделать, связаться с bash.im
        var link = sharedPrefHelper.loadFavorite()

        if (intent != null) {
            val data = intent.data
            if (intent.action == Intent.ACTION_VIEW && data != null) {
                Log.i("INTENT", "Data = ${data}")
                val path = data.path ?: ""
                Log.i("INTENT", "Path = ${path}")
                val pathSegments = data.pathSegments ?: listOf()
                Log.i("INTENT", "Segments = ${data.pathSegments}")

                if (pathSegments.size == 0) {
                    quoteHelper.discardCache()
                    link = Link.NEW
                } else {
                    when(pathSegments[0]) {
                        "quote" -> {
                            if (pathSegments.size >= 2) {
                                addSheludePost {
                                    quoteHelper.goToQuote(pathSegments[1])
                                }
                            } else {
                                quoteHelper.discardCache()
                                link = Link.NEW
                            }
                        }
                        "index" -> {
                            quoteHelper.discardCache()
                            link = Link.NEW

                            if (pathSegments.size >= 2) {
                                val index = max(1, pathSegments[1].toInt())
                                quoteHelper.setupLoaderData(index.toString())
                                quoteHelper.setupTitleName(resourceHelper.string(R.string.link_new_page, index))
                            }
                        }
                        "random" -> {
                            quoteHelper.discardCache()
                            link = Link.RANDOM_ONLINE
                        }
                        "best" -> {
                            quoteHelper.discardCache()
                            link = Link.BEST_TODAY
                        }
                        "bestmonth" -> {
                            quoteHelper.discardCache()
                            link = Link.BEST_MONTH

                            val date = Calendar.getInstance()

                            val pathYear = if (pathSegments.size >= 2) pathSegments[1].toInt() else 0
                            val pathMonth = if (pathSegments.size >= 3) pathSegments[2].toInt() - 1 else 0

                            val year = max(FIRST_YEAR, min(date.get(Calendar.YEAR), pathYear))
                            val month = when {
                                year == date.get(Calendar.YEAR) -> {
                                    max(Calendar.JANUARY, min(date.get(Calendar.MONTH), pathMonth))
                                }
                                year == FIRST_YEAR -> {
                                    max(FIRST_MONTH, min(Calendar.DECEMBER, pathMonth))
                                }
                                else -> {
                                    max(Calendar.JANUARY, min(Calendar.DECEMBER, pathMonth))
                                }
                            }

                            quoteHelper.setupDate(date)
                            quoteHelper.setupPosition(Link.NONE, quoteHelper.calculatePosition(date, year, month),
                                    -1, -1)
                        }
                        "bestyear" -> {
                            quoteHelper.discardCache()
                            link = Link.BEST_YEAR

                            val date = Calendar.getInstance()

                            val pathYear = if (pathSegments.size >= 2) pathSegments[1].toInt() else 0
                            val year = max(FIRST_YEAR, min(date.get(Calendar.YEAR), pathYear))

                            quoteHelper.setupDate(date)
                            quoteHelper.setupPosition(Link.NONE, quoteHelper.calculatePosition(date, year),
                                    -1, -1)
                        }
                        "byrating" -> {
                            quoteHelper.discardCache()
                            link = Link.BEST_ALL

                            val pathIndex = if (pathSegments.size >= 2) pathSegments[1].toInt() else 0
                            val index = max(1, pathIndex).toString()

                            quoteHelper.setupLoaderData(index)
                            quoteHelper.setupTitleName(resourceHelper.string(R.string.link_best_all_page, index))
                        }
                        "abyss" -> {
                            quoteHelper.discardCache()
                            link = Link.ABYSS_NEW
                        }
                        "abysstop" -> {
                            quoteHelper.discardCache()
                            link = Link.ABYSS_TOP
                        }
                        "abyssbest" -> {
                            quoteHelper.discardCache()
                            link = Link.ABYSS_BEST

                            val date = Calendar.getInstance()
                            quoteHelper.setupDate(date)

                            val pathDate = if (pathSegments.size >= 2 && pathSegments[1].length == 8 && pathSegments[1].toIntOrNull() != null) pathSegments[1] else null

                            if (pathDate != null) {
                                val pathYear = pathDate.subSequence(0, 4).toString().toInt()
                                val pathMonth = pathDate.subSequence(4, 6).toString().toInt() - 1
                                val pathDay = pathDate.subSequence(6, 8).toString().toInt()

                                val year = if (date.get(Calendar.YEAR) >= pathYear) pathYear else null
                                val month = if (date.get(Calendar.YEAR) > pathYear

                                        || date.get(Calendar.YEAR) == pathYear
                                        && date.get(Calendar.MONTH) >= pathMonth) pathMonth else null
                                val day = if (date.get(Calendar.YEAR) > pathYear

                                        || date.get(Calendar.YEAR) == pathYear
                                        && date.get(Calendar.MONTH) > pathMonth

                                        || date.get(Calendar.YEAR) == pathYear
                                        && date.get(Calendar.MONTH) == pathMonth
                                        && date.get(Calendar.DAY_OF_MONTH) >= pathDay) pathDay else null

                                Log.i("Path", "$year $month $day")

                                if (year != null && month != null && day != null) {
                                    val index = quoteHelper.calculatePosition(date, year, month, day)
                                    if (index in 0..365)
                                        quoteHelper.setupPosition(Link.NONE, index, -1, -1)
                                }
                            }
                        }
                        else -> {
                            quoteHelper.discardCache()
                            link = Link.NEW
                        }
                    }
                }
            } else if (intent.action == ACTION_LINK) {
                quoteHelper.discardCache()
                link = Link.fromInt((intent.extras?.get(MainActivity.ACTION_LINK) as Int?) ?: -1)
            } else {
                val savedLink = quoteHelper.getLink()
                if (savedLink != Link.NONE)
                    link = savedLink
            }
        }

        loadQuotes(link)
    }

    private fun loadOtherQuotes(link: Link) {
        quoteHelper.discardCache()
        loadQuotes(link)
    }

    private fun loadQuotes(link: Link) {
        setupQuoteAdapter(link)

        val titleName = quoteHelper.getTitleName()
        supportActionBar?.title = if (titleName.isNotEmpty())
            titleName
        else
            resources.getStringArray(R.array.preferred_links)[link.id]

        setupDrawer(link)

        addSheludePost {
            val savedPos = quoteHelper.getPagerPos()
            val savedOffset = quoteHelper.getPagerOffset()

            (recycler()?.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(savedPos, -savedOffset)
        }
    }

    private fun setupQuoteAdapter(link: Link) {
        pagerDateStrip.visibility = if (link.title != TitleType.NONE) View.VISIBLE else View.GONE

        viewPager.adapter = when(link) {
            Link.COMICS -> ComicsHeaderPagerAdapter(viewPager)
            else -> QuotePagerAdapter(link, viewPager, pagerDateStrip)
        }

        val pagerIndex = quoteHelper.getPagerIndex()
        if (pagerIndex != -1) {
            viewPager.currentItem = pagerIndex
        }
    }

    private fun setupDrawer(link: Link) {
        for (notLink in Link.values().filterNot{it == link}) {

            navigationView.menu.findItem(when (notLink) {

                Link.RANDOM_ONLINE -> R.id.navRandomOnline
                Link.RANDOM_OFFLINE -> R.id.navRandomOffline

                Link.BEST_TODAY -> R.id.navBestToday
                Link.BEST_MONTH -> R.id.navBestMonth
                Link.BEST_YEAR -> R.id.navBestYear
                Link.BEST_ALL -> R.id.navBestAll

                Link.ABYSS_NEW -> R.id.navAbyssNew
                Link.ABYSS_TOP -> R.id.navAbyssTop
                Link.ABYSS_BEST -> R.id.navAbyssBest

                Link.FAVORITE -> R.id.navFavorite
                Link.COMICS -> R.id.navComics

                else -> R.id.navNew
            })?.isChecked = false

        }

        navigationView.menu.findItem(when(link) {

            Link.RANDOM_ONLINE -> R.id.navRandomOnline
            Link.RANDOM_OFFLINE -> R.id.navRandomOffline

            Link.BEST_TODAY -> R.id.navBestToday
            Link.BEST_MONTH -> R.id.navBestMonth
            Link.BEST_YEAR -> R.id.navBestYear
            Link.BEST_ALL -> R.id.navBestAll

            Link.ABYSS_NEW -> R.id.navAbyssNew
            Link.ABYSS_TOP -> R.id.navAbyssTop
            Link.ABYSS_BEST -> R.id.navAbyssBest

            Link.FAVORITE -> R.id.navFavorite
            Link.COMICS -> R.id.navComics

            else -> R.id.navNew
        })?.isChecked = true

    }

    private fun saveQuoteState() {
        val pos = (recycler()?.layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition() ?: 0
        val view = recycler()?.findViewWithTag<View>(pos)

        val link = link()

        quoteHelper.setupPosition(link, viewPager.currentItem, pos,
                (-(view?.top ?: 0)))

        if (link != Link.COMICS) {
            val adapter = viewPager.adapter as QuotePagerAdapter?
            val quotes = (recycler()?.adapter as? QuoteAdapter)?.get() ?: listOf()

            quoteHelper.saveQuotes(quotes)

            if (adapter != null) {
                val loader = adapter.getHolder(viewPager.currentItem)?.loader
                if (loader is RegularQuoteLoader?) {
                    quoteHelper.setupLoaderData(loader?.nextData ?: "", loader?.defaultData ?: "")

                    if (link == Link.BEST_MONTH || link == Link.BEST_YEAR || link == Link.ABYSS_BEST) {
                        quoteHelper.setupDate(adapter.getRefDate())
                    }
                } else if (loader is FavoriteQuotesLoader?) {
                    quoteHelper.setupFavoriteOffset(loader?.getOffset() ?: 0)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        saveQuoteState()
    }

    private fun hideRandom() {
        if (navigationView.menu.findItem(R.id.navRandomOnline).isVisible) {
            changeStateRandom()
        }
    }

    private fun hideBest() {
        if (navigationView.menu.findItem(R.id.navBestToday).isVisible) {
            changeStateBest()
        }
    }

    private fun hideAbyss() {
        if (navigationView.menu.findItem(R.id.navAbyssNew).isVisible) {
            changeStateAbyss()
        }
    }

    private fun changeStateRandom() {
        val visibility = !navigationView.menu.findItem(R.id.navRandomOnline).isVisible

        navigationView.menu.findItem(R.id.navRandomOnline).setVisible(visibility)
        navigationView.menu.findItem(R.id.navRandomOffline).setVisible(visibility)

        navigationView.menu.findItem(R.id.navRandomTitle).setTitle(if (visibility) R.string.link_random_title_up else R.string.link_random_title_down)
    }

    private fun changeStateBest() {
        val visibility = !navigationView.menu.findItem(R.id.navBestToday).isVisible

        navigationView.menu.findItem(R.id.navBestToday).setVisible(visibility)
        navigationView.menu.findItem(R.id.navBestMonth).setVisible(visibility)
        navigationView.menu.findItem(R.id.navBestYear).setVisible(visibility)
        navigationView.menu.findItem(R.id.navBestAll).setVisible(visibility)

        navigationView.menu.findItem(R.id.navBestTitle).setTitle(if (visibility) R.string.link_best_title_up else R.string.link_best_title_down)
    }

    private fun changeStateAbyss() {
        val visibility = !navigationView.menu.findItem(R.id.navAbyssNew).isVisible

        navigationView.menu.findItem(R.id.navAbyssNew).setVisible(visibility)
        navigationView.menu.findItem(R.id.navAbyssTop).setVisible(visibility)
        navigationView.menu.findItem(R.id.navAbyssBest).setVisible(visibility)

        navigationView.menu.findItem(R.id.navAbyssTitle).setTitle(if (visibility) R.string.link_abyss_title_up else R.string.link_abyss_title_down)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        item.actionView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

        when(item.itemId) {
            R.id.navNew -> loadOtherQuotes(Link.NEW)

            R.id.navRandomOnline -> loadOtherQuotes(Link.RANDOM_ONLINE)
            R.id.navRandomOffline -> loadOtherQuotes(Link.RANDOM_OFFLINE)

            R.id.navBestToday -> loadOtherQuotes(Link.BEST_TODAY)
            R.id.navBestMonth -> loadOtherQuotes(Link.BEST_MONTH)
            R.id.navBestYear -> loadOtherQuotes(Link.BEST_YEAR)
            R.id.navBestAll -> loadOtherQuotes(Link.BEST_ALL)

            R.id.navAbyssNew -> loadOtherQuotes(Link.ABYSS_NEW)
            R.id.navAbyssTop -> loadOtherQuotes(Link.ABYSS_TOP)
            R.id.navAbyssBest -> loadOtherQuotes(Link.ABYSS_BEST)

            R.id.navFavorite -> loadOtherQuotes(Link.FAVORITE)
            R.id.navComics -> loadOtherQuotes(Link.COMICS)

            R.id.navSettings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivityForResult(intent, SETTINGS)
//                overridePendingTransition(R.anim.fadeout, R.anim.fadenone)
            }
            R.id.navExit -> finish()

            R.id.navRandomTitle -> {
                changeStateRandom()
                return true
            }
            R.id.navBestTitle -> {
                changeStateBest()
                return true
            }
            R.id.navAbyssTitle -> {
                changeStateAbyss()
                return true
            }
        }

        hideRandom()
        hideBest()
        hideAbyss()

        if (!resourceHelper.bool(R.bool.is_drawer_fixed))
            drawerLayout.closeDrawer(GravityCompat.START)

        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == COMICS && resultCode == RESULT_OK) {
            val index = data?.extras?.getInt(ComicsActivity.COMICS_ID, 0) ?: 0
            Log.i("ComicsAdapter","Before update")
            (viewPager.adapter as ComicsHeaderPagerAdapter).update(index)
            Log.i("ComicsAdapter","After update")
        } else {
            if (quoteHelper.isNeedUpdateTheme()) {
                quoteHelper.saveUpdateTheme(false)
                recreate()
            }
        }
    }

}
