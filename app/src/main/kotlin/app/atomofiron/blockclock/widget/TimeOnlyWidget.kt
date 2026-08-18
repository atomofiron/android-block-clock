package app.atomofiron.blockclock.widget

import android.content.Context
import androidx.compose.runtime.Composable
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
import androidx.glance.layout.fillMaxSize

/**
 * Виджет «только время»: 2 × 1 клеток рабочего стола, часы и минуты.
 * Клик открывает системное приложение часов.
 */
class TimeOnlyWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = WidgetSettingsStore.load(context)
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
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(openClockApp),
        contentAlignment = Alignment.Center,
    ) {
        if (settings.gapDp == 0) {
            CellBackground(settings.effectiveRectColor, settings.cornerRadiusDp, letterboxSize(available, 2f))
        }
        Row {
            TimeSection(settings = settings, area = available, onClick = openClockApp)
        }
    }
}
