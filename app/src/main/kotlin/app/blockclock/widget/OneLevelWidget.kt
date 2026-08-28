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
 * The full "time and date" widget: 4 × 1 home screen cells, one level —
 * time on the left, weekday and date on the right.
 */
class OneLevelWidget(
    private var preview: WidgetSettings? = null,
) : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = preview ?: WidgetSettingsStore(context).read()
        val openSettings = actionStartActivity(Intent(context, MainActivity::class.java))
        val openClockApp = settings.clockApp.launchAction(context, defaultClockApp(context))
        val openCalendarApp = settings.calendarApp.launchAction(context, defaultCalendarApp(context))
        provideContent {
            OneLevelWidgetContent(
                settings = settings,
                openSettings = openSettings,
                openClockApp = openClockApp,
                openCalendarApp = openCalendarApp,
            )
        }
    }

    fun update(preview: WidgetSettings) {
        this.preview = preview
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
