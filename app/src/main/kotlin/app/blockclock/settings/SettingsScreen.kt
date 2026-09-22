package app.blockclock.settings

import android.content.Intent
import android.os.Build.VERSION_CODES.Q
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.blockclock.AbstractApp
import app.blockclock.R
import app.blockclock.licenses.LicensesScreen
import app.blockclock.model.AppPickerTarget
import app.blockclock.model.ColorSource
import app.blockclock.model.ColorTarget
import app.blockclock.model.FontAxis
import app.blockclock.model.FontAxis.Companion.SLANTS
import app.blockclock.model.TextStyle
import app.blockclock.model.WallpaperColors
import app.blockclock.model.WidgetFont
import app.blockclock.ui.ColorBox
import app.blockclock.ui.ForwardIcon
import app.blockclock.ui.GroupItem
import app.blockclock.ui.SegmentedButton
import app.blockclock.ui.insets.InsetsBackground
import app.blockclock.ui.values.Dimens
import app.blockclock.ui.values.Padding
import app.blockclock.ui.values.clickable
import app.blockclock.update.AppSource
import app.blockclock.update.UpdateService
import app.blockclock.update.UpdateStore
import app.blockclock.update.model.UpdateState
import app.blockclock.update.model.UpdateType
import app.blockclock.util.Android
import app.blockclock.util.Percents
import app.blockclock.util.animatedBackgroundColor
import app.blockclock.util.familyStyles
import app.blockclock.util.getSystemFonts
import app.blockclock.util.horizontal
import app.blockclock.util.plus
import app.blockclock.util.rememberAppIconPainter
import app.blockclock.util.steps
import app.blockclock.util.toFontFamily
import app.blockclock.util.windowInsetsPadding
import app.blockclock.widget.WidgetSettings
import app.blockclock.widget.WidgetSettingsStore
import app.blockclock.widget.defaultCalendarApp
import app.blockclock.widget.defaultClockApp
import app.blockclock.widget.updateWidgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val TransparencyRange = 0f..1f
private val RoundingRange = 0f..32f
private val GapRange = 0f..16f
private val FontSizeRange = 68f..132f

private const val GITHUB_URL = "https://github.com/atomofiron/android-block-clock"
private const val ShowPreviewFactory = false

/**
 * The widget settings screen: live preview, rectangle color and
 * transparency, spacing, text color, 12/24 time format and date order.
 * Every change is saved and redraws the widget immediately.
 */
