package app.blockclock.model

import android.graphics.Typeface

/**
 * The style of the widget text of the default font. A picked font brings its own style
 * from the file, so the bits are used only while the default font is on.
 */
enum class TextStyle {
    Normal,
    Bold,
    Italic,
    ;

    /** The `Typeface` style bits for the text span. */
    val bits: Int get() = when (this) {
        Normal -> Typeface.NORMAL
        Bold -> Typeface.BOLD
        Italic -> Typeface.ITALIC
    }

    companion object {
        fun from(name: String?): TextStyle = entries
            .takeIf { name != null }
            ?.find { it.name == name }
            ?: Normal
    }
}
