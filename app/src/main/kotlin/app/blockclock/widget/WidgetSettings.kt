package app.blockclock.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.appwidget.updateAll

data class WidgetSettings(
    val background: Color = Color.White,
    val transparency: Float = 0.3f,
    val text: Color = Color.Black,
    val dayFirst: Boolean = true,
    val gapDp: Int = 6,
    val cornerRadiusDp: Int = 12,
) {
    /** The rectangle color with the transparency applied. */
    val effectiveRectColor: Color get() = background.copy(alpha = 1f - transparency)
}

suspend fun Context.updateWidgets() {
    OneLevelWidget().updateAll(this)
    TwoLevelWidget().updateAll(this)
    ThreeLevelWidget().updateAll(this)
    TimeOnlyWidget().updateAll(this)
    DateOnlyWidget().updateAll(this)
}
