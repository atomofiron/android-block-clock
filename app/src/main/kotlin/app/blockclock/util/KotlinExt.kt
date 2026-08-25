package app.blockclock.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

fun ClosedRange<Float>.steps() = run { endInclusive - start }.toInt().dec()

fun <T> Flow<T>.collect(scope: CoroutineScope, collector: (T) -> Unit) {
    scope.launch {
        collect(collector)
    }
}
