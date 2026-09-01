package app.blockclock

import android.app.WallpaperManager
import android.app.WallpaperManager.FLAG_SYSTEM
import android.os.Build.VERSION_CODES.O_MR1
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.RoundedCorner
import android.view.View
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import app.blockclock.model.ColorSource
import app.blockclock.model.ColorTarget
import app.blockclock.model.WallpaperColors
import app.blockclock.settings.SettingsScreen
import app.blockclock.ui.LocalScreenCorners
import app.blockclock.ui.insets.InsetsBackground
import app.blockclock.ui.insets.ScreenCorners
import app.blockclock.ui.theme.AppTheme
import app.blockclock.update.UpdateService
import app.blockclock.update.UpdateStore
import app.blockclock.util.Android
import app.blockclock.util.collect
import app.blockclock.util.contains
import app.blockclock.util.get
import app.blockclock.widget.WidgetSettingsStore
import app.blockclock.widget.updateWidgets
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private val isEnterAnimationCompleted = mutableStateOf(false)
    private val wallpaperColors = mutableStateOf<WallpaperColors?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navigationBar = ContextCompat.getColor(this, R.color.navigation_bar)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.auto(navigationBar, navigationBar),
        )

        UpdateService.self.onActivityCreate(this)
        UpdateStore.self.alerts.collect(lifecycleScope) {
            Toast.makeText(this, resources[it.text], Toast.LENGTH_LONG).show()
        }
        val store = WidgetSettingsStore(this)
        if (Android.O1) {
            handleWallpaperColors { colors ->
                wallpaperColors.value = colors
                when {
                    colors == null -> Unit
                    store.isEmpty() -> store.setColors(colors)
                    else -> store.updateColors(colors)
                }
            }
        }
        setContent {
            CompositionLocalProvider(
                LocalScreenCorners provides window.decorView.screenCorners(),
            ) {
                AppTheme {
                    SettingsScreen(store, wallpaperColors.value, isEnterAnimationCompleted.value)
                    InsetsBackground(Modifier.alpha(0.5f))
                }
            }
        }
    }

    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()
        isEnterAnimationCompleted.value = true
    }

    override fun onStop() {
        super.onStop()
        isEnterAnimationCompleted.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) UpdateService.self.completeUpdate()
    }

    @RequiresApi(O_MR1)
    private fun handleWallpaperColors(consumer: (WallpaperColors?) -> Unit) {
        val manager = WallpaperManager.getInstance(this)
        val listener = WallpaperManager.OnColorsChangedListener { colors, which ->
            if (which contains FLAG_SYSTEM) {
                consumer(colors?.let(::WallpaperColors))
            }
        }
        manager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
        object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                manager.removeOnColorsChangedListener(listener)
                owner.lifecycle.removeObserver(this)
            }
        }.let { lifecycle.addObserver(it) }
        val colors = manager.getWallpaperColors(FLAG_SYSTEM)
        listener.onColorsChanged(colors, FLAG_SYSTEM)
    }

    /**
     * Sets the widget colors from the wallpaper: the background takes the
     * wallpaper secondary color; when the wallpaper is solid (no secondary
     * color), the background is black or white depending on the contrast
     * with the wallpaper; the text is black or white depending on the
     * contrast with the chosen background.
     */
    private fun WidgetSettingsStore.setColors(colors: WallpaperColors) {
        colors.run {
            secondary?.let { it.toArgb() to ColorSource.Secondary }
                ?: (primary.toArgb() to ColorSource.Primary)
        }.let { (color, source) -> Color(color) to source }
            .let { (background, source) ->
                val text = background.blackOrWhiteOverIt()
                val settings = read().copy(background = background, text = text)
                store(settings, ColorTarget.Rect, source)
                lifecycleScope.launch {
                    updateWidgets()
                }
            }
    }

    private fun WidgetSettingsStore.updateColors(colors: WallpaperColors) {
        val sources = readSources()
        if (sources.rect.manual() && sources.text.manual()) {
            return
        }
        val contrast = readContrast()
        val settings = read()
        val textColors = when (sources.text) {
            ColorSource.Manual -> listOfNotNull(
                settings.text,
                Color.White.takeIf { settings.text == Color.Black },
                Color.Black.takeIf { settings.text == Color.White },
            )
            ColorSource.Primary -> listOfNotNull(colors.primary, colors.secondary, colors.tertiary)
            ColorSource.Secondary -> listOfNotNull(colors.secondary, colors.primary, colors.tertiary)
            ColorSource.Tertiary -> listOfNotNull(colors.tertiary, colors.primary, colors.secondary)
        }
        val rectColors = when (sources.rect) {
            ColorSource.Manual -> listOfNotNull(
                settings.background,
                Color.White.takeIf { settings.background == Color.Black },
                Color.Black.takeIf { settings.background == Color.White },
            )
            ColorSource.Primary -> listOfNotNull(colors.primary, colors.secondary, colors.tertiary)
            ColorSource.Secondary -> listOfNotNull(colors.secondary, colors.primary, colors.tertiary)
            ColorSource.Tertiary -> listOfNotNull(colors.tertiary, colors.primary, colors.secondary)
        }
        val candidates = buildList {
            for (rect in rectColors) for (text in textColors) {
                val c = ColorUtils.calculateContrast(text.toArgb(), rect.toArgb())
                add(Triple(rect, text, abs(c.toFloat() - contrast)))
            }
        }
        val (rect, text) = candidates.minBy { it.third }
        if (settings.background != rect || settings.text != text) {
            store(settings.copy(background = rect, text = text))
            lifecycleScope.launch {
                updateWidgets()
            }
        }
    }

    private fun Color.blackOrWhiteOverIt(): Color {
        val white = ColorUtils.calculateContrast(Color.White.toArgb(), toArgb())
        val black = ColorUtils.calculateContrast(Color.Black.toArgb(), toArgb())
        return if (white > black) Color.White else Color.Black
    }

    private fun View.screenCorners(): ScreenCorners = when {
        Android.S -> rootWindowInsets?.run {
            ScreenCorners(
                topLeft = getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0,
                topRight = getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.radius ?: 0,
                bottomRight = getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius ?: 0,
                bottomLeft = getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius ?: 0,
            )
        } ?: ScreenCorners.Zero
        else -> ScreenCorners.Zero
    }
}
