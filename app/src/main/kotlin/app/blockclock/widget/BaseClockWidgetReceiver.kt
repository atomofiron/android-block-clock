package app.blockclock.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Общий базовый класс ресиверов виджетов.
 * Время/дата обновляются нативным TextClock, поэтому никакого планирования нет.
 */
abstract class BaseClockWidgetReceiver : GlanceAppWidgetReceiver()
