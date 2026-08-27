package app.blockclock.widget

import androidx.annotation.LayoutRes
import app.blockclock.R

/**
 * A time/date part rendered by a native [android.widget.TextClock] through
 * AndroidRemoteViews. TextClock updates the text every minute by itself,
 * without running any app code.
 */
enum class ClockTextPart(@LayoutRes val layoutRes: Int) {
    HOURS(R.layout.text_clock_hours),
    MINUTES(R.layout.text_clock_minutes),
    WEEKDAY(R.layout.text_clock_weekday),
    DAY(R.layout.text_clock_day),
    MONTH(R.layout.text_clock_month),
    YEAR(R.layout.text_clock_year),
}
