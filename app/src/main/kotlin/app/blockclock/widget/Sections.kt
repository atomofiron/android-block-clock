package app.blockclock.widget

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.wrapContentSize
import app.blockclock.R
import app.blockclock.util.size
import kotlin.math.roundToInt
import android.os.Build.VERSION_CODES.S as AndroidS

private const val TIME_TEXT_HEIGHT_FACTOR = 0.7f
private const val DATE_TEXT_HEIGHT_FACTOR = 0.6f

/**
 * Reactively loads the settings: while the widget composition is alive it
 * listens to SharedPreferences changes and redraws without restarting
 * provideGlance.
 */
@Composable
internal fun rememberWidgetSettings(initial: WidgetSettings): WidgetSettings {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(initial) }
    DisposableEffect(context) {
        val store = WidgetSettingsStore(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            settings = store.read()
        }
        store.setListener(listener)
        onDispose { store.removeListener(listener) }
    }
    return settings
}

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
 * A single cell with rounded corners and padding per the [part] gap flags.
 *
 * The size is [part.calcSize]: the [cellSize] cell times the weight plus
 * the gaps (each flag adds [gap]/2, wide parts add [Part.gapInside] more).
 * The font is a fraction of the cell height minus the top/bottom gaps:
 * 70% for time parts ([Part.time]), 60% for date parts.
 *
 * The background is drawn by Glance on Android 12+ and by a cell bitmap
 * (color with transparency and corners baked in) on Android 11 and below;
 * at [gap] = 0 cells are transparent (the shared [CellBackground] draws).
 * The text is a native [android.widget.TextClock] that updates itself.
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
 * The widget grid background: a single rounded rectangle instead of
 * per-cell backgrounds (used when the gap is zero).
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

@Composable
private fun textRemoteViews(
    context: Context,
    part: ClockTextPart,
    textColor: Color,
    fontSize: TextUnit,
): RemoteViews {
    val views = RemoteViews(context.packageName, part.layoutRes)
    views.setTextColor(R.id.clock_text, textColor.toArgb())
    views.setTextViewTextSize(
        R.id.clock_text,
        TypedValue.COMPLEX_UNIT_PX,
        fontSize.value * context.resources.displayMetrics.density,
    )
    return views
}

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
