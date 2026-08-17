package app.atomofiron.blockclock.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * После перезагрузки устройства будильники сбрасываются —
 * восстанавливает расписание обновлений, если виджет установлен на экран.
 */
class ClockWidgetBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            try {
                val manager = GlanceAppWidgetManager(context)
                val anyWidgetLeft = listOf(
                    ClockWidget::class.java,
                    TimeOnlyWidget::class.java,
                    DateOnlyWidget::class.java,
                ).any { manager.getGlanceIds(it).isNotEmpty() }
                if (anyWidgetLeft) {
                    ClockWidgetScheduler.scheduleNext(context)
                    ClockWidget().updateAll(context)
                    TimeOnlyWidget().updateAll(context)
                    DateOnlyWidget().updateAll(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
