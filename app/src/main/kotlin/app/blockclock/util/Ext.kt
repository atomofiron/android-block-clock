package app.blockclock.util

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import app.blockclock.AbstractApp
import app.blockclock.BuildConfig
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

inline fun Any.debugRequireNotNull(any: Any?, lazyMessage: () -> Any = ::stub) = debugRequire(any != null, lazyMessage)

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

val Any?.className: String get() = when {
    this == null -> null
    else -> this::class.java.name
}.toString()

fun <T> T.takeIfDebug(): T? = if (BuildConfig.DEBUG) this else null

fun Context.granted(permission: String) = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
