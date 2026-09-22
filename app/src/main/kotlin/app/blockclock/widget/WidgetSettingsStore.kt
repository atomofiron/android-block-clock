package app.blockclock.widget

import android.content.Context
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.text.format.DateFormat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import app.blockclock.model.ColorSource
import app.blockclock.model.ColorSources
import app.blockclock.model.ColorTarget
import app.blockclock.model.TargetApp
import app.blockclock.model.TextStyle
import app.blockclock.util.Android
import app.blockclock.util.unsafeLazy
import app.blockclock.util.widgetFont

class WidgetSettingsStore(context: Context) {
    companion object {
        private const val FLAG_EMPTY = "empty"
        private const val KEY_RECT_COLOR = "rect_color"
        private const val KEY_RECT_TRANSPARENCY = "rect_transparency"
        private const val KEY_TEXT_COLOR = "text_color"
        private const val KEY_DAY_FIRST = "day_first"
        private const val KEY_AM_PM = "am_pm"
        private const val KEY_GAP_DP = "gap_dp"
        private const val KEY_CORNER_RADIUS_DP = "corner_radius_dp"
        private const val KEY_TIME_FONT_PERCENT = "time_font_percent"
        private const val KEY_DATE_FONT_PERCENT = "date_font_percent"
        private const val KEY_TEXT_SCALE_PERCENT = "text_scale_percent"
        private const val KEY_CLOCK_APP = "clock_app"
        private const val KEY_CALENDAR_APP = "calendar_app"
        private const val KEY_RECT_SOURCE = "rect_source"
        private const val KEY_TEXT_SOURCE = "text_source"
        private const val KEY_CONTRAST = "contrast"
        private const val KEY_FONT_PATH = "font_path"
        private const val KEY_FONT_TTC_INDEX = "font_ttc_index"
        private const val KEY_FONT_VARIATIONS = "font_variations"
        private const val KEY_TEXT_STYLE = "text_style"

        val Defaults = WidgetSettings()
    }

    private val sp = context.getSharedPreferences("widget_settings", Context.MODE_PRIVATE)
    val systemDayFirst by unsafeLazy {
        DateFormat.getDateFormatOrder(context).run { indexOf('d') < indexOf('M') }
    }
    /** True when the system shows the time on the 12-hour clock, so a marker makes sense. */
    val systemAmPm by unsafeLazy { !DateFormat.is24HourFormat(context) }

    fun read() = WidgetSettings(
        background = Color(sp.getInt(KEY_RECT_COLOR, Defaults.background.toArgb())),
        transparency = sp.getFloat(KEY_RECT_TRANSPARENCY, Defaults.transparency),
        text = Color(sp.getInt(KEY_TEXT_COLOR, Defaults.text.toArgb())),
        gapDp = sp.getInt(KEY_GAP_DP, Defaults.gapDp),
        cornerRadiusDp = sp.getInt(KEY_CORNER_RADIUS_DP, Defaults.cornerRadiusDp),
        timeFontPercent = sp.getInt(KEY_TIME_FONT_PERCENT, Defaults.timeFontPercent),
        dateFontPercent = sp.getInt(KEY_DATE_FONT_PERCENT, Defaults.dateFontPercent),
        textScale = sp.getFloat(KEY_TEXT_SCALE_PERCENT, Defaults.textScale),
        dayFirst = sp.getBoolean(KEY_DAY_FIRST, systemDayFirst),
        amPm = systemAmPm && sp.getBoolean(KEY_AM_PM, systemAmPm),
        clockApp = sp.getString(KEY_CLOCK_APP, null).toAppTarget(),
        calendarApp = sp.getString(KEY_CALENDAR_APP, null).toAppTarget(),
        font = when {
            Android.Q -> sp.getString(KEY_FONT_PATH, null)?.let { path ->
                widgetFont(
                    path = path,
                    ttcIndex = sp.getInt(KEY_FONT_TTC_INDEX, 0),
                    variations = sp.getString(KEY_FONT_VARIATIONS, null).toVariations(),
                )
            }
            else -> null
        },
        textStyle = TextStyle.from(sp.getString(KEY_TEXT_STYLE, null)),
    )

