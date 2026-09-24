package app.blockclock.ui.overscroll

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.unit.Constraints
import app.blockclock.ui.overscroll.SpringOverscrollEffect.Direction
import app.blockclock.util.Epsilon
import kotlin.math.absoluteValue

internal class SpringOverscrollItemNode(
    var effect: SpringOverscrollEffect,
) : Modifier.Node(), LayoutModifierNode {

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            val placementScope = this
            placeable.placeWithLayer(0, 0) {
                val child = placementScope.coordinates?.positionInParent() ?: return@placeWithLayer
                val parent = placementScope.coordinates?.parentCoordinates?.size  ?: return@placeWithLayer
                applyEffect(
                    y = child.y,
                    child = placeable.height,
                    parent = parent.height,
                )
            }
        }
    }

    private fun GraphicsLayerScope.applyEffect(
        y: Float,
        child: Int,
        parent: Int,
    ) {
        val overscroll = effect.overscroll.value
        if (overscroll.absoluteValue < Epsilon) {
            return
        }
        var startY = effect.startY
        var pullY = effect.pullY
        val direction = effect.direction
        startY = when (direction) {
            Direction.Down -> startY
            Direction.Up -> parent - startY
        }
        pullY = when (direction) {
            Direction.Down -> pullY
            Direction.Up -> parent - pullY
        }
        val swipe = pullY - startY
        val childEdge = when (direction) {
            Direction.Down -> y + child
            Direction.Up -> parent - y
        }
        val offset = overscroll * direction.sign / 3
        val slowOffset = childEdge / pullY * offset
        translationY = direction.sign * when {
            childEdge <= startY -> slowOffset // items are above/below the finger
            childEdge + offset >= pullY -> offset // items are below/above the finger (in the current iteration)
            else -> { // at first they were below/above, later they became above/below
                val threshold = startY + swipe * (childEdge - startY) / (swipe - offset)
                val fastPart = offset * (threshold - startY) / swipe
                val slowPart = slowOffset * (pullY - threshold) / swipe
                fastPart + slowPart
            }
        }
    }
}
