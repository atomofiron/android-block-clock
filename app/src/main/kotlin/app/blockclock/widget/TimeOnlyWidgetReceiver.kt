package app.blockclock.widget

import androidx.glance.appwidget.GlanceAppWidget

class TimeOnlyWidgetReceiver : BaseClockWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimeOnlyWidget()
}