@Composable
fun SettingsScreen(
    store: WidgetSettingsStore,
    wallpaperColors: WallpaperColors?,
    uiStarted: Boolean,
) {
    val context = LocalContext.current
    var settings by remember(wallpaperColors) { mutableStateOf(store.read()) }

    var previewSettings by remember(settings) { mutableStateOf(settings) }
    var colorTarget by remember { mutableStateOf<ColorTarget?>(null) }
    var showLicenses by remember { mutableStateOf(false) }
    var appPicker by remember { mutableStateOf<AppPickerTarget?>(null) }
    var showFontPicker by remember { mutableStateOf(false) }
    var systemFonts by remember { mutableStateOf<List<WidgetFont>>(emptyList()) }

    LaunchedEffect(Unit) {
        if (Android.Q) {
            systemFonts = withContext(Dispatchers.Default) { getSystemFonts() }
        }
    }

    fun apply(
        newSettings: WidgetSettings,
        target: ColorTarget? = null,
        source: ColorSource? = null,
    ) {
        settings = newSettings
        previewSettings = newSettings
        store.store(newSettings, target, source, saveContrast = true)
        // The refresh outlives this screen: a scope of the composition would drop it on the way out.
        AbstractApp.scope.launch(Dispatchers.Main) {
            context.applicationContext.updateWidgets()
        }
    }

    colorTarget?.let { target ->
        ColorPickerDialog(
            title = when (target) {
                ColorTarget.Rect -> stringResource(R.string.background_color)
                ColorTarget.Text -> stringResource(R.string.text_color)
            },
            initialColor = when (target) {
                ColorTarget.Rect -> settings.background
                ColorTarget.Text -> settings.text
            },
            wallpaperColors,
            onDismiss = { colorTarget = null },
            onConfirm = { color, source ->
                colorTarget = null
                when (target) {
                    ColorTarget.Rect -> settings.copy(background = color)
                    ColorTarget.Text -> settings.copy(text = color)
                }.let {
                    apply(it, target, source)
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
            cornerRadius = Dimens.ClipCornerRadius,
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
                columns = StaggeredGridCells.Adaptive(Dimens.GridColumnMinWidth),
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
                        Row(
                            Modifier.padding(horizontal = Padding.Common),
                            horizontalArrangement = Arrangement.spacedBy(Padding.Half),
                        ) {
                            ColorField(
                                modifier = Modifier.weight(1f),
                                label = stringResource(R.string.background),
                                color = settings.background,
                                onClick = { colorTarget = ColorTarget.Rect },
                            )
                            ColorField(
                                modifier = Modifier.weight(1f),
                                label = stringResource(R.string.text),
                                color = settings.text,
                                onClick = { colorTarget = ColorTarget.Text },
                            )
                        }
                        TransparencySlider(
                            modifier = Modifier.padding(horizontal = Padding.Common),
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
                            modifier = Modifier.padding(horizontal = Padding.Common),
                            label = stringResource(R.string.label_corner_radius),
                            value = settings.cornerRadiusDp,
                            onChange = {
                                previewSettings = previewSettings.copy(cornerRadiusDp = it)
                            },
                            onChangeFinished = { apply(settings.copy(cornerRadiusDp = it)) },
                        )
                        GapSlider(
                            modifier = Modifier.padding(horizontal = Padding.Common),
                            gap = settings.gapDp,
                            onChange = { previewSettings = previewSettings.copy(gapDp = it) },
                            onChangeFinished = { apply(settings.copy(gapDp = it)) },
                        )
                    }
                }
                item {
                    SectionCard(stringResource(R.string.font)) {
                        Row(
                            modifier = Modifier.padding(horizontal = Padding.Common),
                            horizontalArrangement = Arrangement.spacedBy(Padding.Common),
                        ) {
                            SliderPoint(
                                modifier = Modifier.weight(1f),
                                label = stringResource(R.string.label_time_size),
                                percent = previewSettings.timeFontPercent,
                                onChange = { previewSettings = previewSettings.copy(timeFontPercent = it) },
                                onChangeFinished = { apply(settings.copy(timeFontPercent = it)) },
                            )
                            SliderPoint(
                                modifier = Modifier.weight(1f),
                                label = stringResource(R.string.label_date_size),
                                percent = previewSettings.dateFontPercent,
                                onChange = { previewSettings = previewSettings.copy(dateFontPercent = it) },
                                onChangeFinished = { apply(settings.copy(dateFontPercent = it)) },
                            )
                        }
                        when {
                            Android.Q -> {
                                FontField(
                                    modifier = Modifier
                                        .padding(horizontal = Padding.Common)
                                        .fillMaxWidth(),
                                    font = settings.font,
                                    onClick = { showFontPicker = true },
                                )
                                FontVariations(
                                    font = settings.font,
                                    textStyle = settings.textStyle,
                                    systemFonts = systemFonts,
                                    onFont = { apply(settings.copy(font = it)) },
                                    onStyle = { apply(settings.copy(textStyle = it)) },
                                )
                            }
                            // The system fonts need Android 10, the styles of the default font do not.
                            else -> TextStyleGroup(
                                contentPadding = Padding.Common,
                                selected = settings.textStyle,
                                onStyle = { apply(settings.copy(textStyle = it)) },
                            )
                        }
                    }
                }
                item {
                    SectionCard(title = null) {
                        val clockApp = remember(settings.clockApp) { settings.clockApp ?: defaultClockApp(context) }
                        val calendarApp = remember(settings.calendarApp) { settings.calendarApp ?: defaultCalendarApp(context) }
                        Row(
                            modifier = Modifier.padding(horizontal = Padding.Common),
                            horizontalArrangement = Arrangement.spacedBy(Padding.Half),
                        ) {
                            ClickablePoint(
                                modifier = Modifier.weight(1f),
                                icon = rememberAppIconPainter(clockApp?.packageName),
                                label = R.string.clock_app,
                                tintedIcon = false,
                                largeIcon = true,
                                withArrow = true,
                            ) {
                                appPicker = AppPickerTarget.Clock
                            }
                            ClickablePoint(
                                modifier = Modifier.weight(1f),
                                icon = rememberAppIconPainter(calendarApp?.packageName),
                                label = R.string.calendar_app,
                                tintedIcon = false,
                                largeIcon = true,
                                withArrow = true,
                            ) {
                                appPicker = AppPickerTarget.Calendar
                            }
                        }
                        Row(
                            modifier = Modifier.padding(horizontal = Padding.Common),
                            horizontalArrangement = Arrangement.spacedBy(Padding.Half),
                        ) {
                            if (!store.systemDayFirst) SettingSwitch(
                                modifier = Modifier.weight(1f),
                                label = stringResource(R.string.option_month_first),
                                checked = !settings.dayFirst,
                                onCheckedChange = { checked ->
                                    apply(settings.copy(dayFirst = !checked))
                                },
                            )
                            // The marker belongs to the 12-hour clock: the system is its single source.
                            if (store.systemAmPm) SettingSwitch(
                                modifier = Modifier.weight(1f),
                                label = stringResource(R.string.option_am_pm),
                                checked = settings.amPm,
                                onCheckedChange = { checked ->
                                    apply(settings.copy(amPm = checked))
                                },
                            )
                        }
                    }
                }
                item {
                    SectionCard(title = null) {
                        Row(
                            modifier = Modifier.padding(horizontal = Padding.Common),
                            horizontalArrangement = Arrangement.spacedBy(Padding.Half),
                        ) {
                            ClickablePoint(
                                modifier = Modifier.weight(1f),
                                icon = painterResource(R.drawable.ic_github),
                                label = R.string.github_repository,
                            ) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, GITHUB_URL.toUri()))
                            }
                            ClickablePoint(
                                modifier = Modifier.weight(1f),
                                icon = painterResource(R.drawable.ic_license),
                                label = R.string.licenses,
                                withArrow = true,
                            ) {
                                showLicenses = true
                            }
                        }
                        val updateState by UpdateStore.self.state.collectAsState()
                        ClickablePoint(
                            modifier = Modifier
                                .padding(horizontal = Padding.Common)
                                .fillMaxWidth(),
                            icon = painterResource(updateState.icon()),
                            label = updateState.label(),
                            clickable = updateState.interactable,
                            onClick = updateState::action,
                        )
                        ProgressIndicator(
                            modifier = Modifier
                                .padding(horizontal = Padding.Common)
                                .fillMaxWidth(),
                            progress = updateState.progress(),
                            visible = updateState.processing(),
                        )
                        Row(
                            modifier = Modifier
                                .padding(horizontal = Padding.Common)
                                .align(Alignment.CenterHorizontally),
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
    InsetsBackground(Modifier.alpha(0.5f))
    if (showLicenses) {
        LicensesScreen(onClose = { showLicenses = false })
    }
    if (showFontPicker && Android.Q) {
        FontPickerScreen(
            onPick = { font ->
                showFontPicker = false
                apply(settings.copy(font = font))
            },
            onClose = { showFontPicker = false },
        )
    }
    appPicker?.let { target ->
        AppPickerScreen(
            target,
            onPick = { app ->
                appPicker = null
                when (target) {
                    AppPickerTarget.Clock -> apply(settings.copy(clockApp = app))
                    AppPickerTarget.Calendar -> apply(settings.copy(calendarApp = app))
                }
            },
            onClose = { appPicker = null },
        )
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
    icon: Painter,
    @StringRes label: Int,
    clickable: Boolean = true,
    tintedIcon: Boolean = true,
    largeIcon: Boolean = false,
    withArrow: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(ShapeDefaults.Medium)
            .background(colorScheme.clickable)
            .clickable(enabled = clickable, onClick = onClick)
            .padding(Padding.Semi),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val iconSize = if (largeIcon) Dimens.LargeIconSize else Dimens.IconSize
        when {
            tintedIcon -> Icon(
                modifier = Modifier.size(iconSize),
                painter = icon,
                contentDescription = null,
            )
            else -> Image(
                modifier = Modifier.size(iconSize),
                painter = icon,
                contentDescription = null,
            )
        }
        Text(
            modifier = Modifier
                .padding(start = Padding.Semi)
                .weight(1f),
            text = stringResource(label),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (withArrow) ForwardIcon()
    }
}

@Composable
private fun SectionCard(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeDefaults.ExtraLarge,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Padding.Common),
            verticalArrangement = Arrangement.spacedBy(Padding.Half),
        ) {
            if (title != null) {
                Text(
                    title,
                    modifier = Modifier.padding(bottom = Padding.Half, start = Padding.Common),
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.primary,
                )
            }
            content()
        }
    }
}

