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
 * Двухуровневый виджет 2 × 2: на верхнем этаже время (часы и минуты),
 * на нижнем — в один ряд день недели и дата.
 * Клик по времени открывает системные часы, по дате — календарь.
 */
class TwoLevelWidget(
    private var preview: WidgetSettings? = null,
) : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = preview ?: WidgetSettingsStore(context).read()
        val openSettings = actionStartActivity(Intent(context, MainActivity::class.java))
        val openClockApp = clockAppAction(context)
        val openCalendarApp = calendarAppAction(context)
        provideContent {
            TwoLevelWidgetContent(
                initialSettings = settings,
                openSettings = openSettings,
                openClockApp = openClockApp,
                openCalendarApp = openCalendarApp,
            )
        }
    }
}

/**
 * Контент двухуровневого виджета.
 *
 * Пропорции и отступы заданы в [Structure.TwoLevel]: размер клетки и
 * сетки вычисляются [Structure.resolve] из доступной области; сверху —
 * время, снизу в один ряд день недели и дата (размеры ячеек — клетка
 * × веса частей).
 */
@Composable
private fun TwoLevelWidgetContent(
    initialSettings: WidgetSettings,
    openSettings: Action,
    openClockApp: Action,
    openCalendarApp: Action,
) {
    val settings = rememberWidgetSettings(initialSettings)
    val available = LocalSize.current
    val structure = Structure.TwoLevel
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
        Column {
            TimeSection(settings, structure, cellSize, onClick = openClockApp)
            Row {
                WeekdaySection(settings, part = structure.weekday, cellSize, onClick = openCalendarApp)
                DateSection(settings, structure, cellSize, onClick = openCalendarApp)
            }
        }
    }
}
