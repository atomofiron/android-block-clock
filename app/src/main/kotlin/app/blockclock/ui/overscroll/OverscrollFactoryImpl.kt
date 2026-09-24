package app.blockclock.ui.overscroll

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory

internal class OverscrollFactoryImpl(private val effect: OverscrollEffect) : OverscrollFactory {

    override fun createOverscrollEffect() = effect

    override fun hashCode(): Int = effect.hashCode()

    override fun equals(other: Any?) = when (other) {
        !is OverscrollFactoryImpl -> false
        else -> other.equals(effect)
    }

    fun equals(effect: OverscrollEffect) = effect === this.effect
}
