package app.blockclock.settings

import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.glance.appwidget.updateAll
import app.blockclock.R
import app.blockclock.licenses.LicensesDialog
import app.blockclock.update.AppSource
import app.blockclock.update.UpdateService
import app.blockclock.update.UpdateStore
import app.blockclock.update.model.UpdateState
import app.blockclock.update.model.UpdateType
import app.blockclock.util.animatedBackgroundColor
import app.blockclock.util.horizontal
import app.blockclock.util.plus
import app.blockclock.util.steps
import app.blockclock.util.windowInsetsPadding
import app.blockclock.widget.DateOnlyWidget
import app.blockclock.widget.OneLevelWidget
import app.blockclock.widget.ThreeLevelWidget
import app.blockclock.widget.TimeOnlyWidget
import app.blockclock.widget.WidgetSettings
import app.blockclock.widget.WidgetSettingsStore
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ClipCornerRadius = 28.dp
private val GridColumnMinWidth = 320.dp
private val CardCornerRadius = 28.dp
private val FieldCornerRadius = 12.dp
private val FieldVerticalPadding = 10.dp
private val SwatchSize = 36.dp
private val SwatchCornerRadius = 10.dp
private val SwatchBorderWidth = 1.dp
private val SwatchBorderColor = Color(0x33000000)
private const val PercentFactor = 100f
private val TransparencyRange = 0f..1f
private val RoundingRange = 0f..32f
private val GapRange = 0f..16f
private val IconSize = 24.dp
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

private const val GITHUB_URL = "https://github.com/atomofiron/android-block-clock"
private const val ShowPreviewFactory = false

/**
 * Экран настроек виджета: живой предпросмотр, цвет и прозрачность
 * прямоугольников, отступы между ними, цвет текста, формат времени
 * 12/24 и порядок даты.
 * Любое изменение сразу сохраняется и перерисовывает виджет.
 */
