package app.blockclock.ui.values

import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Dp

object Springs {
    val FastSpatialDp = spring<Dp>(dampingRatio = 0.6f, stiffness = 800f)
}