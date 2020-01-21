package me.terenfear.threeswitcher

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import me.terenfear.threeswitcher.library.R

/**
 * Created by Terenfear on 01.10.2019.
 */

val Context.accentColor: Int
    get() = themeColor(R.attr.colorAccent)
val Context.primaryColor: Int
    get() = themeColor(R.attr.colorPrimary)
val Context.controlNormalColor: Int
    get() = themeColor(R.attr.colorControlNormal)
val Context.controlHighlightColor: Int
    get() = themeColor(R.attr.colorControlHighlight)

fun Context.themeColor(@AttrRes attrRes: Int): Int {
    return TypedValue().run {
        theme.resolveAttribute(attrRes, this, true)
        data
    }
}