package app.atomofiron.blockclock.widget

import androidx.glance.appwidget.GlanceAppWidget

/** Ресивер виджета «только время» (2 × 1). */
class TimeOnlyWidgetReceiver : BaseClockWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimeOnlyWidget()
}
