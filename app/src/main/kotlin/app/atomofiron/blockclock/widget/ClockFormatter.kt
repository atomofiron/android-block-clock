package app.atomofiron.blockclock.widget

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Значения, показываемые виджетом: часы, минуты, день недели,
 * день месяца, месяц и год.
 */
data class ClockParts(
    val hours: String,
    val minutes: String,
    val weekday: String,
    val day: String,
    val month: String,
    val year: String,
)

/**
 * Форматирование времени и даты.
 * Общий для Glance-виджета и Compose-предпросмотра в настройках,
 * чтобы они всегда отображали одно и то же.
 */
object ClockFormatter {

    fun parts(calendar: Calendar, use24Hour: Boolean): ClockParts = ClockParts(
        hours = format(calendar, if (use24Hour) "HH" else "hh"),
        minutes = format(calendar, "mm"),
        weekday = format(calendar, "EEEE"),
        day = format(calendar, "dd"),
        month = format(calendar, "MM"),
        year = format(calendar, "yyyy"),
    )

    /**
     * Порядок «день/месяц» согласно настройке:
     * при [dayFirst] = true — сначала день, иначе сначала месяц.
     */
    fun dayMonth(parts: ClockParts, dayFirst: Boolean): Pair<String, String> =
        if (dayFirst) parts.day to parts.month else parts.month to parts.day

    private fun format(calendar: Calendar, pattern: String): String =
        SimpleDateFormat(pattern, Locale.getDefault()).format(calendar.time)
}
