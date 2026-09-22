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

    companion object {
        /** The weight axis: the host gets its value through a family alias or the bold bit. */
        const val WEIGHT = "wght"

        /** The slant axis: the angle of the lean, in degrees. */
        const val SLANT = "slnt"

        /** The italic axis: a flag rather than an angle, the coarser of the two slants. */
        const val ITALIC = "ital"

        /** The slant axes: the host gets them as a single italic bit and nothing finer. */
        val SLANTS = listOf(SLANT, ITALIC)

        /**
         * The axes the sliders offer: the ones a style bit expresses, so the only ones the
         * widget renders. The rest of the axes of a file have no channel to the host at all: the
         * family arrives as a `TypefaceSpan`, which builds its own typeface and clears the
         * variation instance the axes would have built
         * ([android.graphics.Paint.setFontVariationSettings] documents that).
         */
        val DELIVERED = listOf(WEIGHT) + SLANTS
    }
}
