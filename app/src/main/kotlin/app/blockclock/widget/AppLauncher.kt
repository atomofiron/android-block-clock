package app.blockclock.widget

import android.content.Context
import android.content.Intent
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartActivity
import app.blockclock.MainActivity

/**
 * Candidate "package → activity" pairs of the system clock app for
 * different vendors. Enumerated like launchers do: each manufacturer
 * has its own names.
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

/** Candidate "package → activity" pairs of the system calendar for different vendors. */
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
 * The first explicit intent (package + activity) that has a handler;
 * otherwise — a fallback to the settings screen.
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

/** Opens the system clock app; falls back to the settings screen. */
internal fun clockAppAction(context: Context): Action = explicitAppAction(context, CLOCK_APP_ACTIVITIES)

/** Opens the system calendar; falls back to the settings screen. */
internal fun calendarAppAction(context: Context): Action = explicitAppAction(context, CALENDAR_APP_ACTIVITIES)
