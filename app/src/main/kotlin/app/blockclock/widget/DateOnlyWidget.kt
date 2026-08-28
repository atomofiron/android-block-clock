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
import androidx.glance.layout.Column
import androidx.glance.layout.wrapContentSize

/**
 * The "date only" widget: 2 × 1 home screen cells — weekday, day, month
 * and year. A tap opens the system calendar.
 */
class DateOnlyWidget(
    private var preview: WidgetSettings? = null,
) : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = preview ?: WidgetSettingsStore(context).read()
        val openCalendarApp = settings.calendarApp.launchAction(context, defaultCalendarApp(context))
        provideContent {
            DateOnlyWidgetContent(initialSettings = settings, openCalendarApp = openCalendarApp)
        }
    }
}

@Composable
private fun DateOnlyWidgetContent(
    initialSettings: WidgetSettings,
    openCalendarApp: Action,
) {
    val settings = rememberWidgetSettings(initialSettings)
    val available = LocalSize.current
    val structure = Structure.DateOnly
    val (cellSize, gridSize) = structure.resolve(available, settings.gapDp.dp)
    Box(
        modifier = GlanceModifier
            .wrapContentSize()
            .clickable(openCalendarApp),
        contentAlignment = Alignment.Center,
    ) {
        if (settings.gapDp == 0) {
            CellBackground(settings.effectiveRectColor, settings.cornerRadiusDp, gridSize)
        }
        Column {
            WeekdaySection(settings = settings, part = structure.weekday, cellSize, onClick = openCalendarApp)
            DateSection(settings = settings, structure, cellSize, onClick = openCalendarApp)
        }
    }
}
