package app.atomofiron.blockclock.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
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
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import app.atomofiron.blockclock.MainActivity

/**
 * Виджет «только дата»: 2 × 1 клеток рабочего стола —
 * день недели, день, месяц и год.
 */
class DateOnlyWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = WidgetSettingsStore.load(context)
        
        val openSettings = actionStartActivity(Intent(context, MainActivity::class.java))
        provideContent {
            DateOnlyWidgetContent(initialSettings = settings, openSettings = openSettings)
        }
    }
}

@Composable
private fun DateOnlyWidgetContent(
    initialSettings: WidgetSettings,
    openSettings: Action,
) {
    val settings = rememberWidgetSettings(initialSettings)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(openSettings),
        contentAlignment = Alignment.Center,
    ) {
        Row {
            DateSection(settings = settings, area = LocalSize.current)
        }
    }
}
