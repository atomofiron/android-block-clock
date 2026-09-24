package app.blockclock.ui.overscroll

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

internal class NestedSpringScrollConnection(
    private val effect: SpringOverscrollEffect,
) : NestedScrollConnection {

    private val performScroll: (Offset) -> Offset = { Offset.Zero }
    private val performFling: suspend (Velocity) -> Velocity = { Velocity.Zero }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (available.y != 0f) {
            effect.applyToScroll(available, source, performScroll)
        }
        return Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (effect.isInProgress) {
            effect.applyToFling(available, performFling)
        }
        return Velocity.Zero
    }
}
