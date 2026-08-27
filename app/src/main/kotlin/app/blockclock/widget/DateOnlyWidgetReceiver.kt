package app.blockclock.widget

import androidx.glance.appwidget.GlanceAppWidget

class DateOnlyWidgetReceiver : BaseClockWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DateOnlyWidget()
}
