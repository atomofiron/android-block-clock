package app.blockclock.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.appwidget.updateAll
import app.blockclock.model.TargetApp

data class WidgetSettings(
    val background: Color = Color.White,
    val transparency: Float = 0.3f,
    val text: Color = Color.Black,
    val dayFirst: Boolean = true,
    val gapDp: Int = 6,
    val cornerRadiusDp: Int = 12,
    /** The app opened by the clock tap; null = the default clock app. */
    val clockApp: TargetApp? = null,
    /** The app opened by the date tap; null = the default calendar app. */
    val calendarApp: TargetApp? = null,
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