@Composable
private fun SubTitle(title: String, value: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Padding.Half, bottom = Padding.Mini),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
        )
        if (value != null) Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ColorField(
    modifier: Modifier,
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(ShapeDefaults.Medium)
            .background(colorScheme.clickable)
            .clickable(onClick = onClick)
            .padding(Padding.Semi),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorBox(Modifier.size(Dimens.SwatchSize), color)
        Text(
            modifier = Modifier
                .padding(start = Padding.Semi)
                .weight(1f),
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            overflow = TextOverflow.MiddleEllipsis,
            maxLines = 1,
        )
    }
}

/** The font row: the font file name rendered in that very font. */
@RequiresApi(Q)
@Composable
private fun FontField(
    modifier: Modifier,
    font: WidgetFont?,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(ShapeDefaults.Medium)
            .background(colorScheme.clickable)
            .clickable(onClick = onClick)
            .padding(Padding.Semi),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = font?.name ?: stringResource(R.string.font_default),
            fontFamily = font?.toFontFamily(),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        ForwardIcon()
    }
}

/**
 * The font variations: the style of the default font as a group of buttons, a note that the picked
 * font applies as far as the platform allows and the axes a widget renders as sliders
 * ([FontAxis.DELIVERED]), or the styles of the same font family as a group of buttons — the group
 * of a family with a single style is not shown: there is nothing to choose.
 */
