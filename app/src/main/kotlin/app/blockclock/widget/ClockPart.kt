package app.blockclock.widget

/**
 * A time/date part rendered by a native [android.widget.TextClock] through
 * AndroidRemoteViews. TextClock updates the text every minute by itself,
 * without running any app code.
 */
enum class ClockPart(
    val format24: String,
    val format12: String = format24,
) {
    HOURS("HH", "hh"),
    MINUTES("mm"),
    WEEKDAY("EEEE"),
    AM_PM_WEEKDAY("a   EEEE"),
    DAY("d"),
    MONTH("M"),
    YEAR("yyyy"),
}
