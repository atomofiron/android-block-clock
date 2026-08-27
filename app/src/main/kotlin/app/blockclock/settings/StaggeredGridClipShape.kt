package app.blockclock.settings

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * The clip of the scrolling settings block: the top edge is segmented —
 * one rounded "bump" per grid column, with padding between the segments
 * and along the outline; the bottom and the sides are straight.
 *
 * @property columns the current LazyVerticalStaggeredGrid column count.
 * @property padding the cutout width between segments and the outline offset.
 * @property cornerRadius the segment corner radius.
 */
class StaggeredGridClipShape(
    val columns: Int,
    val padding: Dp,
    val cornerRadius: Dp,
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val radius = density.run { cornerRadius.toPx() }
        val padding = density.run { padding.toPx() }

        val long = radius * 11 / 20
        val short = radius - long
        val path = Path()
        path.moveTo(0f, radius)
        path.relativeCubicTo(0f, -long, short, -radius, radius, -radius)

        val step = (size.width - radius * 2 - (padding + radius * 2) * columns.dec()) / columns
        path.relativeLineTo(step, 0f)
        repeat(columns.dec()) {
            path.relativeCubicTo(long, 0f, radius, short, radius, radius)
            path.relativeLineTo(padding / 2, padding / 2)
            path.relativeLineTo(padding / 2, -padding / 2)
            path.relativeCubicTo(0f, -long, short, -radius, radius, -radius)
            path.relativeLineTo(step, 0f)
        }

        path.relativeCubicTo(long, 0f, radius, short, radius, radius)
        path.relativeLineTo(padding, padding)
        path.relativeLineTo(0f, size.height)
        path.relativeLineTo(-size.width - padding * 2, 0f)
        path.relativeLineTo(0f, -size.height)
        path.relativeLineTo(padding, -padding)

        return Outline.Generic(path)
    }
}