@RequiresApi(Q)
@Composable
private fun FontVariations(
    modifier: Modifier = Modifier,
    font: WidgetFont?,
    textStyle: TextStyle,
    systemFonts: List<WidgetFont>,
    onFont: (WidgetFont?) -> Unit,
    onStyle: (TextStyle) -> Unit,
) {
    when {
        font == null -> TextStyleGroup(
            modifier = modifier,
            contentPadding = Padding.Common,
            selected = textStyle,
            onStyle = onStyle,
        )
        else -> {
            Text(
                text = stringResource(R.string.font_note),
                modifier = modifier.padding(horizontal = Padding.Common),
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
            )
            when {
                font.vf -> font.axes.filter { it.tag in FontAxis.DELIVERED }.forEach { axis ->
                    VariationSlider(
                        modifier = modifier.padding(horizontal = Padding.Common),
                        axis = axis,
                        value = font.variation(axis.tag, axis.default),
                        withSteps = axis.tag in SLANTS,
                        onChange = { onFont(font.copy(variations = font.variations + (axis.tag to it))) },
                    )
                }
                else -> remember(font, systemFonts) { systemFonts.familyStyles(font) }
                    .takeIf { it.size > 1 }
                    ?.let {
                        StyleGroup(
                            modifier = modifier,
                            contentPadding = Padding.Common,
                            styles = it,
                            selected = font,
                            onFont = onFont,
                        )
                    }
            }
        }
    }
}

/** The slider of one variable font axis, e.g. `wght`. */
@Composable
private fun VariationSlider(
    modifier: Modifier = Modifier,
    axis: FontAxis,
    value: Float,
    withSteps: Boolean,
    onChange: (Float) -> Unit,
) {
    var current by remember(axis, value) { mutableFloatStateOf(value) }
    Column(modifier) {
        SubTitle(
            title = axis.tag,
            value = current.roundToInt().toString(),
        )
        Slider(
            value = current,
            onValueChange = { current = it },
            onValueChangeFinished = { onChange(current.roundToInt().toFloat()) },
            valueRange = axis.range,
            steps = if (withSteps) axis.range.steps() else 0,
        )
    }
}

