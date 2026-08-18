package app.atomofiron.blockclock.widget

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.S as AndroidS
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
import androidx.core.graphics.createBitmap

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
    val gridSize = letterboxSize(
        available,
        if (useHorizontalLayout) 4f else 1f,
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(openSettings),
        contentAlignment = Alignment.Center,
    ) {
        if (settings.gapDp == 0) {
            CellBackground(settings.effectiveRectColor, settings.cornerRadiusDp, gridSize)
        }
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
    val cornerRadiusDp = settings.cornerRadiusDp

    val gridSize = letterboxSize(area, ratio = SECTION_ASPECT)
    val fontSize = (gridSize.height.value * TIME_TEXT_HEIGHT_FACTOR).sp
    val cellSize = DpSize(gridSize.width / 2, gridSize.height)

    Row(modifier = modifier.size(gridSize.width, gridSize.height).clickable(onClick)) {
        Cell(ClockTextPart.HOURS, rectColor, textColor, gap, cornerRadiusDp, cellSize, fontSize,
            modifier = GlanceModifier.defaultWeight().fillMaxHeight())
        Cell(ClockTextPart.MINUTES, rectColor, textColor, gap, cornerRadiusDp, cellSize, fontSize,
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
    val cornerRadiusDp = settings.cornerRadiusDp
    val (dayPart, monthPart) = when {
        settings.dayFirst -> ClockTextPart.DAY to ClockTextPart.MONTH
        else -> ClockTextPart.MONTH to ClockTextPart.DAY
    }

    val gridSize = letterboxSize(area, ratio = SECTION_ASPECT)
    val fontSize = ((gridSize.height / 2 - gap).value * DATE_TEXT_HEIGHT_FACTOR).sp
    val topCellSize = DpSize(gridSize.width, gridSize.height / 2)
    val quarterCellSize = DpSize(gridSize.width / 4, gridSize.height / 2)
    val halfCellSize = DpSize(gridSize.width / 2, gridSize.height / 2)

    Column(modifier = modifier.size(gridSize.width, gridSize.height).clickable(onClick)) {
        Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
            Cell(ClockTextPart.WEEKDAY, rectColor, textColor, gap, cornerRadiusDp, topCellSize, fontSize,
                modifier = GlanceModifier.defaultWeight().fillMaxHeight())
        }
        Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
            Row(GlanceModifier.fillMaxHeight().defaultWeight()) {
                Cell(dayPart, rectColor, textColor, gap, cornerRadiusDp, quarterCellSize, fontSize,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                Cell(monthPart, rectColor, textColor, gap, cornerRadiusDp, quarterCellSize, fontSize,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight())
            }
            Cell(ClockTextPart.YEAR, rectColor, textColor, gap, cornerRadiusDp, halfCellSize, fontSize,
                modifier = GlanceModifier.defaultWeight().fillMaxHeight())
        }
    }
}

/**
 * Одна «квадратная» ячейка с закруглёнными углами и отступом [gap] вокруг неё.
 *
 * На Android 12+ скругление рисует Glance. На Android 11 и ниже фон —
 * Cell bitmap с запечёнными цветом (включая прозрачность) и углами.
 * Текст — нативный [android.widget.TextClock], обновляется сам.
 */
@Composable
internal fun Cell(
    part: ClockTextPart,
    rectColor: Color,
    textColor: Color,
    gap: Dp,
    cornerRadiusDp: Int,
    cellSize: DpSize,
    fontSize: TextUnit,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    Box(modifier = modifier.padding(gap / 2)) {
        if (gap <= 0.dp) {
            CellRemoteViews(textRemoteViews(context, part, textColor, fontSize))
        } else if (SDK_INT >= AndroidS) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(rectColor)
                    .cornerRadius(cornerRadiusDp.dp),
                contentAlignment = Alignment.Center,
            ) {
                CellRemoteViews(textRemoteViews(context, part, textColor, fontSize))
            }
        } else {
            val density = context.resources.displayMetrics.density
            val cell = RemoteViews(context.packageName, R.layout.cell_bg)
            cell.setImageViewBitmap(R.id.cell_bg, cellBitmap(cellSize, rectColor, cornerRadiusDp, density))
            cell.addView(R.id.cell_root, textRemoteViews(context, part, textColor, fontSize))
            CellRemoteViews(cell)
        }
    }
}

@Composable
private fun CellRemoteViews(remoteViews: RemoteViews) {
    AndroidRemoteViews(
        remoteViews = remoteViews,
        modifier = GlanceModifier.fillMaxSize(),
    )
}

/**
 * Общий фон сетки виджета: один скруглённый прямоугольник вместо фонов
 * отдельных ячеек (используется при нулевом зазоре).
 */
@Composable
internal fun CellBackground(
    rectColor: Color,
    cornerRadiusDp: Int,
    size: DpSize,
    modifier: GlanceModifier = GlanceModifier,
) {
    if (SDK_INT >= AndroidS) {
        Box(
            modifier = modifier
                .size(size.width, size.height)
                .background(rectColor)
                .cornerRadius(cornerRadiusDp.dp),
        ) {
        }
    } else {
        val context = LocalContext.current
        val density = context.resources.displayMetrics.density
        val bg = RemoteViews(context.packageName, R.layout.cell_bg)
        bg.setImageViewBitmap(
            R.id.cell_bg,
            cellBitmap(size, rectColor, cornerRadiusDp, density),
        )
        AndroidRemoteViews(
            remoteViews = bg,
            modifier = modifier.size(size.width, size.height),
        )
    }
}

/** RemoteViews текста ячейки: цвет, размер и вертикальная компенсация. */
@Composable
private fun textRemoteViews(
    context: Context,
    part: ClockTextPart,
    textColor: Color,
    fontSize: TextUnit,
): RemoteViews {
    val views = RemoteViews(context.packageName, part.layoutRes)
    views.setTextColor(R.id.clock_text, textColor.toArgb())
    views.setPadding(R.id.clock_text, fontSize, FontWeight.Bold)
    views.setTextViewTextSize(
        R.id.clock_text,
        TypedValue.COMPLEX_UNIT_SP,
        fontSize.value,
    )
    return views
}

/** Растровый фон ячейки: цвет с прозрачностью и скруглённые углы. */
private fun cellBitmap(
    size: DpSize,
    color: Color,
    cornerRadiusDp: Int,
    density: Float,
): Bitmap {
    val width = (size.width.value * density).roundToInt().coerceAtLeast(1)
    val height = (size.height.value * density).roundToInt().coerceAtLeast(1)
    val radius = cornerRadiusDp * density
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
    }
    canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, paint)
    return bitmap
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
