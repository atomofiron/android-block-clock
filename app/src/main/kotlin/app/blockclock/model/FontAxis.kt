package app.blockclock.model

/**
 * A variable font axis declared in the `fvar` table of the font file:
 * the [tag] and the values the font itself declares for it.
 */
data class FontAxis(
    val tag: String,
    val min: Float,
    val default: Float,
    val max: Float,
) {
    val range: ClosedFloatingPointRange<Float> get() = min..max
}
