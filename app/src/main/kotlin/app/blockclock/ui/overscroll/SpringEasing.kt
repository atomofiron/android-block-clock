package app.blockclock.ui.overscroll

import androidx.compose.animation.core.Easing
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

val SpringEasing = Easing { input ->
    val x = input.coerceIn(0f, 1f)
    when (x) {
        0f -> return@Easing 0f
        1f -> return@Easing 1f
    }
    val dampingRatio = 0.55f
    val cycles = 2f
    val ratio = dampingRatio.coerceIn(0.001f, 5f)
    val natural = 2f * cycles * Math.PI.toFloat() // angular frequency

    if (ratio < 1f) {
        val sqrt = sqrt(1f - ratio.pow(2))
        val damped = natural * sqrt // angular frequency
        val exp = exp(-ratio * natural * x)
        val cos = cos(damped * x)
        val sin = sin(damped * x)

        1f - exp * (cos + sin * ratio / sqrt)
    } else { // without hesitation
        val exp = exp(-natural * x)
        1f - exp * (1f + natural * x)
    }
}