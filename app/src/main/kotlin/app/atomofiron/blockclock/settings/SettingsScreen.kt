package app.atomofiron.blockclock.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import app.atomofiron.blockclock.R
import app.atomofiron.blockclock.util.statusBarAndCutout
import app.atomofiron.blockclock.widget.ClockWidget
import app.atomofiron.blockclock.widget.DateOnlyWidget
import app.atomofiron.blockclock.widget.TimeOnlyWidget
import app.atomofiron.blockclock.widget.WidgetSettings
import app.atomofiron.blockclock.widget.WidgetSettingsStore
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

private val ClipInset = 16.dp
private val ClipCornerRadius = 28.dp
private val GridColumnMinWidth = 320.dp
private val GridSpacing = 16.dp
private val GridBottomPadding = 16.dp
private val CornerRadiusRange = 1f..32f
private val PreviewSideInset = 16.dp
private val PreviewMaxWidthInset = 32.dp
private const val PreviewAspectRatio = 4f / 1f
private val CardCornerRadius = 28.dp
private val CardContentPadding = 16.dp
private val CardSpacing = 12.dp
private val FieldCornerRadius = 12.dp
private val FieldVerticalPadding = 10.dp
private val FieldSpacing = 12.dp
private val SwatchSize = 36.dp
private val SwatchCornerRadius = 10.dp
private val SwatchBorderWidth = 1.dp
private val SwatchBorderColor = Color(0x33000000)
private val SliderLabelSpacing = 4.dp
private const val PercentFactor = 100f
private val TransparencyRange = 0f..1f
private val GapRange = 0f..8f
private val SwitchRowVerticalPadding = 6.dp
private val DialogSpacing = 12.dp
private val DialogSwatchSize = 32.dp
private val DialogSwatchCornerRadius = 8.dp
private val MarkerSize = 16.dp
private val MarkerRadius = 8.dp
private val MarkerBorderWidth = 2.dp
private val MarkerBorderColor = Color(0x66000000)
private val SvBoxHeight = 160.dp
private val SvBoxCornerRadius = 12.dp
private val HueSliderHeight = 28.dp
private val HueSliderCornerRadius = 6.dp
private const val HueDegrees = 360f

/**
 * Экран настроек виджета: живой предпросмотр, цвет и прозрачность
 * прямоугольников, отступы между ними, цвет текста, формат времени
 * 12/24 и порядок даты.
 * Любое изменение сразу сохраняется и перерисовывает виджет.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(WidgetSettingsStore.load(context)) }

    var previewSettings by remember { mutableStateOf(settings) }
    var colorTarget by remember { mutableStateOf<ColorTarget?>(null) }
    val scope = rememberCoroutineScope()

    fun apply(newSettings: WidgetSettings) {
        settings = newSettings
        previewSettings = newSettings
        scope.launch {
            WidgetSettingsStore.save(context, newSettings)
            ClockWidget().updateAll(context)
            TimeOnlyWidget().updateAll(context)
            DateOnlyWidget().updateAll(context)
        }
    }

    colorTarget?.let { target ->
        ColorPickerDialog(
            title = stringResource(R.string.label_color),
            initialColor = when (target) {
                ColorTarget.Rect -> settings.backgroundColor
                else -> settings.textColor
            },
            onDismiss = { colorTarget = null },
            onConfirm = { color ->
                colorTarget = null
                when (target) {
                    ColorTarget.Rect -> apply(settings.copy(backgroundColor = color))
                    else -> apply(settings.copy(textColor = color))
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarAndCutout(),
    ) {
        WidgetPreviewCard(previewSettings)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .navigationBarsPadding()
                .padding(start = ClipInset, top = ClipInset, end = ClipInset)
                .clip(RoundedCornerShape(topStart = ClipCornerRadius, topEnd = ClipCornerRadius)),
        ) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(GridColumnMinWidth),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = GridBottomPadding),
                horizontalArrangement = Arrangement.spacedBy(GridSpacing),
                verticalItemSpacing = GridSpacing,
            ) {
                item {
                    SectionCard(stringResource(R.string.section_background)) {
                        ColorField(
                            label = stringResource(R.string.label_color),
                            color = settings.backgroundColor,
                            onClick = { colorTarget = ColorTarget.Rect },
                        )
                        TransparencySlider(
                            transparency = settings.backgroundTransparency,
                            onChange = {
                                previewSettings = previewSettings.copy(backgroundTransparency = it)
                            },
                            onChangeFinished = { apply(settings.copy(backgroundTransparency = it)) },
                        )
                        DpSlider(
                            label = stringResource(R.string.label_corner_radius),
                            value = settings.cornerRadiusDp,
                            range = CornerRadiusRange,
                            onChange = {
                                previewSettings = previewSettings.copy(cornerRadiusDp = it)
                            },
                            onChangeFinished = { apply(settings.copy(cornerRadiusDp = it)) },
                        )
                        GapSlider(
                            gap = settings.gapDp,
                            onChange = { previewSettings = previewSettings.copy(gapDp = it) },
                            onChangeFinished = { apply(settings.copy(gapDp = it)) },
                        )
                    }
                }
                item {
                    SectionCard(stringResource(R.string.section_text)) {
                        ColorField(
                            label = stringResource(R.string.label_color),
                            color = settings.textColor,
                            onClick = { colorTarget = ColorTarget.Text },
                        )
                    }
                }
                item {
                    SectionCard(title = null) {
                        SettingSwitch(
                            label = stringResource(R.string.option_month_first),
                            checked = !settings.dayFirst,
                            onCheckedChange = { checked ->
                                apply(settings.copy(dayFirst = !checked))
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Живой предпросмотр виджета на «обоях» — окно прозрачное, обои видны сквозь него. */
@Composable
private fun WidgetPreviewCard(settings: WidgetSettings) {
    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize
    val minWindowSide = with(density) { min(windowSize.width, windowSize.height).toDp() }
    val maxPreviewWidth = (minWindowSide - PreviewMaxWidthInset).coerceAtLeast(0.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = PreviewSideInset)
            .padding(horizontal = PreviewSideInset),
        contentAlignment = Alignment.Center,
    ) {
        GlanceWidgetPreview(
            widget = ClockWidget(settings),
            refreshKey = settings,
            previewSize = DpSize(
                width = maxPreviewWidth,
                height = maxPreviewWidth / PreviewAspectRatio,
            ),
            modifier = Modifier
                .widthIn(max = maxPreviewWidth)
                .aspectRatio(PreviewAspectRatio),
        )
    }
}

