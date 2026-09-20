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

        /** The slant axes: the host gets them as a single italic bit and nothing finer. */
        val SLANTS = listOf("slnt", "ital")

        /**
         * The axes the sliders offer: the ones a style bit expresses, so the only ones the
         * widget renders. The rest of the axes of a file reach the host as the value of
         * `TextView.setFontVariationSettings` ([app.blockclock.util.toCellFont]), a channel the
         * host drops: the method keeps the value in a `Typeface` it builds itself, and the family
         * arrives as a `TypefaceSpan`, which calls `setTypeface` right after and clears it
         * ([android.graphics.Paint.setFontVariationSettings] documents that).
         */
        val DELIVERED = listOf(WEIGHT) + SLANTS
    }
}
