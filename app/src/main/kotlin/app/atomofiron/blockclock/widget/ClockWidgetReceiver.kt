package app.atomofiron.blockclock.widget

import androidx.glance.appwidget.GlanceAppWidget

/** Ресивер виджета «время + дата» (4 × 1). */
class ClockWidgetReceiver : BaseClockWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClockWidget()
}
