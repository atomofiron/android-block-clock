package app.blockclock.model

import android.graphics.Typeface
import android.os.Build.VERSION_CODES.Q
import androidx.annotation.RequiresApi

/**
 * The font of a widget cell as the home screen sees it: the family name the host resolves
 * itself, the style bits, the variable font axes and the typeface for the in-process
 * preview.
 *
 * The typeface is the fallback for the preview of a font the system does not name: a
 * `Typeface` does not survive the transfer to the launcher process, so the host would draw
 * the default font. The host builds the font from [family] and [variationSettings] instead;
 * the preview applies the RemoteViews in its own process, so a file the system does not name
 * keeps rendering there.
 */
@RequiresApi(Q)
data class CellFont(
    val family: String?,
    /** The `Typeface.BOLD`/`Typeface.ITALIC` bits; a family alias carries the weight itself. */
    val style: Int,
    /** The variable font axes in the `TextView.setFontVariationSettings` syntax. */
    val variationSettings: String?,
    /** The typeface of the file; the host cannot use it, the preview can. */
    val typeface: Typeface?,
)
