package app.atomofiron.blockclock.widget

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build.VERSION.SDK_INT
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import app.atomofiron.blockclock.MainActivity
import app.atomofiron.blockclock.R
import kotlin.math.roundToInt
import android.os.Build.VERSION_CODES.P as AndroidP

private const val LAYOUT_HORIZONTAL_MIN_ASPECT = 2.5f
private const val FULL_WIDGET_SECTION_AREA = 0.5f
private const val SECTION_ASPECT = 2f
private const val TIME_TEXT_HEIGHT_FACTOR = 0.75f
private const val DATE_TEXT_HEIGHT_FACTOR = 0.7f

/**
 * Полный виджет «время + дата»: 4 × 1 клеток рабочего стола.
 *
 * Цвет/прозрачность прямоугольников, отступы, цвет текста, формат
 * времени и порядок даты берутся из [WidgetSettingsStore].
 */
class ClockWidget(
    /**
     * Настройки для однократного рендера (превью в настройках).
     * Забираются и сбрасываются в null при следующем [provideGlance].
     */
    @Volatile
    private var preview: WidgetSettings? = null,
) : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = preview ?: WidgetSettingsStore.load(context)
        val openSettings = actionStartActivity(Intent(context, MainActivity::class.java))
        val openClockApp = clockAppAction(context)
        val openCalendarApp = calendarAppAction(context)
        provideContent {
            ClockWidgetContent(
                initialSettings = settings,
                openSettings = openSettings,
                openClockApp = openClockApp,
                openCalendarApp = openCalendarApp,
            )
        }
    }
}

@Composable
private fun ClockWidgetContent(
    initialSettings: WidgetSettings,
    openSettings: Action,
    openClockApp: Action,
    openCalendarApp: Action,
) {
    val settings = rememberWidgetSettings(initialSettings)
    val available = LocalSize.current
    val useHorizontalLayout = available.width / available.height >= LAYOUT_HORIZONTAL_MIN_ASPECT

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(openSettings),
        contentAlignment = Alignment.Center,
    ) {
        if (useHorizontalLayout) {
            Row {
                TimeSection(settings = settings, area = DpSize(width = available.width * FULL_WIDGET_SECTION_AREA, height = available.height), onClick = openClockApp)
                DateSection(settings = settings, area = DpSize(width = available.width * FULL_WIDGET_SECTION_AREA, height = available.height), onClick = openCalendarApp)
            }
        } else {
            Column {
                TimeSection(settings = settings, area = DpSize(width = available.width, height = available.height * FULL_WIDGET_SECTION_AREA), onClick = openClockApp)
                DateSection(settings = settings, area = DpSize(width = available.width, height = available.height * FULL_WIDGET_SECTION_AREA), onClick = openCalendarApp)
            }
        }
    }
}

/**
 * Реактивная загрузка настроек: пока composition виджета жива, она следит
 * за изменениями в SharedPreferences и перерисовывается без перезапуска
 * provideGlance.
 */
@Composable
internal fun rememberWidgetSettings(initial: WidgetSettings): WidgetSettings {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(initial) }
    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(WidgetSettingsStore.PREFS_NAME, Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            settings = WidgetSettingsStore.load(context)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return settings
}

/**
 * Область [available] с сохранением пропорции [ratio] (letterbox):
 * контент вписывается целиком и центрируется, не растягиваясь.
 */
internal fun letterboxSize(available: DpSize, ratio: Float): DpSize =
    if (available.width / available.height > ratio) {
        DpSize(width = available.height * ratio, height = available.height)
    } else {
        DpSize(width = available.width, height = available.width / ratio)
    }

/**
 * Часы и минуты: две квадратные ячейки рядом с нативными TextClock.
 *
 * Секция сама вычисляет letterbox с пропорцией 2:1 от переданной области
 * [area] и размер текста (70% высоты ячейки).
 */
@Composable
internal fun TimeSection(
    settings: WidgetSettings,
    area: DpSize,
    modifier: GlanceModifier = GlanceModifier,
    onClick: Action,
) {
    val rectColor = settings.effectiveRectColor
    val textColor = settings.textColor
    val gap = settings.gapDp.dp
    val cornerRadius = settings.cornerRadiusDp.dp

    val gridSize = letterboxSize(area, ratio = SECTION_ASPECT)
    val fontSize = (gridSize.height.value * TIME_TEXT_HEIGHT_FACTOR).sp

    Row(modifier = modifier.size(gridSize.width, gridSize.height).clickable(onClick)) {
        Cell(ClockTextPart.HOURS, rectColor, textColor, gap, cornerRadius, fontSize,
            modifier = GlanceModifier.defaultWeight().fillMaxHeight())
        Cell(ClockTextPart.MINUTES, rectColor, textColor, gap, cornerRadius, fontSize,
            modifier = GlanceModifier.defaultWeight().fillMaxHeight())
    }
}

