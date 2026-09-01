package app.blockclock.model

import android.app.WallpaperColors
import android.os.Build.VERSION_CODES.O_MR1
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import app.blockclock.util.toComposeColor

data class WallpaperColors(
    val primary: Color,
    val secondary: Color?,
    val tertiary: Color?,
) {
    @RequiresApi(O_MR1)
    constructor(colors: WallpaperColors) : this(
        colors.primaryColor.toComposeColor(),
        colors.secondaryColor?.toComposeColor(),
        colors.tertiaryColor?.toComposeColor(),
    )
}