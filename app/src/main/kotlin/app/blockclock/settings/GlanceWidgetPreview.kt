package app.blockclock.settings

import android.widget.RemoteViews
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.compose
import app.blockclock.widget.WidgetSettings

/**
 * Renders a Glance widget in Compose: runs its composition with the [size]
 * (this is what LocalSize sees inside the widget) and shows the produced
 * RemoteViews.
 */
@Composable
fun GlanceWidgetPreview(
    modifier: Modifier = Modifier,
    widget: GlanceAppWidget,
    size: DpSize,
    settings: WidgetSettings? = null,
) {
    val context = LocalContext.current
    var remoteViews by remember { mutableStateOf<RemoteViews?>(null) }

    LaunchedEffect(widget, settings) {
        remoteViews = widget.compose(context, size = size)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        remoteViews?.run {
            AndroidView(
                factory = { context -> apply(context.applicationContext, null) },
                update = { layout -> reapply(layout.context.applicationContext, layout) },
            )
        } ?: CircularProgressIndicator()
    }
}