/** The group button with the text styles of the default font. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextStyleGroup(
    modifier: Modifier = Modifier,
    selected: TextStyle,
    contentPadding: Dp = 0.dp,
    onStyle: (TextStyle) -> Unit,
) {
    SegmentedButton(modifier.fillMaxWidth(), contentPadding) {
        TextStyle.entries.forEachIndexed { index, style ->
            GroupItem(
                index = index,
                count = TextStyle.entries.size,
                selected = style == selected,
                onClick = { onStyle(style) },
            ) {
                Text(
                    text = stringResource(style.label()),
                    maxLines = 1,
                )
            }
        }
    }
}

/** The label of the text style. */
@StringRes
private fun TextStyle.label(): Int = when (this) {
    TextStyle.Normal -> R.string.style_normal
    TextStyle.Bold -> R.string.style_bold
    TextStyle.Italic -> R.string.style_italic
}

/** The group button with the styles of the font family. */
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Q)
@Composable
private fun StyleGroup(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 0.dp,
    styles: List<WidgetFont>,
    selected: WidgetFont,
    onFont: (WidgetFont?) -> Unit,
) {
    SegmentedButton(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
    ) {
        styles.forEachIndexed { index, style ->
            GroupItem(
                index = index,
                count = styles.size,
                selected = style.path == selected.path && style.ttcIndex == selected.ttcIndex,
                onClick = { onFont(style) },
            ) {
                Text(
                    text = style.style.ifEmpty { style.family },
                    maxLines = 1,
                )
            }
        }
    }
}

/** The transparency slider: the higher the value, the more transparent the rectangles. */
@Composable
private fun TransparencySlider(
    modifier: Modifier = Modifier,
    transparency: Float,
    onChange: (Float) -> Unit,
    onChangeFinished: (Float) -> Unit,
) {
    var value by remember { mutableFloatStateOf(transparency) }
    Column(modifier) {
        SubTitle(
            title = stringResource(R.string.label_transparency),
            value = "${(value * Percents).toInt()} %",
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

@Composable
private fun GapSlider(
    modifier: Modifier = Modifier,
    gap: Int,
    onChange: (Int) -> Unit,
    onChangeFinished: (Int) -> Unit,
) {
    var value by remember { mutableFloatStateOf(gap.toFloat().coerceIn(GapRange)) }
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
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

@Composable
private fun RoundingSlider(
    modifier: Modifier = Modifier,
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    onChangeFinished: (Int) -> Unit,
) {
    var current by remember { mutableFloatStateOf(value.toFloat()) }
    Column(modifier) {
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

/** A settings row with a Switch: the whole row is clickable. */
/** The slider of the text size: the percentage of the cell height the font takes. */
@Composable
private fun SliderPoint(
    modifier: Modifier = Modifier,
    label: String,
    percent: Int,
    onChange: (Int) -> Unit,
    onChangeFinished: (Int) -> Unit,
) {
    var current by remember { mutableFloatStateOf(percent.toFloat().coerceIn(FontSizeRange)) }
    Column(modifier) {
        SubTitle(
            title = label,
            value = "${if (current >= Percents) "+" else ""}${(current - Percents).toInt()}%",
        )
        Slider(
            value = current,
            valueRange = FontSizeRange,
            steps = 7,
            onValueChange = {
                current = it
                onChange(it.roundToInt())
            },
            onValueChangeFinished = { onChangeFinished(current.roundToInt()) },
        )
    }
}

@Composable
private fun SettingSwitch(
    modifier: Modifier = Modifier,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ShapeDefaults.Medium)
            .background(colorScheme.clickable)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = Padding.Semi, vertical = Padding.Mini),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            lineHeight = MaterialTheme.typography.titleSmall.lineHeight,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun UpdateState.icon() = when (this) {
    is UpdateState.Unknown,
    is UpdateState.Checking,
    is UpdateState.Error -> R.drawable.ic_retry
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
