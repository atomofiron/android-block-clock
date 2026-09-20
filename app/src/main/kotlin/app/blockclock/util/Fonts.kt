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
import app.blockclock.model.WidgetFont
import java.io.File
import java.io.IOException

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
 * The weight of the file goes to a family alias when the platform declares one, otherwise
 * to the bold bit and to the axes: the alias weight is applied by the host when it builds
 * the font, the bold bit picks a heavier face of the family, and the axes pick a weight
 * inside a variable font.
 *
 * The axes reach the host only from Android 15 on: `RemoteViews` calls a method of a widget
 * view only when the platform marks it as `@RemotableViewMethod`, and
 * `TextView.setFontVariationSettings` gets the mark there (it is missing from 9 to 14, where
 * the call would fail the whole RemoteViews). On the older versions the weight comes from
 * the family alias and the style bits only.
 */
@RequiresApi(Q)
fun WidgetFont.toCellFont(): CellFont? {
    val weight = variations[WEIGHT_TAG]?.toInt() ?: font.style.weight
    val names = listOfNotNull(FontConfig.family(file), family)
    val alias = WEIGHT_ALIASES[weight]
        ?.takeIf { WEIGHT_TAG !in variations }
        ?.let { suffix -> names.firstNotNullOfOrNull { "$it$suffix".takeIf(::isSystemFamily) } }
    val name = alias ?: names.firstOrNull(::isSystemFamily) ?: return null
    val axes = when {
        alias == null -> variations + (WEIGHT_TAG to weight.toFloat())
        else -> variations
    }
    return CellFont(
        family = name,
        style = italic() or when {
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
private fun Map<String, Float>.toSettings(): String = entries.joinToString(", ") { "'${it.key}' ${it.value}" }

/** The axis of the weight; a variable font picks the weight from it. */
private const val WEIGHT_TAG = "wght"

/** The weight the bold bit asks for; `TypefaceSpan` adds it to the weight of the family. */
private const val BOLD_WEIGHT = 600

/** The family suffixes for the weights the platform declares aliases for. */
private val WEIGHT_ALIASES = mapOf(
    100 to "-thin",
    300 to "-light",
    500 to "-medium",
    700 to "-bold",
    900 to "-black",
)
