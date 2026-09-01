package app.blockclock

import android.app.WallpaperManager
import android.app.WallpaperManager.FLAG_SYSTEM
import android.graphics.Color
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
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
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
import androidx.compose.ui.graphics.Color as ComposeColor

class MainActivity : AppCompatActivity() {

    private val isEnterAnimationCompleted = mutableStateOf(false)
    private val wallpaperColors = mutableStateOf<WallpaperColors?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navigationBar = ContextCompat.getColor(this, R.color.navigation_bar)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(navigationBar, navigationBar),
        )

        UpdateService.self.onActivityCreate(this)
        UpdateStore.self.alerts.collect(lifecycleScope) {
            Toast.makeText(this, resources[it.text], Toast.LENGTH_LONG).show()
        }
        val store = WidgetSettingsStore(this)
        if (Android.O1) {
            handleWallpaperColors()
            wallpaperColors.value
                ?.takeIf { store.isEmpty() }
                ?.let { store.setColors(it) }
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
    private fun handleWallpaperColors() {
        val manager = WallpaperManager.getInstance(this)
        val listener = WallpaperManager.OnColorsChangedListener { colors, which ->
            if (which contains FLAG_SYSTEM) {
                wallpaperColors.value = colors?.let(::WallpaperColors)
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
        colors
            .run { secondary?.toArgb() ?: primary.toArgb().blackOrWhiteOverIt() }
            .let { background ->
                val text = background.blackOrWhiteOverIt()
                val settings = read().copy(background = ComposeColor(background), text = ComposeColor(text))
                lifecycleScope.launch {
                    store(settings)
                    updateWidgets()
                }
            }
    }

    private fun Int.blackOrWhiteOverIt(): Int {
        val white = ColorUtils.calculateContrast(Color.WHITE, this)
        val black = ColorUtils.calculateContrast(Color.BLACK, this)
        return if (white > black) Color.WHITE else Color.BLACK
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
