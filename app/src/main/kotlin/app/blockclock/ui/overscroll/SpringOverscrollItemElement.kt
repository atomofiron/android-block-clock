package app.blockclock.ui.overscroll

import android.annotation.SuppressLint
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement

internal fun Modifier.overscrollItem(effect: SpringOverscrollEffect) = then(OverscrollItemElement(effect))

@SuppressLint("ModifierNodeInspectableProperties")
private data class OverscrollItemElement(
    private val effect: SpringOverscrollEffect,
) : ModifierNodeElement<SpringOverscrollItemNode>() {

    override fun create() = SpringOverscrollItemNode(effect)

    override fun update(node: SpringOverscrollItemNode) { node.effect = effect }
}
