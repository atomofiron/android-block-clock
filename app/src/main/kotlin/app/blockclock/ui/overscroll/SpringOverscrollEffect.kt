package app.blockclock.ui.overscroll

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.overscroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import app.blockclock.ui.overscroll.SpringOverscrollEffect.Companion.applyOverscroll
import app.blockclock.ui.values.Duration
import app.blockclock.util.Epsilon
import app.blockclock.util.invoke
import kotlinx.coroutines.CoroutineScope
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

@Composable
fun <T> T.Overscroll(
    scope: CoroutineScope = rememberCoroutineScope(),
    content: @Composable T.(columnModifier: Modifier, itemModifier: Modifier) -> Unit,
) {
    val effect = remember(scope) { SpringOverscrollEffect(scope) }
    val factory = remember(effect) { OverscrollFactoryImpl(effect) }
    val connection = remember { NestedSpringScrollConnection(effect) }
    CompositionLocalProvider(LocalOverscrollFactory provides factory) {
        this@Overscroll.content(
            Modifier.fillMaxSize()
                .nestedScroll(connection)
                .applyOverscroll(effect),
            Modifier.overscrollItem(effect),
        )
    }
}

@Composable
fun Overscroll(
    scope: CoroutineScope = rememberCoroutineScope(),
    content: @Composable (columnModifier: Modifier, itemModifier: Modifier) -> Unit,
) {
    Unit.Overscroll(scope) { p1, p2 ->
        content(p1, p2)
    }
}

private const val SCRILL_SPEED_BUFFER = 10
private const val SCRILL_SPEED_BUFFER_MIN = 5
private const val HALF_DURATION = 512

@OptIn(ExperimentalFoundationApi::class)
internal class SpringOverscrollEffect(private val scope: CoroutineScope) : OverscrollEffect {
    companion object {
        internal fun Modifier.applyOverscroll(effect: SpringOverscrollEffect): Modifier = pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(pass = PointerEventPass.Initial)
                    effect.onDown(down.position.y)
                    var prev = down
                    do {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val drag = event.changes.firstOrNull()
                        if (drag != null && drag.pressed) {
                            when (drag.id) {
                                prev.id -> effect.onDrag(drag.position.y)
                                else -> effect.onDown(drag.position.y)
                            }
                            prev = drag
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
        }.overscroll(effect)
    }
    enum class Direction(val sign: Int) {
        Up(-1),
        Down(1),
        ;
        companion object {
            operator fun invoke(overscroll: Float) = if (overscroll < 0f) Up else Down
        }
    }

    private var maxVelocity = 24000f
    private var queue = ArrayDeque<Pair<Long, Float>>()
    private var isFlinging = false
    private val zeroOverscroll = 0f
    private var height = 0f
    private var targetValue = 0f
    val overscroll = Animatable(zeroOverscroll)
    var direction = Direction.Down
        private set
    var startY = 0f
        private set
    var pullY = 0f
        private set

    override val isInProgress: Boolean get() = overscroll.value != zeroOverscroll

    override val node: DelegatableNode = object : Modifier.Node(), LayoutModifierNode {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints,
        ): MeasureResult {
            val placeable = measurable.measure(constraints)
            height = placeable.height.toFloat()
            return layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
    }

    private fun onDown(y: Float) {
        startY = y
        pullY = y
        scope { snapTo(zeroOverscroll) }
    }

    private fun onDrag(y: Float) {
        pullY = y
    }

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset {
        if (abs(delta.y) < Epsilon) {
            // The effect is vertical: the delta of the other axis — the segmented button row
            // scrolling on its own — is not a pull and has to reach the scroll itself.
            return performScroll(delta)
        }
        if (isFlinging) {
            queue.add(System.currentTimeMillis() to delta.y)
            if (queue.size > SCRILL_SPEED_BUFFER) {
                queue.removeFirst()
            }
            return performScroll(delta)
        }
        var newValue = targetValue + delta.y
        var consumed = 0f
        if (newValue != zeroOverscroll && (targetValue.sign == zeroOverscroll || targetValue.sign != newValue.sign)) {
            consumed = performScroll(delta.copy(y = newValue)).y
        }
        newValue -= consumed
        direction = Direction(newValue)
        scope { snapTo(newValue) }
        return delta
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity,
    ) {
        maxVelocity = max(abs(velocity.y), maxVelocity)
        val absolute = abs(targetValue)
        val duration = if (absolute < Epsilon) {
            snapTo(zeroOverscroll)
            val velocityY = performFlingScroll(velocity, performFling) ?: velocity.y
            if (abs(velocityY) < Epsilon) {
                return
            }
            flingOverscroll(velocityY)
        } else {
            direction = Direction(targetValue)
            duration(absolute / height)
        }
        animateTo(
            targetValue = zeroOverscroll,
            animationSpec = tween(durationMillis = duration, easing = SpringEasing),
        )
    }

    private suspend fun performFlingScroll(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity,
    ): Float? {
        queue.clear()
        isFlinging = true
        val new = performFling(velocity)
        isFlinging = false
        if (new.y == 0f) {
            return 0f
        }
        queue.removeLastOrNull()
        if (queue.size > SCRILL_SPEED_BUFFER_MIN) {
            var sum = 0f
            queue.forEachIndexed { index, (_, dy) ->
                if (index != 0) {
                    sum += dy
                }
            }
            val time = queue.last().first - queue.first().first
            if (time > 0) {
                return (sum / time * Duration.Long).coerceIn(-maxVelocity, maxVelocity)
            }
        }
        return null
    }

    private suspend fun flingOverscroll(velocityY: Float): Int {
        startY = when {
            velocityY > 0 -> 0f
            else -> height
        }
        pullY = when {
            velocityY < 0 -> 0f
            else -> height
        }
        val overScale = velocityY / maxVelocity
        val duration = duration(abs(overScale) * 2) // forward + backward
        val targetValue = height * overScale / 3 // divide by magic value
        direction = Direction(targetValue)
        animateTo( // forward
            targetValue = targetValue,
            animationSpec = tween(durationMillis = duration / 10, easing = LinearOutSlowInEasing),
        )
        return duration * 9 / 10 // backward
    }

    private suspend fun snapTo(targetValue: Float) {
        this.targetValue = targetValue
        overscroll.snapTo(targetValue)
    }

    private suspend fun animateTo(targetValue: Float, animationSpec: AnimationSpec<Float>) {
        this.targetValue = targetValue
        overscroll.animateTo( // backward
            targetValue = targetValue,
            animationSpec = animationSpec,
        )
    }

    private fun duration(distanceScale: Float): Int {
        return (HALF_DURATION * distanceScale).toInt()
            .let { min(it, HALF_DURATION) }
            .let { it + HALF_DURATION }
    }
}
