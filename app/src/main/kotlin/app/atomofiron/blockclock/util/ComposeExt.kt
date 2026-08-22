package app.atomofiron.blockclock.util

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.glance.GlanceModifier
import androidx.glance.layout.size

@Composable
fun Modifier.statusBarAndCutout() = windowInsetsPadding(WindowInsets.run { statusBars.union(displayCutout) })

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