/** Карточка-секция с необязательным заголовком. */
@Composable
private fun SectionCard(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardCornerRadius),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardContentPadding),
            verticalArrangement = Arrangement.spacedBy(CardSpacing),
        ) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

/** Плитка текущего цвета — по тапу открывает диалог с градиентным полем. */
@Composable
private fun ColorField(label: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FieldCornerRadius))
            .clickable(onClick = onClick)
            .padding(vertical = FieldVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(SwatchSize)
                .clip(RoundedCornerShape(SwatchCornerRadius))
                .background(color)
                .border(
                    SwatchBorderWidth,
                    SwatchBorderColor,
                    RoundedCornerShape(SwatchCornerRadius)
                ),
        )
        Spacer(Modifier.width(FieldSpacing))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            text = "#%06X".format(color.toArgb() and 0xFFFFFF),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Слайдер прозрачности: чем больше значение, тем прозрачнее прямоугольники. */
@Composable
private fun TransparencySlider(
    transparency: Float,
    onChange: (Float) -> Unit,
    onChangeFinished: (Float) -> Unit,
) {
    var value by remember { mutableFloatStateOf(transparency) }
    Column(verticalArrangement = Arrangement.spacedBy(SliderLabelSpacing)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.label_transparency),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = "${(value * PercentFactor).toInt()} %",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value,
            onValueChange = {
                value = it
                onChange(it)
            },
            onValueChangeFinished = { onChangeFinished(value) },
            valueRange = TransparencyRange,
        )
    }
}

/**
 * Дискретный слайдер отступов между ячейками: значения от 1 до 8 с шагом 1.
 */
@Composable
private fun GapSlider(gap: Int, onChange: (Int) -> Unit, onChangeFinished: (Int) -> Unit) {
    var value by remember { mutableFloatStateOf(gap.toFloat().coerceIn(GapRange)) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.label_gap),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = value.roundToInt().toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value,
            onValueChange = {
                value = it
                onChange(it.roundToInt())
            },
            onValueChangeFinished = { onChangeFinished(value.roundToInt()) },
            valueRange = GapRange,
            steps = GapRange.run { endInclusive - start }.toInt().dec(),
        )
    }
}

