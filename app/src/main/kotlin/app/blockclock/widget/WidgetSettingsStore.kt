package app.blockclock.widget

import android.content.Context
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.text.format.DateFormat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit

class WidgetSettingsStore(context: Context) {
    companion object {
        private const val FLAG_EMPTY = "empty"
        private const val KEY_RECT_COLOR = "rect_color"
        private const val KEY_RECT_TRANSPARENCY = "rect_transparency"
        private const val KEY_TEXT_COLOR = "text_color"
        private const val KEY_DAY_FIRST = "day_first"
        private const val KEY_GAP_DP = "gap_dp"
        private const val KEY_CORNER_RADIUS_DP = "corner_radius_dp"
    }

    private val sp = context.getSharedPreferences("widget_settings", Context.MODE_PRIVATE)
    private val systemDayFirst by lazy(LazyThreadSafetyMode.NONE) {
        DateFormat.getDateFormatOrder(context).run { indexOf('d') < indexOf('M') }
    }

    fun read(): WidgetSettings {
        val defaults = WidgetSettings()
        return WidgetSettings(
            background = Color(sp.getInt(KEY_RECT_COLOR, defaults.background.toArgb())),
            transparency = sp.getFloat(KEY_RECT_TRANSPARENCY, defaults.transparency),
            text = Color(sp.getInt(KEY_TEXT_COLOR, defaults.text.toArgb())),
            gapDp = sp.getInt(KEY_GAP_DP, defaults.gapDp),
            cornerRadiusDp = sp.getInt(KEY_CORNER_RADIUS_DP, defaults.cornerRadiusDp),
            dayFirst = when (sp.contains(KEY_DAY_FIRST)) {
                true -> sp.getBoolean(KEY_DAY_FIRST, defaults.dayFirst)
                false -> systemDayFirst
            },
        )
    }

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