/**
 * Дата: день недели на всю ширину сверху; снизу — день, месяц и год
 * (порядок день/месяц зависит от настройки).
 *
 * Секция сама вычисляет letterbox с пропорцией 2:1 от переданной области
 * [area] и размер текста (70% высоты ячейки).
 */
@Composable
internal fun DateSection(
    settings: WidgetSettings,
    area: DpSize,
    modifier: GlanceModifier = GlanceModifier,
    onClick: Action,
) {
    val rectColor = settings.effectiveRectColor
    val textColor = settings.textColor
    val gap = settings.gapDp.dp
    val cornerRadius = settings.cornerRadiusDp.dp
    val (dayPart, monthPart) = when {
        settings.dayFirst -> ClockTextPart.DAY to ClockTextPart.MONTH
        else -> ClockTextPart.MONTH to ClockTextPart.DAY
    }

    val gridSize = letterboxSize(area, ratio = SECTION_ASPECT)
    val fontSize = ((gridSize.height / 2 - gap).value * DATE_TEXT_HEIGHT_FACTOR).sp

    Column(modifier = modifier.size(gridSize.width, gridSize.height).clickable(onClick)) {
        Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
            Cell(ClockTextPart.WEEKDAY, rectColor, textColor, gap, cornerRadius, fontSize,
                modifier = GlanceModifier.defaultWeight().fillMaxHeight())
        }
        Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
            Row(GlanceModifier.fillMaxHeight().defaultWeight()) {
                Cell(dayPart, rectColor, textColor, gap, cornerRadius, fontSize,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                Cell(monthPart, rectColor, textColor, gap, cornerRadius, fontSize,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight())
            }
            Cell(ClockTextPart.YEAR, rectColor, textColor, gap, cornerRadius, fontSize,
                modifier = GlanceModifier.defaultWeight().fillMaxHeight())
        }
    }
}

/**
 * Одна «квадратная» ячейка с закруглёнными углами и отступом [gap] вокруг неё.
 *
 * Текст — нативный [android.widget.TextClock] через AndroidRemoteViews:
 * он сам обновляет время/дату, без запуска кода приложения.
 * Внешний Box задаёт только зазор: в RemoteViews padding не сжимает фон view,
 * поэтому фон рисует отдельный внутренний Box.
 */
@Composable
internal fun Cell(
    part: ClockTextPart,
    rectColor: Color,
    textColor: Color,
    gap: Dp,
    cornerRadius: Dp,
    fontSize: TextUnit,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    Box(modifier = modifier.padding(gap / 2)) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(rectColor)
                .cornerRadius(cornerRadius),
            contentAlignment = Alignment.Center,
        ) {
            val remoteViews = RemoteViews(context.packageName, part.layoutRes)
            remoteViews.setTextColor(R.id.clock_text, textColor.toArgb())
            remoteViews.setPadding(R.id.clock_text, fontSize, FontWeight.Bold)
            remoteViews.setTextViewTextSize(
                R.id.clock_text,
                TypedValue.COMPLEX_UNIT_SP,
                fontSize.value,
            )
            AndroidRemoteViews(
                remoteViews = remoteViews,
                modifier = GlanceModifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun RemoteViews.setPadding(
    viewId: Int,
    fontSize: TextUnit,
    fontWeight: FontWeight,
) {
    val density = LocalContext.current.resources.displayMetrics.density
    val paint = remember {
        Paint().apply {
            textSize = fontSize.value * density
            if (SDK_INT >= AndroidP) {
                typeface = Typeface.create(Typeface.DEFAULT, fontWeight.value, false)
            }
        }
    }
    val metrics = paint.fontMetrics
    val topSpace = metrics.ascent - metrics.top
    val bottomSpace = metrics.bottom
    val offset = ((bottomSpace - topSpace) / 2).roundToInt()

    when {
        offset < 0 -> setViewPadding(viewId, 0, offset, 0, 0)
        offset > 0 -> setViewPadding(viewId, 0, 0, 0, offset)
    }
}
