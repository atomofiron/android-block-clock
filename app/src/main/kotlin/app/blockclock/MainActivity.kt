package app.blockclock

import android.app.WallpaperManager
import android.graphics.Color
import android.os.Build.VERSION_CODES.O_MR1
import android.os.Bundle
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
import app.blockclock.util.get
import app.blockclock.util.toComposeColor
import app.blockclock.widget.WidgetSettingsStore
import app.blockclock.widget.updateWidgets
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color as ComposeColor

class MainActivity : AppCompatActivity() {

    private var isEnterAnimationCompleted = mutableStateOf(false)

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
        val wallpaperColors = when {
            Android.O1 -> wallpaperColors()
            else -> null
        }
        if (wallpaperColors != null && store.isEmpty()) {
            store.setDefaultColor(wallpaperColors)
        }

        setContent {
            CompositionLocalProvider(
                LocalScreenCorners provides window.decorView.screenCorners(),
            ) {
                AppTheme {
                    SettingsScreen(store, wallpaperColors, isEnterAnimationCompleted.value)
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
    private fun wallpaperColors(): WallpaperColors? {
        val colors = WallpaperManager.getInstance(this@MainActivity)
            .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            ?:  return null
        return WallpaperColors(
            colors.primaryColor.toComposeColor(),
            colors.secondaryColor?.toComposeColor(),
            colors.tertiaryColor?.toComposeColor(),
        )
    }

    /**
     * Sets the widget colors from the wallpaper: the background takes the
     * wallpaper secondary color; when the wallpaper is solid (no secondary
     * color), the background is black or white depending on the contrast
     * with the wallpaper; the text is black or white depending on the
     * contrast with the chosen background.
     */
    private fun WidgetSettingsStore.setDefaultColor(colors: WallpaperColors) {
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
