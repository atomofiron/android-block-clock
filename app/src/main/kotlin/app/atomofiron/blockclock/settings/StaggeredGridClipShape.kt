package app.atomofiron.blockclock.settings

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * Клип скроллящегося блока настроек: верхняя кромка сегментирована —
 * по одному скруглённому «горбу» на каждую колонку грида, между сегментами
 * и по контуру — отступ; низ и бока прямые.
 *
 * @property columns текущее количество колонок LazyVerticalStaggeredGrid.
 * @property padding ширина выреза между сегментами и отступ контура от края.
 * @property cornerRadius радиус скругления углов сегментов.
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
