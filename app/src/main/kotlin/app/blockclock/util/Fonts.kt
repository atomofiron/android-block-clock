package app.blockclock.util

import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.graphics.fonts.FontStyle
import android.graphics.fonts.FontVariationAxis
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

/** The font file with the [WidgetFont.variations] applied on top of the font settings. */
@RequiresApi(Q)
private fun WidgetFont.variableFont(): Font = when {
    variations.isEmpty() -> font
    else -> Font.Builder(file)
        .setTtcIndex(ttcIndex)
        .setFontVariationSettings(variationAxes().toAxes())
        .build()
}

/** The settings of the font itself with the [WidgetFont.variations] applied on top. */
@RequiresApi(Q)
private fun WidgetFont.variationAxes(): Map<String, Float> = font.axes.orEmpty()
    .associate { it.tag to it.styleValue } + variations

@RequiresApi(Q)
private fun Map<String, Float>.toAxes(): Array<FontVariationAxis> = map { FontVariationAxis(it.key, it.value) }.toTypedArray()

/**
 * The typeface for the in-process preview of a font the system does not name: the font with
 * the system fallback attached. The host cannot use it — a `Typeface` does not survive the
 * transfer to the launcher process (see [CellFont]).
 */
@RequiresApi(Q)
fun WidgetFont.toTypeface(): Typeface? = try {
    Typeface.CustomFallbackBuilder(FontFamily.Builder(variableFont()).build())
        .setSystemFallback("sans-serif")
        .build()
} catch (_: IOException) {
    null
}

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
 * itself, the style bits, the variable font axes and the typeface for the in-process
 * preview.
 *
 * A `Typeface` cannot be passed to the launcher process: `TypefaceSpan` writes it into
 * `android.graphics.LeakyTypefaceStorage`, a per-process list, and the host reads null out
 * of it and silently draws the default font. The family name survives the transfer instead,
 * so the host builds the font itself — that is what [CellFont.family] is for, see
 * [FontConfig]. A file the system does not name has no family to send: it keeps its
 * [CellFont.typeface], which the settings preview applies in its own process, and the host
 * draws the default font.
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
fun WidgetFont.toCellFont(): CellFont {
    val weight = variations[WEIGHT_TAG]?.toInt() ?: font.style.weight
    val names = listOfNotNull(FontConfig.family(file), family)
    val alias = WEIGHT_ALIASES[weight]
        ?.takeIf { WEIGHT_TAG !in variations }
        ?.let { suffix -> names.firstNotNullOfOrNull { "$it$suffix".takeIf(::isSystemFamily) } }
    val name = alias ?: names.firstOrNull(::isSystemFamily)
    val axes = when {
        name == null -> emptyMap()
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
        typeface = name?.let { null } ?: toTypeface(),
    )
}

/**
 * True when the system has a family with this [name]. The typeface reports the name it was
 * created from, and an unknown name silently falls back to the default typeface and reports
 * that one instead.
 */
private fun isSystemFamily(name: String): Boolean = Android.P &&
    Typeface.create(name, Typeface.NORMAL).systemFontFamilyName == name

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
