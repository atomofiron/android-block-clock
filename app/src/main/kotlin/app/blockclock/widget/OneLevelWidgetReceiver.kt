package app.blockclock.widget

import androidx.glance.appwidget.GlanceAppWidget

/** Ресивер виджета «время + дата» (4 × 1, один уровень). */
class OneLevelWidgetReceiver : BaseClockWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OneLevelWidget()
}
