package com.notjuststudio.bashim.helper

import android.content.Context
import android.content.SharedPreferences
import android.support.annotation.StyleRes
import com.notjuststudio.bashim.R
import com.notjuststudio.bashim.common.Link

class SharedPrefHelper(context: Context,
                       private val resourceHelper: ResourceHelper) {

    companion object {
        const val SHARED_PREF_NAME = "shared"

        const val FAVORITE = "favorite"
        const val IS_DARK_THEME = "isThemeDark"
        const val QUOTE_TEXT_SIZE = "quoteTextSize"
    }


    private var sharedPref: SharedPreferences

    init {
        sharedPref = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
    }

    fun loadFavoriteInt() : Int {
        return sharedPref.getInt(FAVORITE, Link.NEW.id)
    }

    fun loadFavorite() : Link {
        return Link.fromInt(loadFavoriteInt())
    }

    fun saveFavorite(link: Link) {
        val edit = sharedPref.edit()
        edit.putInt(FAVORITE, link.id)
        edit.apply()
    }

    fun isDarkTheme() : Boolean {
        return sharedPref.getBoolean(IS_DARK_THEME, false)
    }

    fun setIsDarkTheme(isDark: Boolean) {
        val edit = sharedPref.edit()
        edit.putBoolean(IS_DARK_THEME, isDark)
        edit.apply()
    }

    fun getQuoteTextSize() : Float {
        return sharedPref.getFloat(QUOTE_TEXT_SIZE, resourceHelper.int(R.integer.quote_text_init_size).toFloat())
    }

    fun setQuoteTextSize(size: Float) {
        val edit = sharedPref.edit()
        edit.putFloat(QUOTE_TEXT_SIZE, size)
        edit.apply()
    }


}