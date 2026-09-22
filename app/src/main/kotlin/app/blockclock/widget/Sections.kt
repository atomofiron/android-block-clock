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
import app.blockclock.util.Percents
import app.blockclock.util.size
import app.blockclock.util.toCellFont
import kotlin.math.roundToInt
import android.os.Build.VERSION_CODES.S as AndroidS

private const val TEXT_HEIGHT_FACTOR = 0.6f

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
        Cell(ClockPart.HOURS, structure.hours, cellSize, settings)
        Cell(ClockPart.MINUTES, structure.minutes, cellSize, settings)
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
        val clockPart = when {
            settings.amPm -> ClockPart.AM_PM_WEEKDAY
            else -> ClockPart.WEEKDAY
        }
        Cell(clockPart, part, cellSize, settings)
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
        settings.dayFirst -> ClockPart.DAY to ClockPart.MONTH
        else -> ClockPart.MONTH to ClockPart.DAY
    }
    Row(modifier = GlanceModifier.wrapContentSize().clickable(onClick)) {
        Cell(firstPart, structure.first, cellSize, settings)
        Cell(secondPart, structure.second, cellSize, settings)
        Cell(ClockPart.YEAR, structure.year, cellSize, settings)
    }
}

/**
 * A single cell with rounded corners and padding per the gap flags of its [Part].
 *
 * The size is [Part.calcSize]: the [cellSize] cell times the [Part.weight] plus the gaps —
 * [WidgetSettings.gapDp]/2 on each flagged side, and a part wider or taller than a single
 * cell gets [Part.gapInside] × [WidgetSettings.gapDp] more. The padding takes those halves
 * back from the text, so the text height is the cell height minus the halves of the top and
 * the bottom flags, and the font is a percentage of it ([WidgetSettings.timeFontPercent] for
 * time parts ([Part.time]), [WidgetSettings.dateFontPercent] for date parts).
 *
 * The background is drawn by Glance on Android 12+ and by a cell bitmap (color with
 * transparency and corners baked in) on Android 11 and below; at a zero
 * [WidgetSettings.gapDp] the cells are transparent (the shared [CellBackground] draws).
 *
 * The text is a native [android.widget.TextClock] that updates itself. The font reaches the
 * home screen as a name the host resolves itself — the family of the file, or an alias of it
 * that asks for the weight of the file — with the style bits beside it: a typeface would not
 * survive the trip (see [CellFont]). The axes of a variable font go as a string as well, but
 * the host drops them, so only the weight and the slant the platform expresses as style bits
 * are rendered. Without a font the [WidgetSettings.textStyle] bits style the default font.
 */
@Composable
internal fun Cell(
    clockPart: ClockPart,
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
        true -> TEXT_HEIGHT_FACTOR * settings.timeFontPercent / Percents
        false -> TEXT_HEIGHT_FACTOR * settings.dateFontPercent / Percents
    }
    val fontSize = (height.value * factor).sp
    val cellHeightPx = height.value * context.resources.displayMetrics.density
    val font = remember(settings.font) {
        when {
            settings.font == null || !Android.Q -> null
            else -> settings.font.toCellFont()
        }
    }
    val textStyle = settings.textStyle.bits
    Box(modifier = modifier) {
        if (gap <= 0.dp) {
            CellRemoteViews(textRemoteViews(context, clockPart, textColor, fontSize, cellHeightPx, font, textStyle))
        } else if (SDK_INT >= AndroidS) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(rectColor)
                    .cornerRadius(cornerRadiusDp.dp),
                contentAlignment = Alignment.Center,
            ) {
                CellRemoteViews(textRemoteViews(context, clockPart, textColor, fontSize, cellHeightPx, font, textStyle))
            }
        } else {
            val density = context.resources.displayMetrics.density
            val cell = RemoteViews(context.packageName, R.layout.cell_bg)
            cell.setImageViewBitmap(R.id.cell_bg, cellBitmap(size, rectColor, cornerRadiusDp, density))
            cell.addView(R.id.cell_root, textRemoteViews(context, clockPart, textColor, fontSize, cellHeightPx, font, textStyle))
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
    part: ClockPart,
    textColor: Color,
    fontSize: TextUnit,
    cellHeightPx: Float,
    font: CellFont?,
    textStyle: Int,
): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.text_clock)
    val density = context.resources.displayMetrics.density
    val sizePx = fontSize.value * density
    views.setTextColor(R.id.clock_text, textColor.toArgb())
    views.setTextViewTextSize(R.id.clock_text, TypedValue.COMPLEX_UNIT_PX, sizePx)
    val shift = remember(font, fontSize, cellHeightPx, part) {
        font.textShift(sizePx, cellHeightPx, part.sample())
    }
    when {
        // The shift is the padding itself: the host moves the text by its half while the line
        // fits the cell, and by the whole of it when it does not, see [textShift].
        shift > 0 -> views.setViewPadding(R.id.clock_text, 0, shift, 0, 0)
        shift < 0 -> views.setViewPadding(R.id.clock_text, 0, 0, 0, -shift)
    }
    font?.variationSettings?.let { views.setString(R.id.clock_text, "setFontVariationSettings", it) }
    views.setCharSequence(R.id.clock_text, "setFormat24Hour", styledClockFormat(part.format24, font, textStyle))
    views.setCharSequence(R.id.clock_text, "setFormat12Hour", styledClockFormat(part.format12, font, textStyle))
    return views
}

