package app.atomofiron.blockclock.widget

import androidx.glance.appwidget.GlanceAppWidget

/**
 * Ресивер двухуровневого виджета 2 × 2: время сверху,
 * день недели и дата снизу в один ряд.
 */
class TwoLevelWidgetReceiver : BaseClockWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TwoLevelWidget()
}
