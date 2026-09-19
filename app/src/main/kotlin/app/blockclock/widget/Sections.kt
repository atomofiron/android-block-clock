package app.blockclock.widget

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build.VERSION.SDK_INT
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
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
import app.blockclock.model.CellFont
import app.blockclock.util.Android
import app.blockclock.util.size
import app.blockclock.util.toCellFont
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
    Row(modifier = modifier.wrapContentSize().clickable(onClick)) {
        Cell(ClockTextPart.HOURS, structure.hours, cellSize, settings)
        Cell(ClockTextPart.MINUTES, structure.minutes, cellSize, settings)
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
    Row(modifier = modifier.wrapContentSize().clickable(onClick)) {
        Cell(ClockTextPart.WEEKDAY, part, cellSize, settings)
    }
}

@Composable
internal fun DateSection(
    settings: WidgetSettings,
    structure: Structure.Date,
    cellSize: DpSize,
    onClick: Action,
) {
    val (firstPart, secondPart) = when {
        settings.dayFirst -> ClockTextPart.DAY to ClockTextPart.MONTH
        else -> ClockTextPart.MONTH to ClockTextPart.DAY
    }
    Row(modifier = GlanceModifier.wrapContentSize().clickable(onClick)) {
        Cell(firstPart, structure.first, cellSize, settings)
        Cell(secondPart, structure.second, cellSize, settings)
        Cell(ClockTextPart.YEAR, structure.year, cellSize, settings)
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
 * The font goes to the home screen as a family name it resolves itself —
 * a typeface would not survive the trip (see [CellFont]); without a font the
 * [WidgetSettings.textStyle] bits style the text of the default font.
 */
@Composable
internal fun Cell(
    layoutPart: ClockTextPart,
    part: Part,
    cellSize: DpSize,
    settings: WidgetSettings,
) {
    val rectColor = settings.effectiveRectColor
    val textColor = settings.text
    val gap = settings.gapDp.dp
    val cornerRadiusDp = settings.cornerRadiusDp
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
    val font = remember(settings.font) {
        when {
            settings.font == null || !Android.Q -> null
            else -> settings.font.toCellFont()
        }
    }
    val textStyle = settings.textStyle.bits
    Box(modifier = modifier) {
        if (gap <= 0.dp) {
            CellRemoteViews(textRemoteViews(context, layoutPart, textColor, fontSize, font, textStyle))
        } else if (SDK_INT >= AndroidS) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(rectColor)
                    .cornerRadius(cornerRadiusDp.dp),
                contentAlignment = Alignment.Center,
            ) {
                CellRemoteViews(textRemoteViews(context, layoutPart, textColor, fontSize, font, textStyle))
            }
        } else {
            val density = context.resources.displayMetrics.density
            val cell = RemoteViews(context.packageName, R.layout.cell_bg)
            cell.setImageViewBitmap(R.id.cell_bg, cellBitmap(size, rectColor, cornerRadiusDp, density))
            cell.addView(R.id.cell_root, textRemoteViews(context, layoutPart, textColor, fontSize, font, textStyle))
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
    font: CellFont?,
    textStyle: Int,
): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.text_clock)
    val density = context.resources.displayMetrics.density
    val sizePx = fontSize.value * density
    views.setTextColor(R.id.clock_text, textColor.toArgb())
    views.setTextViewTextSize(R.id.clock_text, TypedValue.COMPLEX_UNIT_PX, sizePx)
    val shift = remember(font, fontSize, part) {
        font.textShift(sizePx, part.sample())
    }
    when {
        // Padding moves the centred line by its half, so the shift is doubled.
        shift > 0 -> views.setViewPadding(R.id.clock_text, 0, shift * 2, 0, 0)
        shift < 0 -> views.setViewPadding(R.id.clock_text, 0, 0, 0, -shift * 2)
    }
    font?.variationSettings?.let { views.setString(R.id.clock_text, "setFontVariationSettings", it) }
    views.setCharSequence(R.id.clock_text, "setFormat24Hour", styledClockFormat(part.format24, font, textStyle))
    views.setCharSequence(R.id.clock_text, "setFormat12Hour", styledClockFormat(part.format12, font, textStyle))
    return views
}

/**
 * The vertical shift in pixels that puts the text on the optical centre of the cell
 * (`shift > 0` moves the text down).
 *
 * The host centres the line box of the font — its ascent and descent — and not the ink of
 * the text: the digits sit off that centre by a per-font amount, e.g. 1.4 px up for Roboto
 * and 3.2 px down for Noto Serif per 100 px of the font size. `includeFontPadding="false"`
 * does not help: it only drops the accent space on top of the very same ascent and descent.
 */
private fun CellFont?.textShift(sizePx: Float, sample: String): Int {
    val face = when {
        this == null -> Typeface.DEFAULT
        family != null -> Typeface.create(family, style)
        else -> typeface ?: Typeface.DEFAULT
    }
    val paint = Paint().apply {
        typeface = face
        textSize = sizePx
    }
    val bounds = Rect()
    paint.getTextBounds(sample, 0, sample.length, bounds)
    val metrics = paint.fontMetrics
    return ((metrics.ascent + metrics.descent - bounds.top - bounds.bottom) / 2f).roundToInt()
}

/** The sample of the cell text: the digits of a font share one ink box. */
private fun ClockTextPart.sample(): String = when (this) {
    ClockTextPart.HOURS, ClockTextPart.MINUTES, ClockTextPart.DAY, ClockTextPart.YEAR -> DIGITS
    ClockTextPart.WEEKDAY, ClockTextPart.MONTH -> CAPITAL
}

/** The digits of the time and the day of the month. */
private const val DIGITS = "0123456789"

/** A capital letter: the words of the weekday and the month are centred by the cap height. */
private const val CAPITAL = "H"

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

/**
 * The clock format with the font spans attached. The home screen resolves the family name
 * itself; the typeface span is the fallback for the in-process preview of a font the
 * system does not name — the host would drop it (see [CellFont]). Without a font the
 * [textStyle] bits style the text of the default font.
 */
private fun styledClockFormat(
    format: String,
    font: CellFont?,
    textStyle: Int,
): CharSequence = when {
    font == null || Android.Below.Q -> format.withStyle(textStyle)
    font.family != null -> SpannableString(format).apply {
        setSpan(TypefaceSpan(font.family), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (font.style != Typeface.NORMAL) {
            setSpan(StyleSpan(font.style), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
    font.typeface != null -> SpannableString(format).apply {
        setSpan(TypefaceSpan(font.typeface), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    else -> format
}

/** The format with the style bits of the default font, or the format itself for the normal style. */
private fun String.withStyle(style: Int): CharSequence = when {
    style == Typeface.NORMAL -> this
    else -> SpannableString(this).apply {
        setSpan(StyleSpan(style), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}
