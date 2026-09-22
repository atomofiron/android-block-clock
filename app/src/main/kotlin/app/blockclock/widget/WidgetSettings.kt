package app.blockclock.widget

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.glance.appwidget.updateAll
import app.blockclock.model.TargetApp
import app.blockclock.model.TextStyle
import app.blockclock.model.WidgetFont

@Immutable
data class WidgetSettings(
    val background: Color = Color.White,
    val transparency: Float = 0.3f,
    val text: Color = Color.Black,
    val dayFirst: Boolean = true,
    /** True adds the AM/PM marker to the weekday cell; the clock format follows the system. */
    val amPm: Boolean = false,
    val gapDp: Int = 6,
    val cornerRadiusDp: Int = 12,
    /** The height of the time text as a percentage of the cell height. */
    val timeFontPercent: Int = 116,
    /** The height of the date text as a percentage of the cell height. */
    val dateFontPercent: Int = 100,
    /** The app opened by the clock tap; null = the default clock app. */
    val clockApp: TargetApp? = null,
    /** The app opened by the date tap; null = the default calendar app. */
    val calendarApp: TargetApp? = null,
    /** The text font; null = the system default font. */
    val font: WidgetFont? = null,
    /** The style of the text of the default font; a picked font brings its own style. */
    val textStyle: TextStyle = TextStyle.Normal,
) {
    /** The rectangle color with the transparency applied. */
    val effectiveRectColor: Color get() = background.copy(alpha = 1f - transparency)
}

suspend fun Context.updateWidgets() = listOf(
    OneLevelWidget(),
    TwoLevelWidget(),
    ThreeLevelWidget(),
    TimeOnlyWidget(),
    DateOnlyWidget(),
).forEach { it.updateAll(this) }
