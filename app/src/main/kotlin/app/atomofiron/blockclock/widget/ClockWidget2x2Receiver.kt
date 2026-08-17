package app.atomofiron.blockclock.widget

import androidx.glance.appwidget.GlanceAppWidget

/**
 * Ресивер виджета «время + дата» в квадратном размере 2 × 2.
 * Класс виджета тот же, что и у 4 × 1 — компоновка (горизонтальная 4:1
 * или квадратная 1:1) выбирается сама по доступному пространству.
 */
class ClockWidget2x2Receiver : BaseClockWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClockWidget()
}
