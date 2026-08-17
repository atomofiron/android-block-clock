package app.atomofiron.blockclock.widget

import androidx.glance.appwidget.GlanceAppWidget

/** Ресивер виджета «только дата» (2 × 1). */
class DateOnlyWidgetReceiver : BaseClockWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DateOnlyWidget()
}
