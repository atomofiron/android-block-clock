package app.blockclock.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import app.blockclock.widget.ClockWidget
import app.blockclock.widget.Structure
import app.blockclock.widget.WidgetSettings
import app.blockclock.widget.resolve
import kotlin.math.min

@Composable
internal fun WidgetPreview(
    modifier: Modifier = Modifier,
    settings: WidgetSettings,
) {
    val width = previewWidth()
    val available = DpSize(width, width)
    val (_, gridSize) = Structure.OneLevel.resolve(available, 0.dp) // fixed max possible height
    GlanceWidgetPreview(
        modifier = modifier,
        widget = ClockWidget(settings),
        previewSize = gridSize,
    )
}

@Composable
internal fun previewWidth(): Dp {
    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize
    val minWindowSide = with(density) { min(windowSize.width, windowSize.height).toDp() }
    return (minWindowSide - Padding.Common * 2).coerceAtLeast(0.dp)
}
