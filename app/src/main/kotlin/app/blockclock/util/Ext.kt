package app.blockclock.util

import android.graphics.fonts.SystemFonts
import android.os.Build.VERSION_CODES.Q
import android.widget.Toast
import androidx.annotation.RequiresApi
import app.blockclock.AbstractApp
import app.blockclock.BuildConfig
import app.blockclock.model.WidgetFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun Throwable.forHumans() = "${this::class.simpleName}: $message"

fun stub(): String = "check the stack trace"

inline fun debug(action: () -> Unit) = when {
    BuildConfig.DEBUG -> action()
    else -> Unit
}

inline fun Any.debugFail(lazyMessage: () -> Any = ::stub) = debugRequire(false, lazyMessage)

fun Any.debugFailUnreachable() = debugFail { "unreachable?" }

@Suppress("OPT_IN_USAGE")
inline fun Any.debugRequire(value: Boolean, lazyMessage: () -> Any = ::stub)  {
    if (BuildConfig.DEBUG) require(value) {
        val message = "$simpleName: ${lazyMessage()}"
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(AbstractApp.self, message, Toast.LENGTH_LONG).show()
        }
        message
    }
}

val Any?.simpleName: String get() = when {
    this == null -> null
    else -> this::class.java.simpleName
}.toString()

/**
 * The fonts of the system the picker offers: only the ones the home screen can draw.
 *
 * A picked font reaches the widget as a family name the host resolves itself ([toCellFont]):
 * a typeface would not survive the process boundary, and the host silently draws its own
 * default for a name it does not know. Nearly every file of the system belongs to a family
 * the configuration does not name: [toCellFont] answers null for those, the host draws the
 * default for them, and the widget would ignore the pick. They are left out of the list.
 */
@RequiresApi(Q)
fun getSystemFonts(): List<WidgetFont> = when {
    Android.SupportFonts -> SystemFonts.getAvailableFonts()
        .asSequence()
        .mapNotNull { it.toWidgetFont() }
        .filter { it.toCellFont() != null }
        .distinctBy { it.name }
        .sortedBy { it.name }
        .toList()
    else -> emptyList()
}
