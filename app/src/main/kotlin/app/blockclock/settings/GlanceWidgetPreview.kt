package app.blockclock.settings

import android.widget.FrameLayout
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
 *
 * Updates patch the shown view in place via reapply; when the widget
 * structure changes (e.g. gap 0 ↔ > 0) the actions target a stale tree and
 * throw [RemoteViews.ActionException], so it falls back to a full rebuild.
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
        remoteViews?.let { views ->
            AndroidView(
                factory = { context -> FrameLayout(context).set(views) },
                update = { container ->
                    try {
                        views.reapply(container.context.applicationContext, container.getChildAt(0))
                    } catch (_: RemoteViews.ActionException) {
                        container.set(views)
                    }
                },
            )
        } ?: CircularProgressIndicator()
    }
}

/** Replaces the container content with a freshly applied [views] root. */
private fun FrameLayout.set(views: RemoteViews): FrameLayout {
    removeAllViews()
    val view = views.apply(context.applicationContext, this)
    addView(view)
    return this
}
