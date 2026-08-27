package app.blockclock.ui.theme

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.S as AndroidS
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.blockclock.ui.values.Colors

private val DarkColorScheme = darkColorScheme(
    primary = Colors.Purple80,
    secondary = Colors.PurpleGrey80,
    tertiary = Colors.Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Colors.Purple40,
    secondary = Colors.PurpleGrey40,
    tertiary = Colors.Pink40
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        SDK_INT >= AndroidS -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}