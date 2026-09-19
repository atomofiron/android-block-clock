package app.blockclock.model

import android.graphics.fonts.Font
import android.os.Build.VERSION_CODES.Q
import androidx.annotation.RequiresApi
import java.io.File

/**
 * A system font picked for the widget: the font file path, the index of
 * the font inside a collection file and the variable font axis values.
 * A null [path] is a font built from a buffer — it cannot be restored.
 */
@RequiresApi(Q)
data class WidgetFont(
    val font: Font,
    val file: File,
    val ttcIndex: Int = 0,
    /** The axes declared in the font file; empty for a static font. */
    val axes: List<FontAxis> = emptyList(),
    val variations: Map<String, Float> = emptyMap(),
) {
    /** True for a variable font. */
    val vf: Boolean get() = axes.isNotEmpty()

    /** The font file name, e.g. `Roboto-Bold.ttf`. */
    val name: String = file.name.substringBeforeLast('.')

    val path: String = file.absolutePath

    /** The family part of the file name: `Roboto` for `Roboto-Bold.ttf`. */
    val family: String get() = name.substringBefore(FAMILY_SEPARATOR)

    /** The style part of the file name: `Bold` for `Roboto-Bold.ttf`. */
    val style: String get() = name.substringAfter(FAMILY_SEPARATOR, "")

    fun variation(tag: String, fallback: Float): Float = variations[tag] ?: fallback

    companion object {
        const val FAMILY_SEPARATOR = '-'
    }
}