/**
 * The vertical padding in pixels that puts the ink of the [sample] on the optical centre of a
 * [cellHeightPx] tall cell: `> 0` is the top padding and moves the text down, `< 0` is the
 * bottom one.
 *
 * The host centres the line box of the font — its ascent and descent — and not the ink of
 * the text: the digits sit off that centre by a per-font amount, e.g. 1.4 px up for Roboto
 * and 3.2 px down for Noto Serif per 100 px of the font size. `includeFontPadding="false"`
 * does not help: it only drops the accent space on top of the very same ascent and descent.
 *
 * A line box that does not fit the cell is the second case, and another formula. The host then
 * centres nothing: `TextView.getVerticalOffset` shifts the text only `if (textht < boxht)`,
 * where the box is the cell minus the paddings, and otherwise leaves `voffset` at zero — the
 * text sits at the padding itself. ComingSoon is that case: its line box is 1.51 em against the
 * 0.7 em the cell height gives the time parts, and a half-shift padding dropped its digits 10%
 * of the cell height below the centre on the device. The opposite direction is limited by the
 * cell: a padding larger than the cell minus the line box stops the host centring anything, so an
 * upward shift the cell has no room for is cut down to the slack it has, and a line taller than
 * the cell leaves no slack at all and no shift.
 */
private fun CellFont?.textShift(
    sizePx: Float,
    cellHeightPx: Float,
    sample: String,
): Int {
    val face = when {
        this == null -> Typeface.DEFAULT
        // The host draws the text with the family it is handed, so the metrics of the shift
        // come from that family and not from the file of the picked font: the file may have
        // other metrics entirely, and it does not even reach the host ([CellFont]).
        else -> Typeface.create(family, style)
    }
    val paint = Paint().apply {
        typeface = face
        textSize = sizePx
    }
    val bounds = Rect()
    paint.getTextBounds(sample, 0, sample.length, bounds)
    val metrics = paint.fontMetrics
    val inkTop = bounds.top.toFloat()
    val inkBottom = bounds.bottom.toFloat()
    // The padding the centred case asks for: it moves the line by its half, so twice the shift.
    val padding = (metrics.ascent + metrics.descent - inkTop - inkBottom).roundToInt()
    // The padding the host stops centring at: the cell minus the line box.
    val slack = cellHeightPx - metrics.descent + metrics.ascent
    return when {
        // The padding the cell has no room for is cut down to what fits: the padding shrinks
        // the box the host centres the line in, and a line that fills the cell leaves no room.
        padding <= 0 -> -minOf(-padding, slack.toInt().coerceAtLeast(0))
        padding < slack -> padding
        // Pinned to the padding: the offset of the ink from the top of the cell.
        else -> (cellHeightPx / 2f + metrics.ascent - (inkTop + inkBottom) / 2f).roundToInt().coerceAtLeast(0)
    }
}

/** The sample of the cell text: the digits of a font share one ink box. */
private fun ClockPart.sample(): String = when (this) {
    ClockPart.HOURS, ClockPart.MINUTES, ClockPart.DAY, ClockPart.YEAR, ClockPart.MONTH -> "0123456789"
    ClockPart.WEEKDAY, ClockPart.AM_PM_WEEKDAY -> "H"
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

/**
 * The clock format with the font spans attached. The home screen resolves the family name
 * itself (see [CellFont]). Without a font the [textStyle] bits style the text of the
 * default font.
 */
private fun styledClockFormat(
    format: String,
    font: CellFont?,
    textStyle: Int,
): CharSequence = when {
    font == null || Android.Below.Q -> format.withStyle(textStyle)
    else -> SpannableString(format).apply {
        setSpan(TypefaceSpan(font.family), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (font.style != Typeface.NORMAL) {
            setSpan(StyleSpan(font.style), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}

/** The format with the style bits of the default font, or the format itself for the normal style. */
private fun String.withStyle(style: Int): CharSequence = when {
    style == Typeface.NORMAL -> this
    else -> SpannableString(this).apply {
        setSpan(StyleSpan(style), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}
