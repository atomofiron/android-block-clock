package app.blockclock.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.wrapContentSize
import app.blockclock.MainActivity

/**
 * Полный виджет «время + дата»: 4 × 1 клеток рабочего стола,
 * один уровень — время слева, день недели и дата справа.
 *
 * Цвет/прозрачность прямоугольников, отступы, цвет текста, формат
 * времени и порядок даты берутся из [WidgetSettingsStore].
 */
class OneLevelWidget(
    private var preview: WidgetSettings? = null,
) : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = preview ?: WidgetSettingsStore.load(context)
        val openSettings = actionStartActivity(Intent(context, MainActivity::class.java))
        val openClockApp = clockAppAction(context)
        val openCalendarApp = calendarAppAction(context)
        provideContent {
            OneLevelWidgetContent(
                settings = settings,
                openSettings = openSettings,
                openClockApp = openClockApp,
                openCalendarApp = openCalendarApp,
            )
        }
    }
}

@Composable
private fun OneLevelWidgetContent(
    settings: WidgetSettings,
    openSettings: Action,
    openClockApp: Action,
    openCalendarApp: Action,
) {
    val settings = rememberWidgetSettings(settings)
    val available = LocalSize.current
    val structure = Structure.OneLevel
    val (cellSize, gridSize) = structure.resolve(available, settings.gapDp.dp)

    Box(
        modifier = GlanceModifier
            .wrapContentSize()
            .clickable(openSettings),
        contentAlignment = Alignment.Center,
    ) {
        if (settings.gapDp == 0) {
            CellBackground(settings.effectiveRectColor, settings.cornerRadiusDp, gridSize)
        }
        Row {
            TimeSection(settings, structure, cellSize, onClick = openClockApp)
            Column {
                WeekdaySection(settings, part = structure.weekday, cellSize = cellSize, onClick = openCalendarApp)
                DateSection(settings, structure, cellSize, onClick = openCalendarApp)
            }
        }
    }
}
