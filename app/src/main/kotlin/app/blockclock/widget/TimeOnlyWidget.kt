package app.blockclock.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.wrapContentSize

/**
 * The "time only" widget: 2 × 1 home screen cells, hours and minutes.
 * A tap opens the system clock app.
 */
class TimeOnlyWidget(
    private var preview: WidgetSettings? = null,
) : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = preview ?: WidgetSettingsStore(context).read()
        val openClockApp = clockAppAction(context)
        provideContent {
            TimeOnlyWidgetContent(initialSettings = settings, openClockApp = openClockApp)
        }
    }
}

@Composable
private fun TimeOnlyWidgetContent(
    initialSettings: WidgetSettings,
    openClockApp: Action,
) {
    val settings = rememberWidgetSettings(initialSettings)
    val available = LocalSize.current
    val structure = Structure.TimeOnly
    val (cellSize, gridSize) = structure.resolve(available, settings.gapDp.dp)
    Box(
        modifier = GlanceModifier
            .wrapContentSize()
            .clickable(openClockApp),
        contentAlignment = Alignment.Center,
    ) {
        if (settings.gapDp == 0) {
            CellBackground(settings.effectiveRectColor, settings.cornerRadiusDp, gridSize)
        }
        Row {
            TimeSection(settings, structure, cellSize, onClick = openClockApp)
        }
    }
}
