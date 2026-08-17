package app.atomofiron.blockclock.widget

import androidx.annotation.LayoutRes
import app.atomofiron.blockclock.R

/**
 * Часть времени/даты, отображаемая нативным [android.widget.TextClock]
 * через AndroidRemoteViews. TextClock сам обновляет текст каждую минуту,
 * без запуска кода нашего приложения.
 */
enum class ClockTextPart(@LayoutRes val layoutRes: Int) {
    HOURS(R.layout.text_clock_hours),
    MINUTES(R.layout.text_clock_minutes),
    WEEKDAY(R.layout.text_clock_weekday),
    DAY(R.layout.text_clock_day),
    MONTH(R.layout.text_clock_month),
    YEAR(R.layout.text_clock_year),
}
