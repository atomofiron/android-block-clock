package app.blockclock.widget

import android.content.Context
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.text.format.DateFormat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import app.blockclock.model.TargetApp

class WidgetSettingsStore(context: Context) {
    companion object {
        private const val FLAG_EMPTY = "empty"
        private const val KEY_RECT_COLOR = "rect_color"
        private const val KEY_RECT_TRANSPARENCY = "rect_transparency"
        private const val KEY_TEXT_COLOR = "text_color"
        private const val KEY_DAY_FIRST = "day_first"
        private const val KEY_GAP_DP = "gap_dp"
        private const val KEY_CORNER_RADIUS_DP = "corner_radius_dp"
        private const val KEY_CLOCK_APP = "clock_app"
        private const val KEY_CALENDAR_APP = "calendar_app"

        val Defaults = WidgetSettings()
    }

    private val sp = context.getSharedPreferences("widget_settings", Context.MODE_PRIVATE)
    private val systemDayFirst by lazy(LazyThreadSafetyMode.NONE) {
        DateFormat.getDateFormatOrder(context).run { indexOf('d') < indexOf('M') }
    }

    fun read() = WidgetSettings(
        background = Color(sp.getInt(KEY_RECT_COLOR, Defaults.background.toArgb())),
        transparency = sp.getFloat(KEY_RECT_TRANSPARENCY, Defaults.transparency),
        text = Color(sp.getInt(KEY_TEXT_COLOR, Defaults.text.toArgb())),
        gapDp = sp.getInt(KEY_GAP_DP, Defaults.gapDp),
        cornerRadiusDp = sp.getInt(KEY_CORNER_RADIUS_DP, Defaults.cornerRadiusDp),
        dayFirst = sp.getBoolean(KEY_DAY_FIRST, systemDayFirst),
        clockApp = sp.getString(KEY_CLOCK_APP, null).toAppTarget(),
        calendarApp = sp.getString(KEY_CALENDAR_APP, null).toAppTarget(),
    )

    fun store(settings: WidgetSettings) {
        sp.edit {
            putBoolean(FLAG_EMPTY, false)
            putInt(KEY_RECT_COLOR, settings.background.toArgb())
            putFloat(KEY_RECT_TRANSPARENCY, settings.transparency)
            putInt(KEY_TEXT_COLOR, settings.text.toArgb())
            putInt(KEY_GAP_DP, settings.gapDp)
            putInt(KEY_CORNER_RADIUS_DP, settings.cornerRadiusDp)

            when (settings.dayFirst) {
                systemDayFirst -> remove(KEY_DAY_FIRST)
                else -> putBoolean(KEY_DAY_FIRST, settings.dayFirst)
            }
            when (val target = settings.clockApp) {
                null -> remove(KEY_CLOCK_APP)
                else -> putString(KEY_CLOCK_APP, target.encode())
            }
            when (val target = settings.calendarApp) {
                null -> remove(KEY_CALENDAR_APP)
                else -> putString(KEY_CALENDAR_APP, target.encode())
            }
        }
    }

    fun isEmpty(): Boolean = sp.getBoolean(FLAG_EMPTY, true)

    fun setListener(listener: OnSharedPreferenceChangeListener) {
        sp.registerOnSharedPreferenceChangeListener(listener)
    }

    fun removeListener(listener: OnSharedPreferenceChangeListener) {
        sp.unregisterOnSharedPreferenceChangeListener(listener)
    }
}

private fun TargetApp.encode(): String = "$packageName/$activityName"

fun String?.toAppTarget(): TargetApp? = this
    ?.split('/', limit = 2)
    ?.takeIf { it.size == 2 }
    ?.let { (pkg, activity) -> TargetApp(pkg, activity) }
