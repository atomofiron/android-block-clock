package app.atomofiron.blockclock.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
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
import androidx.glance.layout.fillMaxSize

/**
 * Виджет «только дата»: 2 × 1 клеток рабочего стола —
 * день недели, день, месяц и год. Клик открывает системный календарь.
 */
class DateOnlyWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = WidgetSettingsStore.load(context)
        val openCalendarApp = calendarAppAction(context)
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
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(openCalendarApp),
        contentAlignment = Alignment.Center,
    ) {
        if (settings.gapDp == 0) {
            CellBackground(settings.effectiveRectColor, settings.cornerRadiusDp, letterboxSize(available, 2f))
        }
        val grid = letterboxSize(available, ratio = SECTION_ASPECT)
        Column {
            WeekdaySection(settings = settings, area = DpSize(grid.width, grid.height / 2), onClick = openCalendarApp)
            DateSection(settings = settings, area = DpSize(grid.width, grid.height / 2), onClick = openCalendarApp)
        }
    }
}
