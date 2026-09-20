package app.blockclock.util

import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontStyle
import android.os.Build.VERSION_CODES.Q
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.font.Font as ComposeFont
import androidx.compose.ui.text.font.FontFamily as ComposeFontFamily
import androidx.compose.ui.text.font.FontVariation
import app.blockclock.model.CellFont
import app.blockclock.model.FontAxis
import app.blockclock.model.WidgetFont
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt

@RequiresApi(Q)
fun Font.toWidgetFont(): WidgetFont? {
    val file = file ?: return null
    return WidgetFont(
        font = this,
        file = file,
        ttcIndex = ttcIndex,
        axes = readAxes(file, ttcIndex),
    )
}

/** The widget font of the file [path]: the font restored from the settings. */
@RequiresApi(Q)
fun widgetFont(
    path: String,
    ttcIndex: Int,
    variations: Map<String, Float>,
): WidgetFont? = try {
    Font.Builder(File(path))
        .setTtcIndex(ttcIndex)
        .build()
        .toWidgetFont()
        ?.copy(variations = variations)
} catch (_: IOException) {
    null
}

/** The system fonts of the same family as the [font], sorted by the style name. */
@RequiresApi(Q)
fun List<WidgetFont>.familyStyles(font: WidgetFont): List<WidgetFont> = filter { it.family == font.family }
    .sortedBy { it.style }

/** The Compose font family for the settings preview. */
fun WidgetFont.toFontFamily(): ComposeFontFamily = ComposeFontFamily(
    ComposeFont(
        file = file,
        variationSettings = when {
            variations.isEmpty() -> FontVariation.Settings()
            else -> FontVariation.Settings(
                *variations.map { FontVariation.Setting(it.key, it.value) }.toTypedArray(),
            )
        },
    ),
)

/**
 * The font as the widget passes it to the home screen: the family name the host resolves
 * itself, the style bits and the variable font axes; null for a font the system does not name.
 *
 * A `Typeface` cannot be passed to the launcher process ([CellFont]), so the font travels as a
 * name the host builds itself, see [FontConfig]. A file the system does not name has none to
 * send — the configuration declares the vast majority of the fonts without a name — and the
 * host would draw its own default font for such a pick, ignoring the choice entirely: the
 * result is null, and [getSystemFonts] leaves those fonts out of the picker.
 *
 * The weight of the file goes to the alias the configuration declares for it when there is one
 * ([FontConfig.alias]), otherwise to the bold bit: the host applies the weight of the alias when
 * it builds the font, the bold bit picks a heavier face of the family. The axes of the file are passed as well, but the host
 * drops them: `TextView.setFontVariationSettings` keeps the value in a `Typeface` it builds
 * itself, and the family arrives as a `TypefaceSpan`, which calls `setTypeface` right after and
 * clears it ([android.graphics.Paint.setFontVariationSettings] documents that). The slant is
 * the one axis a style bit expresses, see [slant].
 */
@RequiresApi(Q)
fun WidgetFont.toCellFont(): CellFont? {
    val weight = variations[FontAxis.WEIGHT]?.toInt() ?: font.style.weight
    val names = listOfNotNull(FontConfig.family(file), family)
    val family = names.firstOrNull(::isSystemFamily) ?: return null
    // The alias is the only way to ask the host for a weight of its own: the style bits carry the
    // bold bit alone, so the faces of a family that declares several weights look the same.
    val alias = FontConfig.alias(family, weight)
    val name = alias ?: family
    val axes = when {
        alias == null -> variations + (FontAxis.WEIGHT to weight.toFloat())
        else -> variations
    }
    return CellFont(
        family = name,
        style = italic() or slant() or when {
            alias != null -> Typeface.NORMAL // the alias carries the weight
            weight >= BOLD_WEIGHT -> Typeface.BOLD
            else -> Typeface.NORMAL
        },
        variationSettings = axes.takeIf { it.isNotEmpty() && Android.V }?.toSettings(),
    )
}

/**
 * True when the system declares a family with this [name], or an alias of one. The names are
 * read from the configurations of the device ([FontConfig]), because no API answers before
 * the 31st level: `Typeface.getSystemFontFamilyName` appears there, and calling it on the
 * older versions fails the lookup instead of answering.
 */
private fun isSystemFamily(name: String): Boolean = Android.Q && FontConfig.canResolve(name)

/** The italic bit of the file. */
@RequiresApi(Q)
private fun WidgetFont.italic(): Int = when (font.style.slant) {
    FontStyle.FONT_SLANT_UPRIGHT -> Typeface.NORMAL
    else -> Typeface.ITALIC
}

/** The variable font axes in the `TextView.setFontVariationSettings` syntax. */
private fun Map<String, Float>.toSettings(): String = entries.joinToString(", ") { "'${it.key}' ${it.value.roundToInt()}" }

/**
 * The italic bit of the slant axes. The host gets a slant as the bit and as nothing finer: the
 * axes of the files are continuous, the bit is not, so any value but the upright one asks for
 * the italic face of the family, or for the synthetic slant when the family has no such face.
 */
@RequiresApi(Q)
private fun WidgetFont.slant(): Int = when {
    FontAxis.SLANTS.none { (variations[it] ?: 0f) != 0f } -> Typeface.NORMAL
    else -> Typeface.ITALIC
}

/** The weight the bold bit asks for; `TypefaceSpan` adds it to the weight of the family. */
private const val BOLD_WEIGHT = 600
