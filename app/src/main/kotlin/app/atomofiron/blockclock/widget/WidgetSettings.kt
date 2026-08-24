package app.atomofiron.blockclock.widget

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit

/**
 * Параметры оформления и поведения виджета.
 */
data class WidgetSettings(
    val background: Color = Color.White,
    val transparency: Float = 0.3f,
    val text: Color = Color.Black,
    val dayFirst: Boolean = true,
    /** Отступ между прямоугольниками в dp: 1..16. */
    val gapDp: Int = 6,
    /** Радиус скругления углов прямоугольников в dp: 1..32. */
    val cornerRadiusDp: Int = 16,
) {
    /** Цвет прямоугольников с учётом прозрачности. */
    val effectiveRectColor: Color get() = background.copy(alpha = 1f - transparency)
}

/**
 * Хранение настроек виджета в SharedPreferences.
 * Читается как из Glance-кода (provideGlance), так и из экрана настроек.
 */
object WidgetSettingsStore {

    const val PREFS_NAME = "widget_settings"
    private const val KEY_RECT_COLOR = "rect_color"
    private const val KEY_RECT_TRANSPARENCY = "rect_transparency"
    private const val KEY_TEXT_COLOR = "text_color"
    private const val KEY_DAY_FIRST = "day_first"
    private const val KEY_GAP_DP = "gap_dp"
    private const val KEY_CORNER_RADIUS_DP = "corner_radius_dp"

    /**
     * Порядок даты из системных настроек: true = день перед месяцем.
     * Используется как значение по умолчанию и не сохраняется в prefs.
     */
    fun systemDayFirst(context: Context): Boolean {
        val order = DateFormat.getDateFormatOrder(context)
        return order.indexOf('d') < order.indexOf('M')
    }

    fun load(context: Context): WidgetSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaults = WidgetSettings()
        return WidgetSettings(
            background = Color(prefs.getInt(KEY_RECT_COLOR, defaults.background.toArgb())),
            transparency = prefs.getFloat(KEY_RECT_TRANSPARENCY, defaults.transparency),
            text = Color(prefs.getInt(KEY_TEXT_COLOR, defaults.text.toArgb())),
            
            dayFirst = if (prefs.contains(KEY_DAY_FIRST)) {
                prefs.getBoolean(KEY_DAY_FIRST, defaults.dayFirst)
            } else {
                systemDayFirst(context)
            },
            gapDp = prefs.getInt(KEY_GAP_DP, defaults.gapDp),
            cornerRadiusDp = prefs.getInt(KEY_CORNER_RADIUS_DP, defaults.cornerRadiusDp),
        )
    }

    fun save(context: Context, settings: WidgetSettings) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putInt(KEY_RECT_COLOR, settings.background.toArgb())
            putFloat(KEY_RECT_TRANSPARENCY, settings.transparency)
            putInt(KEY_TEXT_COLOR, settings.text.toArgb())
            putInt(KEY_GAP_DP, settings.gapDp)
            putInt(KEY_CORNER_RADIUS_DP, settings.cornerRadiusDp)

            when (settings.dayFirst) {
                systemDayFirst(context) -> remove(KEY_DAY_FIRST)
                else -> putBoolean(KEY_DAY_FIRST, settings.dayFirst)
            }
        }
    }
}