    fun store(
        settings: WidgetSettings,
        target: ColorTarget? = null,
        source: ColorSource? = null,
        saveContrast: Boolean = false,
    ) {
        sp.edit {
            putBoolean(FLAG_EMPTY, false)
            putInt(KEY_RECT_COLOR, settings.background.toArgb())
            putFloat(KEY_RECT_TRANSPARENCY, settings.transparency)
            putInt(KEY_TEXT_COLOR, settings.text.toArgb())
            putInt(KEY_GAP_DP, settings.gapDp)
            putInt(KEY_CORNER_RADIUS_DP, settings.cornerRadiusDp)
            putInt(KEY_TIME_FONT_PERCENT, settings.timeFontPercent)
            putInt(KEY_DATE_FONT_PERCENT, settings.dateFontPercent)
            putFloat(KEY_TEXT_SCALE_PERCENT, settings.textScale)

            when (settings.dayFirst) {
                systemDayFirst -> remove(KEY_DAY_FIRST)
                else -> putBoolean(KEY_DAY_FIRST, settings.dayFirst)
            }
            when (settings.amPm) {
                systemAmPm -> remove(KEY_AM_PM)
                else -> putBoolean(KEY_AM_PM, settings.amPm)
            }
            when (val target = settings.clockApp) {
                null -> remove(KEY_CLOCK_APP)
                else -> putString(KEY_CLOCK_APP, target.encode())
            }
            when (val target = settings.calendarApp) {
                null -> remove(KEY_CALENDAR_APP)
                else -> putString(KEY_CALENDAR_APP, target.encode())
            }
            when {
                settings.font == null -> {
                    remove(KEY_FONT_PATH)
                    remove(KEY_FONT_TTC_INDEX)
                    remove(KEY_FONT_VARIATIONS)
                }
                Android.Q -> {
                    val font = settings.font
                    putString(KEY_FONT_PATH, font.path)
                    putInt(KEY_FONT_TTC_INDEX, font.ttcIndex)
                    when (font.variations.isEmpty()) {
                        true -> remove(KEY_FONT_VARIATIONS)
                        false -> putString(KEY_FONT_VARIATIONS, font.variations.encode())
                    }
                }
            }
            when (settings.textStyle) {
                TextStyle.Normal -> remove(KEY_TEXT_STYLE)
                else -> putString(KEY_TEXT_STYLE, settings.textStyle.name)
            }
            if (source != null) when (target) {
                null -> null
                ColorTarget.Rect -> KEY_RECT_SOURCE
                ColorTarget.Text -> KEY_TEXT_SOURCE
            }?.let { putString(it, source.name) }
            if (saveContrast) {
                val contrast = ColorUtils.calculateContrast(settings.text.toArgb(), settings.background.toArgb())
                putFloat(KEY_CONTRAST, contrast.toFloat())
            }
        }
    }

    fun readContrast() = sp.getFloat(KEY_CONTRAST, 1f)

    fun readSources() = ColorSources(
        rect = ColorSource.from(sp.getString(KEY_RECT_SOURCE, null)),
        text = ColorSource.from(sp.getString(KEY_TEXT_SOURCE, null)),
    )

    fun isEmpty(): Boolean = sp.getBoolean(FLAG_EMPTY, true)

    fun setListener(listener: OnSharedPreferenceChangeListener) {
        sp.registerOnSharedPreferenceChangeListener(listener)
    }

    fun removeListener(listener: OnSharedPreferenceChangeListener) {
        sp.unregisterOnSharedPreferenceChangeListener(listener)
    }
}

private fun TargetApp.encode(): String = "$packageName/$activityName"

private fun Map<String, Float>.encode(): String = entries.joinToString(VARIATION_SEPARATOR) { "${it.key}$VARIATION_VALUE_SEPARATOR${it.value}" }

private fun String?.toVariations(): Map<String, Float> = this
    ?.split(VARIATION_SEPARATOR)
    ?.mapNotNull { variation ->
        variation.split(VARIATION_VALUE_SEPARATOR, limit = 2)
            .takeIf { it.size == 2 }
            ?.let { (tag, value) -> tag to value.toFloat() }
    }
    ?.toMap()
    .orEmpty()

private const val VARIATION_SEPARATOR = ","
private const val VARIATION_VALUE_SEPARATOR = "="

fun String?.toAppTarget(): TargetApp? = this
    ?.split('/', limit = 2)
    ?.takeIf { it.size == 2 }
    ?.let { (pkg, activity) -> TargetApp(pkg, activity) }
