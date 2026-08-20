package app.atomofiron.blockclock.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
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
import androidx.glance.layout.fillMaxSize
import app.atomofiron.blockclock.MainActivity

/**
 * Двухуровневый виджет 2 × 2: на верхнем этаже время (часы и минуты),
 * на нижнем — в один ряд день недели и дата.
 * Клик по времени открывает системные часы, по дате — календарь.
 */
class TwoLevelWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = WidgetSettingsStore.load(context)
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
 * Пропорции: строка дня недели и даты — всегда 8:1 (ширина к высоте),
 * сам виджет при этом получается 8:5: высота = ширина/2 + ширина/8.
 * Сетка вписывается в доступную область по fitCenter.
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
    val gridWidth = minOf(available.width, available.height * 8f / 5f)
    val gridHeight = gridWidth * 5f / 8f
    val bottom = gridWidth / 8

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(openSettings),
        contentAlignment = Alignment.Center,
    ) {
        if (settings.gapDp == 0) {
            CellBackground(settings.effectiveRectColor, settings.cornerRadiusDp, DpSize(gridWidth, gridHeight))
        }
        Column {
            TimeSection(settings = settings, area = DpSize(gridWidth, gridWidth / 2), onClick = openClockApp)
            Row {
                WeekdaySection(settings = settings, area = DpSize(gridWidth / 2, bottom), onClick = openCalendarApp)
                DateSection(settings = settings, area = DpSize(gridWidth / 2, bottom), onClick = openCalendarApp)
            }
        }
    }
}
