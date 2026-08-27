package app.blockclock.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * The common base class for widget receivers.
 * Time/date are updated by the native TextClock, so no scheduling is needed.
 */
abstract class BaseClockWidgetReceiver : GlanceAppWidgetReceiver()
