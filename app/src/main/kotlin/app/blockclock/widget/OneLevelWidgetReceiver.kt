package app.blockclock.widget

import androidx.glance.appwidget.GlanceAppWidget

class OneLevelWidgetReceiver : BaseClockWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OneLevelWidget()
}