/** Слайдер целого значения в dp: значение показывается справа от подписи. */
@Composable
private fun DpSlider(
    label: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Int) -> Unit,
    onChangeFinished: (Int) -> Unit,
) {
    var current by remember { mutableFloatStateOf(value.toFloat()) }
    Column(verticalArrangement = Arrangement.spacedBy(SliderLabelSpacing)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = current.toInt().toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = current,
            onValueChange = {
                current = it
                onChange(current.toInt())
            },
            onValueChangeFinished = { onChangeFinished(current.toInt()) },
            valueRange = range,
        )
    }
}

/** Строка настройки со Switch: весь ряд кликабелен. */
@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FieldCornerRadius))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = SwitchRowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * Диалог выбора цвета: градиентное поле насыщенность × яркость (SV)
 * при выбранном оттенке (H) + слайдер оттенка + текущий цвет и HEX.
 */
@Composable
private fun ColorPickerDialog(
    title: String,
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
) {
    val initialHsv = remember(initialColor) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor.toArgb(), it) }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    val color = remember(hue, sat, value) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DialogSpacing)) {
                SaturationValueBox(
                    hue = hue,
                    sat = sat,
                    value = value,
                    onChange = { s, v ->
                        sat = s
                        value = v
                    },
                )
                HueSlider(hue = hue, onChange = { hue = it })
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(DialogSwatchSize)
                            .clip(RoundedCornerShape(DialogSwatchCornerRadius))
                            .background(color)
                            .border(
                                SwatchBorderWidth,
                                SwatchBorderColor,
                                RoundedCornerShape(DialogSwatchCornerRadius)
                            ),
                    )
                    Spacer(Modifier.width(FieldSpacing))
                    Text(
                        text = "#%06X".format(color.toArgb() and 0xFFFFFF),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(color) }) {
                Text(stringResource(R.string.button_done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        },
    )
}

/** Квадрат насыщенность (по горизонтали) × яркость (по вертикали). */
@Composable
private fun SaturationValueBox(
    hue: Float,
    sat: Float,
    value: Float,
    onChange: (sat: Float, value: Float) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(SvBoxHeight)
            .clip(RoundedCornerShape(SvBoxCornerRadius))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.White,
                        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                    )
                )
            )
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            .pointerInput(Unit) {
                detectTapGestures { position ->
                    val (s, v) = satValueOf(size, position)
                    onChange(s, v)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { position ->
                        val (s, v) = satValueOf(size, position)
                        onChange(s, v)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val (s, v) = satValueOf(size, change.position)
                        onChange(s, v)
                    },
                )
            },
    ) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (sat * maxWidth.value * density - MarkerRadius.value * density).roundToInt(),
                        y = ((1f - value) * maxHeight.value * density - MarkerRadius.value * density).roundToInt(),
                    )
                }
                .size(MarkerSize)
                .clip(CircleShape)
                .background(Color.White)
                .border(MarkerBorderWidth, MarkerBorderColor, CircleShape),
        )
    }
}

/** Позиция в SV-квадрате → пара (насыщенность, яркость). */
private fun satValueOf(size: IntSize, position: Offset): Pair<Float, Float> = Pair(
    (position.x / size.width).coerceIn(0f, 1f),
    1f - (position.y / size.height).coerceIn(0f, 1f),
)

/** Позиция на радуге → оттенок в градусах (0..360). */
private fun hueOf(size: IntSize, position: Offset): Float =
    (position.x / size.width * HueDegrees).coerceIn(0f, HueDegrees)

/** Слайдер оттенка: градиентная радуга. */
@Composable
private fun HueSlider(hue: Float, onChange: (Float) -> Unit) {
    val rainbow = Color.run { listOf(Red, Yellow, Green, Cyan, Blue, Magenta, Red) }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(HueSliderHeight)
            .clip(RoundedCornerShape(HueSliderCornerRadius))
            .background(Brush.horizontalGradient(rainbow))
            .pointerInput(Unit) {
                detectTapGestures { position -> onChange(hueOf(size, position)) }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { position -> onChange(hueOf(size, position)) },
                    onDrag = { change, _ ->
                        change.consume()
                        onChange(hueOf(size, change.position))
                    },
                )
            },
    ) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (hue / HueDegrees * maxWidth.value * density - MarkerRadius.value * density).roundToInt(),
                        y = (maxHeight.value * density / 2 - MarkerRadius.value * density).roundToInt(),
                    )
                }
                .size(MarkerSize)
                .clip(CircleShape)
                .border(MarkerBorderWidth, Color.White, CircleShape),
        )
    }
}
