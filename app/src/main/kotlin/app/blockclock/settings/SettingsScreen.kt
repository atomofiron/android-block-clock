package app.blockclock.settings

import android.content.Intent
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.blockclock.R
import app.blockclock.licenses.LicensesScreen
import app.blockclock.model.AppPickerTarget
import app.blockclock.model.ColorTarget
import app.blockclock.model.WallpaperColors
import app.blockclock.ui.ColorBox
import app.blockclock.ui.values.Dimens
import app.blockclock.ui.values.Padding
import app.blockclock.update.AppSource
import app.blockclock.update.UpdateService
import app.blockclock.update.UpdateStore
import app.blockclock.update.model.UpdateState
import app.blockclock.update.model.UpdateType
import app.blockclock.util.animatedBackgroundColor
import app.blockclock.util.horizontal
import app.blockclock.util.plus
import app.blockclock.util.rememberAppIconPainter
import app.blockclock.util.steps
import app.blockclock.util.windowInsetsPadding
import app.blockclock.widget.WidgetSettings
import app.blockclock.widget.WidgetSettingsStore
import app.blockclock.widget.defaultCalendarApp
import app.blockclock.widget.defaultClockApp
import app.blockclock.widget.updateWidgets
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val PercentFactor = 100f
private val TransparencyRange = 0f..1f
private val RoundingRange = 0f..32f
private val GapRange = 0f..16f

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
    var settings by remember { mutableStateOf(store.read()) }

    var previewSettings by remember { mutableStateOf(settings) }
    var colorTarget by remember { mutableStateOf<ColorTarget?>(null) }
    var showLicenses by remember { mutableStateOf(false) }
    var appPicker by remember { mutableStateOf<AppPickerTarget?>(null) }
    val scope = rememberCoroutineScope()

    fun apply(newSettings: WidgetSettings) {
        settings = newSettings
        previewSettings = newSettings
        scope.launch {
            store.store(newSettings)
            context.updateWidgets()
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
                else -> settings.text
            },
            wallpaperColors,
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
                        val clockApp = remember(settings.clockApp) { settings.clockApp ?: defaultClockApp(context) }
                        val calendarApp = remember(settings.calendarApp) { settings.calendarApp ?: defaultCalendarApp(context) }
                        Row {
                            ClickablePoint(
                                modifier = Modifier.padding(end = Padding.Half).weight(1f),
                                icon = rememberAppIconPainter(clockApp?.packageName),
                                label = R.string.clock_app,
                                tintedIcon = false,
                                largeIcon = true,
                            ) {
                                appPicker = AppPickerTarget.Clock
                            }
                            ClickablePoint(
                                modifier = Modifier.padding(start = Padding.Half).weight(1f),
                                icon = rememberAppIconPainter(calendarApp?.packageName),
                                label = R.string.calendar_app,
                                tintedIcon = false,
                                largeIcon = true,
                            ) {
                                appPicker = AppPickerTarget.Calendar
                            }
                        }
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
                                icon = painterResource(R.drawable.ic_github),
                                R.string.github,
                            ) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, GITHUB_URL.toUri()))
                            }
                            ClickablePoint(
                                modifier = Modifier.padding(start = Padding.Half).weight(1f),
                                painterResource(R.drawable.ic_license),
                                R.string.licenses,
                            ) {
                                showLicenses = true
                            }
                        }
                        val updateState by UpdateStore.self.state.collectAsState()
                        ClickablePoint(
                            modifier = Modifier.fillMaxWidth(),
                            icon = painterResource(updateState.icon()),
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
        LicensesScreen(onClose = { showLicenses = false })
    }
    appPicker?.let { picker ->
        AppPickerScreen(
            title = when (picker) {
                AppPickerTarget.Clock -> R.string.clock_app
                AppPickerTarget.Calendar -> R.string.calendar_app
            }.let { stringResource(it) },
            onPick = { target ->
                appPicker = null
                when (picker) {
                    AppPickerTarget.Clock -> apply(settings.copy(clockApp = target))
                    AppPickerTarget.Calendar -> apply(settings.copy(calendarApp = target))
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
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(ShapeDefaults.Medium)
            .clickable(enabled = clickable, onClick = onClick)
            .padding(vertical = Padding.Semi),
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
        )
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
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.FieldVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorBox(Modifier.size(Dimens.SwatchSize), color)
        Text(
            modifier = Modifier.padding(start = Padding.Semi),
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            overflow = TextOverflow.MiddleEllipsis,
            maxLines = 1,
        )
    }
}

/** The transparency slider: the higher the value, the more transparent the rectangles. */
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

/** A settings row with a Switch: the whole row is clickable. */
@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeDefaults.Medium)
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
