package app.blockclock.widget

import androidx.annotation.LayoutRes
import app.blockclock.R

/**
 * A time/date part rendered by a native [android.widget.TextClock] through
 * AndroidRemoteViews. TextClock updates the text every minute by itself,
 * without running any app code.
 */
enum class ClockTextPart(
    val format24: String,
    val format12: String = format24,
) {
    HOURS("HH", "hh"),
    MINUTES("mm"),
    WEEKDAY("EEEE"),
    DAY("d"),
    MONTH("M"),
    YEAR("yyyy"),
}
