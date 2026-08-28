package app.blockclock.util

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.glance.GlanceModifier
import androidx.glance.layout.size

operator fun WindowInsets.plus(other: WindowInsets) = union(other)

fun WindowInsets.horizontal() = only(WindowInsetsSides.Horizontal)

val WindowInsets.Companion.Empty get() = WindowInsets()

inline operator fun WindowInsets.Companion.invoke(block: WindowInsets.Companion.() -> WindowInsets) = block()

@Composable
fun Modifier.windowInsetsPadding(action: @Composable WindowInsets.Companion.() -> WindowInsets): Modifier {
    return windowInsetsPadding(WindowInsets.action())
}

@Composable
fun animatedBackgroundColor(transparent: Boolean): Color {
    val background = MaterialTheme.colorScheme.background
    var targetBackgroundColor by remember { mutableStateOf(background) }
    val backgroundColor by animateColorAsState(targetBackgroundColor)
    LaunchedEffect(background, transparent) {
        targetBackgroundColor = if (transparent) Color.Transparent else background
    }
    return backgroundColor
}

fun GlanceModifier.size(size: DpSize) = size(width = size.width, height = size.height)

fun Offset.calcSize(right: Float, bottom: Float): Size {
    return Size(right - x, bottom - y)
}

@Composable
fun rememberAppIconPainter(packageName: String?): Painter {
    val context = LocalContext.current
    return remember(packageName) {
        context.appIcon(packageName)
            ?.toPainter()
            ?: ColorPainter(Color.Transparent)
    }
}

fun Context.appIcon(packageName: String?): Drawable? {
    packageName ?: return null
    return packageManager.getApplicationIcon(packageName)
}

fun Drawable?.toPainter(): Painter {
    this ?: return ColorPainter(Color.Transparent)
    return object : Painter() {
        override val intrinsicSize: Size
            get() = Size(
                intrinsicWidth.toFloat().takeIf { it > 0 } ?: Size.Unspecified.width,
                intrinsicHeight.toFloat().takeIf { it > 0 } ?: Size.Unspecified.height,
            )

        override fun DrawScope.onDraw() {
            setBounds(0, 0, size.width.toInt(), size.height.toInt())
            draw(drawContext.canvas.nativeCanvas)
        }
    }
}
