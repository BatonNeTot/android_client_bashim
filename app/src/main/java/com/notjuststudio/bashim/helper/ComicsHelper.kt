package com.notjuststudio.bashim.helper

import com.notjuststudio.bashim.App
import com.notjuststudio.bashim.R
import com.notjuststudio.bashim.comics.ComicsHeaderLoader
import com.notjuststudio.bashim.common.ComicsMonth
import com.notjuststudio.bashim.common.ComicsUnit
import com.notjuststudio.bashim.common.ComicsYear

class ComicsHelper(private val imageLoaderHelper: ImageLoaderHelper, private val resourceHelper: ResourceHelper) {

    private val headerLoader = ComicsHeaderLoader()

    fun discardLoader() {
        imageLoaderHelper.resetErrorFlag()
        discardRefs()
        comicsYears.clear()
        comicsCount = 0
    }

    private fun discardRefs() {
        comicsRefs.clear()
        comicsRefs.add(ComicsHeaderLoader.DEFAULT_REF)
    }

    private val comicsRefs = mutableListOf<String>()
    private val comicsYears = mutableListOf<ComicsYear>()

    private var comicsCount = 0

    init {
        discardRefs()
    }

    fun hasYearToLoad() : Boolean {
        return comicsRefs.size > comicsYears.size
    }

    fun loadYear(ref: String, onDone: () -> Unit = {}, onFail: () -> Unit = {}) {
        headerLoader.loadHeaders(ref, {
            year, nextRef ->

            val index = comicsYears.indexOf(year)
            if (index != -1) {
                val oldYear = comicsYears[index]

                for (month in oldYear.months) {
                    comicsCount -= month.comics.size
                }

                comicsYears.removeAt(index)
                comicsYears.add(index, year)
            } else {
                comicsYears.add(year)
            }

            for (month in year.months) {
                comicsCount += month.comics.size
            }

            if (nextRef != null && !comicsRefs.contains(nextRef))
                comicsRefs.add(nextRef)


            onDone.invoke()
        }, {
            App.error(R.string.quotes_load_error)
            onFail()
        })
    }

    fun getComicsCount() : Int {
        return comicsCount
    }

    fun getRef(index: Int) : String {
        return comicsRefs[index]
    }

    fun getRefsCount() : Int {
        return comicsRefs.size
    }

    fun getYearName(yearIndex: Int) : String {
        return if (comicsYears.size <= yearIndex) resourceHelper.string(R.string.comics_need_to_update) else comicsYears[yearIndex].name
    }

    fun getYearCount() : Int {
        return comicsYears.size
    }

    fun getMonth(yearIndex: Int, monthIndex: Int) : ComicsMonth{
        return comicsYears[yearIndex].months[monthIndex]
    }

    fun getMonthCount(yearIndex: Int) : Int {
        return if (comicsYears.size <= yearIndex) 0 else comicsYears[yearIndex].months.size
    }

    fun getComics(index: Int) : ComicsUnit {
        if (index < comicsCount) {
            var fullIndex = index
            for (year in comicsYears) {
                for (month in year.months) {
                    if (fullIndex < month.comics.size) {
                        return month.comics[fullIndex]
                    } else {
                        fullIndex -= month.comics.size
                    }
                }
            }
        }
        throw IndexOutOfBoundsException()
    }

    fun findComicsIndex(url: String, onFound: (Int) -> Unit, onNotFound: () -> Unit, onFail: () -> Unit) {
        val index = getIndex(url)
        if (index == -1) {
            if (hasYearToLoad()) {
                loadYear(comicsRefs[comicsRefs.size - 1], onDone = {
                    findComicsIndex(url, onFound, onNotFound, onFail)
                }, onFail = onFail)
            } else {
                onFound(0)
            }
        } else {
            if (index == -2) {
                onNotFound()
            } else {
                onFound(index)
            }
        }
    }

    fun getIndex(url: String) : Int {
        var fullIndex = 0
        for (year in comicsYears) {
            for (month in year.months) {
                for (i in 0 until month.comics.size) {
                    val checkingId = url.subSequence(url.lastIndexOf('/') + 1, url.length).toString().toInt()
                    val tmp = month.comics[i].header.comicsId
                    val id = tmp.subSequence(tmp.lastIndexOf('/') + 1, tmp.length).toString().toInt()
                    if (id == checkingId) {
                        return fullIndex + i
                    }
                    if (id < checkingId) {
                        return -2
                    }
                }
                fullIndex += month.comics.size
            }
        }
        return -1
    }

    fun getIndex(comics: ComicsUnit) : Int {
        var fullIndex = 0
        for (year in comicsYears) {
            for (month in year.months) {
                val index = month.comics.indexOf(comics)
                if (index != -1) {
                    return fullIndex + index
                } else {
                    fullIndex += month.comics.size
                }
            }
        }
        throw IndexOutOfBoundsException()
    }

    fun findYearIndex(year: Int, onFound: (Int) -> Unit, onFail: () -> Unit) {
        val index = getYearIndex(year)
        if (index == -1) {
            if (hasYearToLoad()) {
                loadYear(comicsRefs[comicsRefs.size - 1], onDone = {
                    findYearIndex(year, onFound, onFail)
                }, onFail = onFail)
            } else {
                onFound(0)
            }
        } else {
            onFound(index)
        }
    }

    fun getYearIndex(year: Int) : Int {
        for (i in 0 until comicsYears.size) {
            val comicsYear = comicsYears[i].name.toInt()
            if (comicsYear == year)
                return i
            if (comicsYear < year)
                return 0
        }
        return -1
    }

    fun getYearIndexByComicsIndex(index: Int) : Int {
        var fullIndex = index
        for (i in 0 until comicsYears.size) {
            for (month in comicsYears[i].months) {
                if (fullIndex < month.comics.size) {
                    return i
                } else {
                    fullIndex -= month.comics.size
                }
            }
        }
        return 0
    }

    fun getMonthIndexByComicsIndex(index: Int) : Int {
        var fullIndex = index
        for (year in comicsYears) {
            for (i in 0 until year.months.size) {
                if (fullIndex < year.months[i].comics.size) {
                    return i
                } else {
                    fullIndex -= year.months[i].comics.size
                }
            }
        }
        return 0
    }

    fun loadBody(index: Int, setup: (String, String?, String, String?) -> Unit, onFail: () -> Unit) {
        getComics(index).loadBody(setup, onFail)
    }

}