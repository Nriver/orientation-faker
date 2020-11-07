/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.orientation.settings

import android.content.Context
import androidx.core.content.ContextCompat
import net.mm2d.android.orientationfaker.R
import net.mm2d.orientation.control.Orientation

internal object Default {
    lateinit var color: Color
        private set

    val orientationList: List<Int> = listOf(
        Orientation.UNSPECIFIED,
        Orientation.PORTRAIT,
        Orientation.LANDSCAPE,
        Orientation.REVERSE_PORTRAIT,
        Orientation.REVERSE_LANDSCAPE
    )

    fun init(context: Context) {
        color = Color(
            ContextCompat.getColor(context, R.color.fg_notification),
            ContextCompat.getColor(context, R.color.bg_notification),
            ContextCompat.getColor(context, R.color.fg_notification),
            ContextCompat.getColor(context, R.color.bg_notification_selected)
        )
    }

    class Color(
        val foreground: Int,
        val background: Int,
        val foregroundSelected: Int,
        val backgroundSelected: Int
    )
}
