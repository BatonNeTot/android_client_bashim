package com.notjuststudio.bashim.helper

import android.content.Context
import android.content.SharedPreferences
import android.support.annotation.StyleRes
import com.notjuststudio.bashim.R
import com.notjuststudio.bashim.common.Link

class SharedPrefHelper(private val context: Context) {

    companion object {
        const val SHARED_PREF_NAME = "shared"

        const val FAVORITE = "favorite"
        const val IS_DARK_THEME = "isThemeDark"
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

    fun saveIsDarkTheme(isDark: Boolean) {
        val edit = sharedPref.edit()
        edit.putBoolean(IS_DARK_THEME, isDark)
        edit.apply()
    }


}