package app.blockclock.widget

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
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
import androidx.core.graphics.createBitmap
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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.wrapContentSize
import androidx.glance.text.FontWeight
import app.blockclock.MainActivity
import app.blockclock.util.size
import app.blockclock.R
import kotlin.math.roundToInt
import android.os.Build.VERSION_CODES.P as AndroidP
import android.os.Build.VERSION_CODES.S as AndroidS

private const val LAYOUT_HORIZONTAL_MIN_ASPECT = 2.5f
private const val TIME_TEXT_HEIGHT_FACTOR = 0.7f
private const val DATE_TEXT_HEIGHT_FACTOR = 0.6f

/**
 * Полный виджет «время + дата»: 4 × 1 клеток рабочего стола.
 *
 * Цвет/прозрачность прямоугольников, отступы, цвет текста, формат
 * времени и порядок даты берутся из [WidgetSettingsStore].
 */
class ClockWidget(
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
                settings = settings,
                openSettings = openSettings,
                openClockApp = openClockApp,
                openCalendarApp = openCalendarApp,
            )
        }
    }
}

@Composable
private fun ClockWidgetContent(
    settings: WidgetSettings,
    openSettings: Action,
    openClockApp: Action,
    openCalendarApp: Action,
) {
    val settings = rememberWidgetSettings(settings)
    val available = LocalSize.current
    val structure = when {
        available.width / available.height >= LAYOUT_HORIZONTAL_MIN_ASPECT -> Structure.OneLevel
        else -> Structure.ThreeLevel
    }
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
        if (structure == Structure.OneLevel) {
            Row {
                TimeSection(settings, structure, cellSize, onClick = openClockApp)
                Column {
                    WeekdaySection(settings, part = structure.weekday, cellSize = cellSize, onClick = openCalendarApp)
                    DateSection(settings, structure, cellSize, onClick = openCalendarApp)
                }
            }
        } else {
            Column {
                TimeSection(settings, structure, cellSize, onClick = openClockApp)
                Column {
                    WeekdaySection(settings, part = structure.weekday, cellSize, onClick = openCalendarApp)
                    DateSection(settings, structure, cellSize, onClick = openCalendarApp)
                }
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
 * Часы и минуты: две ячейки рядом с нативными TextClock.
 *
 * Размер каждой ячейки — [cellSize] (клетка структуры), умноженный
 * на вес части; шрифт времени — 70% высоты ячейки.
 */
@Composable
internal fun TimeSection(
    settings: WidgetSettings,
    structure: Structure.Time,
    cellSize: DpSize,
    modifier: GlanceModifier = GlanceModifier,
    onClick: Action,
) {
    val rectColor = settings.effectiveRectColor
    val textColor = settings.text
    val gap = settings.gapDp.dp
    val cornerRadiusDp = settings.cornerRadiusDp

    Row(modifier = modifier.wrapContentSize().clickable(onClick)) {
        Cell(ClockTextPart.HOURS, rectColor, textColor, gap, structure.hours, cornerRadiusDp, cellSize)
        Cell(ClockTextPart.MINUTES, rectColor, textColor, gap, structure.minutes, cornerRadiusDp, cellSize)
    }
}

/**
 * День недели: одна ячейка части [part] на всю ширину.
 */
@Composable
internal fun WeekdaySection(
    settings: WidgetSettings,
    part: Part,
    cellSize: DpSize,
    modifier: GlanceModifier = GlanceModifier,
    onClick: Action,
) {
    val rectColor = settings.effectiveRectColor
    val textColor = settings.text
    val gap = settings.gapDp.dp
    val cornerRadiusDp = settings.cornerRadiusDp

    Row(modifier = modifier.wrapContentSize().clickable(onClick)) {
        Cell(ClockTextPart.WEEKDAY, rectColor, textColor, gap, part, cornerRadiusDp, cellSize)
    }
}

/**
 * Дата: день, месяц и год в один ряд (порядок день/месяц зависит
 * от настройки), размеры ячеек — [cellSize] × веса частей [structure].
 */
@Composable
internal fun DateSection(
    settings: WidgetSettings,
    structure: Structure.Date,
    cellSize: DpSize,
    onClick: Action,
) {
    val rectColor = settings.effectiveRectColor
    val textColor = settings.text
    val gap = settings.gapDp.dp
    val cornerRadiusDp = settings.cornerRadiusDp
    val (firstPart, secondPart) = when {
        settings.dayFirst -> ClockTextPart.DAY to ClockTextPart.MONTH
        else -> ClockTextPart.MONTH to ClockTextPart.DAY
    }

    Row(modifier = GlanceModifier.wrapContentSize().clickable(onClick)) {
        Cell(firstPart, rectColor, textColor, gap, structure.first, cornerRadiusDp, cellSize)
        Cell(secondPart, rectColor, textColor, gap, structure.second, cornerRadiusDp, cellSize)
        Cell(ClockTextPart.YEAR, rectColor, textColor, gap, structure.year, cornerRadiusDp, cellSize)
    }
}

/**
 * Одна ячейка с закруглёнными углами и отступами по флагам части [part].
 *
 * Размер — [part.calcSize]: клетка [cellSize], умноженная на вес, плюс
 * отступы (каждый флаг — [gap]/2, у широких частей — ещё [Part.gapInside]).
 * Шрифт — доля высоты ячейки за вычетом верхнего/нижнего отступов:
 * 70% для времени ([Part.time]), 60% для даты.
 *
 * На Android 12+ фон рисует Glance, на Android 11 и ниже — Cell bitmap
 * с запечёнными цветом (включая прозрачность) и углами; при [gap] = 0
 * ячейки прозрачны (общий фон рисует [CellBackground]).
 * Текст — нативный [android.widget.TextClock], обновляется сам.
 */
@Composable
internal fun Cell(
    layoutPart: ClockTextPart,
    rectColor: Color,
    textColor: Color,
    gap: Dp,
    part: Part,
    cornerRadiusDp: Int,
    cellSize: DpSize,
) {
    val context = LocalContext.current
    val size = part.calcSize(cellSize, gap)
    var modifier: GlanceModifier = GlanceModifier.size(size)
    var height = size.height
    if (part.gap.left) {
        modifier = modifier.padding(start = gap / 2)
    }
    if (part.gap.top) {
        modifier = modifier.padding(top = gap / 2)
        height -= gap / 2
    }
    if (part.gap.right) {
        modifier = modifier.padding(end = gap / 2)
    }
    if (part.gap.bottom) {
        modifier = modifier.padding(bottom = gap / 2)
        height -= gap / 2
    }
    val factor = when (part.time) {
        true -> TIME_TEXT_HEIGHT_FACTOR
        false -> DATE_TEXT_HEIGHT_FACTOR
    }
    val fontSize = (height.value * factor).sp
    Box(modifier = modifier) {
        if (gap <= 0.dp) {
            CellRemoteViews(textRemoteViews(context, layoutPart, textColor, fontSize))
        } else if (SDK_INT >= AndroidS) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(rectColor)
                    .cornerRadius(cornerRadiusDp.dp),
                contentAlignment = Alignment.Center,
            ) {
                CellRemoteViews(textRemoteViews(context, layoutPart, textColor, fontSize))
            }
        } else {
            val density = context.resources.displayMetrics.density
            val cell = RemoteViews(context.packageName, R.layout.cell_bg)
            cell.setImageViewBitmap(R.id.cell_bg, cellBitmap(size, rectColor, cornerRadiusDp, density))
            cell.addView(R.id.cell_root, textRemoteViews(context, layoutPart, textColor, fontSize))
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
) {
    if (SDK_INT >= AndroidS) {
        Box(
            modifier = GlanceModifier
                .size(size)
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
            modifier = GlanceModifier.size(size),
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
        TypedValue.COMPLEX_UNIT_PX,
        fontSize.value * context.resources.displayMetrics.density,
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
