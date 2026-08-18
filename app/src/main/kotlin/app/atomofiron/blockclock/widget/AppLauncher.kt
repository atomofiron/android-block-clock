package app.atomofiron.blockclock.widget

import android.content.Context
import android.content.Intent
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartActivity
import app.atomofiron.blockclock.MainActivity

/**
 * Кандидаты «пакет → активити» системных часов для разных вендоров.
 * Перебор, как в лаунчерах: у каждого производителя свои имена.
 */
private val CLOCK_APP_ACTIVITIES = listOf(
    "com.android.alarmclock" to "com.android.alarmclock.AlarmClock",
    "com.android.deskclock" to "com.android.deskclock.AlarmClock",
    "com.android.deskclock" to "com.android.deskclock.DeskClock",
    "com.android.deskclock" to "com.android.deskclock.alarmclock.AlarmClock",
    "com.google.android.deskclock" to "com.android.deskclock.AlarmClock",
    "com.google.android.deskclock" to "com.android.deskclock.DeskClock",
    "com.google.android.deskclock" to "com.android.deskclock.alarmclock.AlarmClock",
    "com.htc.android.worldclock" to "com.htc.android.worldclock.WorldClockTabControl",
    "com.lge.alarm" to "com.lge.alarm.Super_Clock",
    "com.lge.clock" to "com.lge.clock.AlarmClockActivity",
    "com.lenovo.deskclock" to "com.lenovo.deskclock.AlarmClock",
    "com.motorola.blur.alarmclock" to "com.motorola.blur.alarmclock.AlarmClock",
    "com.oneplus.deskclock" to "com.oneplus.deskclock.DeskClock",
    "com.sec.android.app.alarm" to "com.sec.android.app.alarm.AlarmList",
    "com.sec.android.app.clockpackage" to "com.sec.android.app.clockpackage.ClockPackage",
    "com.sonyericsson.organizer" to "com.sonyericsson.organizer.Organizer",
)

/** Кандидаты «пакет → активити» системного календаря для разных вендоров. */
private val CALENDAR_APP_ACTIVITIES = listOf(
    "com.android.calendar" to "com.android.calendar.LaunchActivity",
    "com.android.calendar" to "com.android.calendar.homepage.AllInOneActivity",
    "com.google.android.calendar" to "com.android.calendar.LaunchActivity",
    "com.htc.calendar" to "com.htc.calendar.LaunchActivity",
    "com.htc.calendar" to "com.htc.calendar.CalendarActivityMain",
    "com.lenovo.app.Calendar" to "com.lenovo.app.Calendar.MonthActivityNew",
    "com.motorola.calendar" to "com.android.calendar.LaunchActivity",
    "com.samsung.android.calendar" to "com.android.calendar.LaunchActivity",
    "com.samsung.android.calendar" to "com.samsung.android.app.calendar.activity.MainActivity",
    "com.sonymobile.calendar" to "com.sonymobile.calendar.LaunchActivity",
)

/**
 * Первый явный intent (пакет + активити), для которого нашёлся обработчик;
 * иначе — фолбэк на экран настроек.
 */
private fun explicitAppAction(context: Context, candidates: List<Pair<String, String>>): Action {
    val packageManager = context.packageManager
    for ((pkg, activity) in candidates) {
        val intent = Intent().setClassName(pkg, activity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (packageManager.queryIntentActivities(intent, 0).isNotEmpty()) {
            return actionStartActivity(intent)
        }
    }
    return actionStartActivity(Intent(context, MainActivity::class.java))
}

/** Открывает системное приложение часов; фолбэк — экран настроек. */
internal fun clockAppAction(context: Context): Action = explicitAppAction(context, CLOCK_APP_ACTIVITIES)

/** Открывает системный календарь; фолбэк — экран настроек. */
internal fun calendarAppAction(context: Context): Action = explicitAppAction(context, CALENDAR_APP_ACTIVITIES)
