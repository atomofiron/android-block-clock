package app.blockclock.util

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import android.graphics.Color as AndroidColor

fun ClosedRange<Float>.steps() = run { endInclusive - start }.toInt().dec()

fun <T> Flow<T>.collect(scope: CoroutineScope, collector: (T) -> Unit) {
    scope.launch {
        collect(collector)
    }
}

fun <T, V : Any> T.ifNotNull(value: V?, action: T.(V) -> T): T = when (value) {
    null -> this
    else -> action(value)
}

@RequiresApi(Build.VERSION_CODES.O)
fun AndroidColor.toComposeColor() = Color(toArgb())

inline infix fun Int.contains(flags: Int): Boolean = (this and flags) == flags
