package com.notjuststudio.bashim.helper

import android.content.Context
import android.support.annotation.*
import android.support.v4.content.ContextCompat
import android.util.TypedValue

class ResourceHelper(private val context: Context) {

    fun bool(@BoolRes resourceId: Int) = context.resources.getBoolean(resourceId)

    fun string(@StringRes resourceId: Int) = context.getString(resourceId)

    fun string(@StringRes resourceId: Int, vararg formatArgs: Any?) = context.getString(resourceId, *formatArgs)

    fun stringArray(@ArrayRes resourceId: Int) = context.resources.getStringArray(resourceId)

    fun int(@IntegerRes resourceId: Int) = context.resources.getInteger(resourceId)

    fun intArray(@ArrayRes resourceId: Int) = context.resources.getIntArray(resourceId)

    fun color(@ColorRes resourceId: Int) = ContextCompat.getColor(context, resourceId)

    @ColorRes fun colorIdFromAttr(@AttrRes attributeId: Int) : Int {
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(attributeId, typedValue, true)
        return typedValue.resourceId
    }

    @ColorInt fun colorFromAttr(@AttrRes attributeId: Int) : Int {
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(attributeId, typedValue, true)
        return typedValue.data
    }

    @DrawableRes fun drawableIdFromAttr(@AttrRes attributeId: Int) : Int {
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(attributeId, typedValue, true)
        return typedValue.resourceId
    }

    fun intFromAttr(@AttrRes attributeId: Int) : Int {
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(attributeId, typedValue, true)
        return typedValue.data
    }

}