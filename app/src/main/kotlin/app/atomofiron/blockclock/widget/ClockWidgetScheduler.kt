package app.atomofiron.blockclock.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Планирует обновление виджета на границу каждой минуты через AlarmManager.
 * Launcher умеет обновлять виджеты не чаще, чем раз в ~30 минут,
 * поэтому точное время — только через собственный будильник.
 */
object ClockWidgetScheduler {

    const val ACTION_UPDATE = "app.atomofiron.blockclock.action.UPDATE_CLOCK_WIDGET"
    private const val ALARM_REQUEST_CODE = 1001

    fun scheduleNext(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = updatePendingIntent(context)
        val now = System.currentTimeMillis()
        val nextMinuteBoundary = (now / 60_000L + 1) * 60_000L

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            
            alarmManager.setWindow(AlarmManager.RTC, nextMinuteBoundary, 30_000L, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, nextMinuteBoundary, pendingIntent)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(updatePendingIntent(context))
    }

    private fun updatePendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            Intent(context, ClockWidgetReceiver::class.java).setAction(ACTION_UPDATE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
