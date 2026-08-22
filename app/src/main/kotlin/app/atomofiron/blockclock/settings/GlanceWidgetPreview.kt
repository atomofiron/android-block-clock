package app.atomofiron.blockclock.settings

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

/**
 * Рендерит Glance-виджет в Compose: запускает его композицию с размером
 * [previewSize] (этот размер видит LocalSize внутри виджета) и показывает
 * полученные RemoteViews. При изменении [refreshKey] композиция
 * запускается заново.
 */
@Composable
fun GlanceWidgetPreview(
    modifier: Modifier = Modifier,
    widget: GlanceAppWidget,
    previewSize: DpSize,
    refreshKey: Any?,
) {
    val context = LocalContext.current
    var remoteViews by remember { mutableStateOf<RemoteViews?>(null) }

    LaunchedEffect(widget, refreshKey) {
        remoteViews = widget.compose(context, size = previewSize)
    }

    Box(
        modifier = modifier.size(previewSize),
        contentAlignment = Alignment.Center,
    ) {
        remoteViews?.run {
            AndroidView(
                factory = { ctx ->
                    val frameLayout = FrameLayout(ctx)
                    val view = apply(ctx.applicationContext, frameLayout)
                    frameLayout.addView(view)
                    frameLayout
                },
                update = { frameLayout ->
                    frameLayout.removeAllViews()
                    val view = apply(frameLayout.context.applicationContext, frameLayout)
                    frameLayout.addView(view)
                }
            )
        } ?: CircularProgressIndicator()
    }
}
