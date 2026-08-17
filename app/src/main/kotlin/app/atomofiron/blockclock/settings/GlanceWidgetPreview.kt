package app.atomofiron.blockclock.settings
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.compose

/**
 * Рендерит Glance-виджет в Compose: запускает его композицию один
 * раз и показывает полученные RemoteViews. [previewSize] задаёт
 * размер области (LocalSize внутри виджета). При изменении [refreshKey]
 * виджет перерисовывается, а предыдущий кадр виден до готовности нового.
 */
@Composable
fun GlanceWidgetPreview(
    widget: GlanceAppWidget,
    modifier: Modifier = Modifier,
    previewSize: DpSize,
    refreshKey: Any? = null,
) {
    val context = LocalContext.current
    var remoteViews by remember { mutableStateOf<RemoteViews?>(null) }

    LaunchedEffect(widget, refreshKey) {
        remoteViews = widget.compose(context, size = previewSize)
    }

    Box(modifier = modifier) {
        remoteViews?.let { rv ->
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val frameLayout = FrameLayout(ctx)
                    val view = rv.apply(ctx.applicationContext, frameLayout)
                    frameLayout.addView(view)
                    frameLayout
                },
                update = { frameLayout ->
                    frameLayout.removeAllViews()
                    val view = rv.apply(frameLayout.context.applicationContext, frameLayout)
                    frameLayout.addView(view)
                }
            )
        }
    }
}
