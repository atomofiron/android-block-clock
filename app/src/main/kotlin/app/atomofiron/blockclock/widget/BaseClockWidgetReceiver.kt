package app.atomofiron.blockclock.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Общая логика ресиверов всех трёх виджетов: планирование минутного
 * обновления, пересоздание при изменении настроек и отмена планировщика,
 * когда на рабочем столе не осталось ни одного виджета.
 */
abstract class BaseClockWidgetReceiver : GlanceAppWidgetReceiver() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        ClockWidgetScheduler.scheduleNext(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        ClockWidgetScheduler.scheduleNext(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        scope.launch {
            val manager = GlanceAppWidgetManager(context)
            val anyWidgetLeft = listOf(
                ClockWidget::class.java,
                TimeOnlyWidget::class.java,
                DateOnlyWidget::class.java,
            ).any { manager.getGlanceIds(it).isNotEmpty() }
            if (!anyWidgetLeft) {
                ClockWidgetScheduler.cancel(context)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ClockWidgetScheduler.ACTION_UPDATE) {

            scope.launch {
                ClockWidget().updateAll(context)
                TimeOnlyWidget().updateAll(context)
                DateOnlyWidget().updateAll(context)
            }
            ClockWidgetScheduler.scheduleNext(context)
            return
        }
        super.onReceive(context, intent)
    }

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }
}
