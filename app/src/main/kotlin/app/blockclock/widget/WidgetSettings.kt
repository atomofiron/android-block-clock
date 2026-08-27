package app.blockclock.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.appwidget.updateAll

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
    val cornerRadiusDp: Int = 12,
) {
    /** Цвет прямоугольников с учётом прозрачности. */
    val effectiveRectColor: Color get() = background.copy(alpha = 1f - transparency)
}

suspend fun Context.updateWidgets() {
    OneLevelWidget().updateAll(this)
    TwoLevelWidget().updateAll(this)
    ThreeLevelWidget().updateAll(this)
    TimeOnlyWidget().updateAll(this)
    DateOnlyWidget().updateAll(this)
}
