package app.blockclock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

@Composable
fun rememberVerticalSpacing(space: Dp) = remember { VerticalSpacing(space) }

fun Modifier.apply(arrangement: VerticalSpacing): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height - arrangement.subtraction) {
        placeable.place(0, 0)
    }
}

class VerticalSpacing(private val space: Dp) : Arrangement.Vertical {

    override val spacing: Dp = space

    var subtraction: Int = 0
        private set

    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        outPositions: IntArray,
    ) {
        val spacePx = space.roundToPx()
        var y = 0
        subtraction = 0
        sizes.forEachIndexed { index, size ->
            if (size > 0 && y > 0) {
                y += spacePx
            }
            if (size <= 0) {
                subtraction += spacePx
            }
            outPositions[index] = y
            y += size
        }
    }
}
