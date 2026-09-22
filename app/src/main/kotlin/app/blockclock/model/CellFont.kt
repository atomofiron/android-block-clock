package app.blockclock.model

import android.os.Build.VERSION_CODES.Q
import androidx.annotation.RequiresApi

/**
 * The font of a widget cell as the home screen sees it: the family name the host resolves
 * itself and the style bits.
 *
 * The font travels as [family], because a `Typeface` does not survive the transfer to the
 * launcher process: `TypefaceSpan` writes it into `android.graphics.LeakyTypefaceStorage`, a
 * per-process list, and the host reads nothing out of it and silently draws its own default
 * font. A file the system does not name has no name to send — the configuration declares most
 * of the fonts without one — so [app.blockclock.util.toCellFont] answers with null for it, the
 * picker leaves those fonts out ([app.blockclock.util.getSystemFonts]) and the cells draw the
 * default font of the host.
 */
@RequiresApi(Q)
data class CellFont(
    /** The family name the host resolves itself. */
    val family: String,
    /** The `Typeface.BOLD`/`Typeface.ITALIC` bits; a family alias carries the weight itself. */
    val style: Int,
)