@Composable
fun SettingsScreen(uiStarted: Boolean) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(WidgetSettingsStore.load(context)) }

    var previewSettings by remember { mutableStateOf(settings) }
    var colorTarget by remember { mutableStateOf<ColorTarget?>(null) }
    var showLicenses by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun apply(newSettings: WidgetSettings) {
        settings = newSettings
        previewSettings = newSettings
        scope.launch {
            WidgetSettingsStore.save(context, newSettings)
            OneLevelWidget().updateAll(context)
            ThreeLevelWidget().updateAll(context)
            TimeOnlyWidget().updateAll(context)
            DateOnlyWidget().updateAll(context)
        }
    }

    colorTarget?.let { target ->
        ColorPickerDialog(
            title = stringResource(R.string.color),
            initialColor = when (target) {
                ColorTarget.Rect -> settings.background
                else -> settings.text
            },
            onDismiss = { colorTarget = null },
            onConfirm = { color ->
                colorTarget = null
                when (target) {
                    ColorTarget.Rect -> apply(settings.copy(background = color))
                    else -> apply(settings.copy(text = color))
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBackgroundColor(transparent = uiStarted))
            .windowInsetsPadding { displayCutout + statusBars + navigationBars.horizontal() },
    ) {
        when {
            ShowPreviewFactory -> WidgetPreviewFactory(previewSettings)
            else -> WidgetPreview(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Padding.Common),
                settings = previewSettings,
            )
        }

        val gridState = rememberLazyStaggeredGridState()
        val columns = gridState.layoutInfo.visibleItemsInfo
            .maxOfOrNull { it.lane }
            ?.inc() ?: 1
        val clipShape = StaggeredGridClipShape(
            columns = columns,
            padding = Padding.Common,
            cornerRadius = ClipCornerRadius,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = Padding.Common)
                .clip(clipShape),
        ) {
            LazyVerticalStaggeredGrid(
                state = gridState,
                columns = StaggeredGridCells.Adaptive(GridColumnMinWidth),
                modifier = Modifier.fillMaxSize(),
                contentPadding = WindowInsets.navigationBars
                    .only(WindowInsetsSides.Bottom)
                    .add(WindowInsets(bottom = Padding.Common))
                    .asPaddingValues(),
                horizontalArrangement = Arrangement.spacedBy(Padding.Common, Alignment.CenterHorizontally),
                verticalItemSpacing = Padding.Common,
            ) {
                item {
                    SectionCard(stringResource(R.string.color)) {
                        Row {
                            ColorField(
                                modifier = Modifier.padding(end = Padding.Half).weight(1f),
                                label = stringResource(R.string.background),
                                color = settings.background,
                                onClick = { colorTarget = ColorTarget.Rect },
                            )
                            ColorField(
                                modifier = Modifier.padding(start = Padding.Half).weight(1f),
                                label = stringResource(R.string.text),
                                color = settings.text,
                                onClick = { colorTarget = ColorTarget.Text },
                            )
                        }
                        TransparencySlider(
                            transparency = settings.transparency,
                            onChange = {
                                previewSettings = previewSettings.copy(transparency = it)
                            },
                            onChangeFinished = { apply(settings.copy(transparency = it)) },
                        )
                    }
                }
                item {
                    SectionCard(stringResource(R.string.shape)) {
                        RoundingSlider(
                            label = stringResource(R.string.label_corner_radius),
                            value = settings.cornerRadiusDp,
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
                item {
                    SectionCard(title = null) {
                        Row {
                            ClickablePoint(
                                modifier = Modifier.padding(end = Padding.Half).weight(1f),
                                R.drawable.ic_github,
                                R.string.github,
                            ) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, GITHUB_URL.toUri()))
                            }
                            ClickablePoint(
                                modifier = Modifier.padding(start = Padding.Half).weight(1f),
                                R.drawable.ic_license,
                                R.string.licenses,
                            ) {
                                showLicenses = true
                            }
                        }
                        val updateState by UpdateStore.self.state.collectAsState()
                        ClickablePoint(
                            modifier = Modifier.fillMaxWidth(),
                            icon = updateState.icon(),
                            label = updateState.label(),
                            clickable = updateState.interactable,
                            onClick = updateState::action,
                        )
                        ProgressIndicator(
                            modifier = Modifier.fillMaxWidth().offset(y = (-6).dp),
                            progress = updateState.progress(),
                            visible = updateState.processing(),
                        )
                        Row(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val (icon, tint) = when (UpdateStore.self.source) {
                                AppSource.GitHub -> R.drawable.ic_github to ColorFilter.tint(LocalContentColor.current)
                                AppSource.GooglePlay -> R.drawable.ic_google_play to null
                            }
                            Image(
                                modifier = Modifier.size(18.dp),
                                painter = painterResource(icon),
                                colorFilter = tint,
                                contentDescription = null,
                            )
                            Text(
                                modifier = Modifier.padding(start = Padding.Mini),
                                text = stringResource(R.string.version_name),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }
    }
    if (showLicenses) {
        LicensesDialog(onDismiss = { showLicenses = false })
    }
}

@Composable
private fun ProgressIndicator(
    modifier: Modifier,
    progress: Float? = null,
    visible: Boolean = true,
) = when {
    !visible -> LinearProgressIndicator(
        modifier = modifier.alpha(0f),
        progress = { 0f },
    )
    progress == null -> LinearProgressIndicator(modifier = modifier)
    else -> LinearProgressIndicator(
        modifier = modifier,
        progress = { progress },
    )
}

@Composable
private fun ClickablePoint(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    @StringRes label: Int,
    clickable: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(FieldCornerRadius))
            .clickable(enabled = clickable, onClick = onClick)
            .padding(vertical = Padding.Semi),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(IconSize),
            painter = painterResource(icon),
            contentDescription = null,
        )
        Text(
            modifier = Modifier
                .padding(start = Padding.Semi)
                .weight(1f),
            text = stringResource(label),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/** Карточка-секция с необязательным заголовком. */
@Composable
private fun SectionCard(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardCornerRadius),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Padding.Common),
        ) {
            if (title != null) {
                Text(
                    title,
                    modifier = Modifier.padding(bottom = Padding.Half),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            content()
        }
    }
}

@Composable
private fun SubTitle(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Padding.Half, bottom = Padding.Mini),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Плитка текущего цвета — по тапу открывает диалог с градиентным полем. */
@Composable
private fun ColorField(
    modifier: Modifier,
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
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
        Text(
            modifier = Modifier.padding(start = Padding.Semi),
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            overflow = TextOverflow.MiddleEllipsis,
            maxLines = 1,
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
    Column {
        SubTitle(
            title = stringResource(R.string.label_transparency),
            value = "${(value * PercentFactor).toInt()} %",
        )
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
        SubTitle(
            title = stringResource(R.string.label_gap),
            value = value.roundToInt().toString(),
        )
        Slider(
            value = value,
            onValueChange = {
                value = it
                onChange(it.roundToInt())
            },
            onValueChangeFinished = { onChangeFinished(value.roundToInt()) },
            valueRange = GapRange,
            steps = GapRange.steps(),
        )
    }
}

/** Слайдер целого значения в dp: значение показывается справа от подписи. */
@Composable
private fun RoundingSlider(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    onChangeFinished: (Int) -> Unit,
) {
    var current by remember { mutableFloatStateOf(value.toFloat()) }
    Column {
        SubTitle(
            title = label,
            value = current.toInt().toString(),
        )
        Slider(
            value = current,
            onValueChange = {
                current = it
                onChange(current.toInt())
            },
            onValueChangeFinished = { onChangeFinished(current.toInt()) },
            valueRange = RoundingRange,
            steps = RoundingRange.steps() / 2,
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
            .padding(),
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
            Column(verticalArrangement = Arrangement.spacedBy(Padding.Semi)) {
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
                                RoundedCornerShape(DialogSwatchCornerRadius),
                            ),
                    )
                    Text(
                        modifier = Modifier.padding(start = Padding.Semi),
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

private fun UpdateState.icon() = when (this) {
    is UpdateState.Unknown,
    is UpdateState.Checking,
    is UpdateState.Error -> R.drawable.ic_update
    is UpdateState.Available,
    is UpdateState.Downloading,
    is UpdateState.Completable,
    is UpdateState.Installing -> R.drawable.ic_download
    is UpdateState.UpToDate -> R.drawable.ic_circle_check
}

private fun UpdateState.label() = when (this) {
    is UpdateState.Available -> R.string.download_update
    is UpdateState.Downloading -> R.string.update_downloading
    is UpdateState.Completable -> R.string.install_update
    is UpdateState.Installing -> R.string.update_installing
    is UpdateState.Error -> R.string.retry
    is UpdateState.Checking -> R.string.checking
    is UpdateState.Unknown -> R.string.check_updates
    is UpdateState.UpToDate -> R.string.is_up_to_date
}

private fun UpdateState.action() = when (this) {
    is UpdateState.Available -> UpdateService.self.startUpdate(type as UpdateType.Variant)
    is UpdateState.Completable -> UpdateService.self.completeUpdate()
    is UpdateState.Error -> UpdateService.self.retry()
    is UpdateState.Unknown -> UpdateService.self.check(userAction = true)
    is UpdateState.Checking,
    is UpdateState.Downloading,
    is UpdateState.Installing,
    is UpdateState.UpToDate -> Unit
}
