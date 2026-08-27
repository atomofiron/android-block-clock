package app.blockclock.widget

import androidx.glance.appwidget.GlanceAppWidget

class ThreeLevelWidgetReceiver : BaseClockWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ThreeLevelWidget()
}
