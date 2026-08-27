package app.blockclock.widget

import androidx.glance.appwidget.GlanceAppWidget

class TwoLevelWidgetReceiver : BaseClockWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TwoLevelWidget()
}
