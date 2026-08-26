package app.blockclock.widget

import androidx.glance.appwidget.GlanceAppWidget

/**
 * Ресивер виджета «время + дата» в квадратном размере 2 × 2:
 * три уровня — время, день недели и дата друг под другом.
 */
class ThreeLevelWidgetReceiver : BaseClockWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ThreeLevelWidget()
}
