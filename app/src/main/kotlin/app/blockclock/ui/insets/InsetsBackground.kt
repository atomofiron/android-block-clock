package app.blockclock.ui.insets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import app.blockclock.ui.LocalScreenCorners
import app.blockclock.ui.values.Dimens
import app.blockclock.util.Empty
import app.blockclock.util.calcSize
import app.blockclock.util.invoke
import app.blockclock.util.plus
import kotlin.math.max

@Composable
fun InsetsBackground(
    modifier: Modifier = Modifier,
    statusBar: Boolean = true,
    navigationStart: Boolean = true,
    navigationEnd: Boolean = true,
    navigationBottom: Boolean = true,
    transparent: WindowInsets = WindowInsets.Empty,
) {
    when {
        statusBar -> Unit
        navigationStart -> Unit
        navigationEnd -> Unit
        navigationBottom -> Unit
        else -> return
    }

    val direction = LocalLayoutDirection.current
    val drawStatusBar = statusBar
    val (navigationLeft, navigationRight) = when (direction) {
        LayoutDirection.Ltr -> (navigationStart to navigationEnd)
        LayoutDirection.Rtl -> (navigationEnd to navigationStart)
    }
    val color = MaterialTheme.colorScheme.background
    val screenCorners = LocalScreenCorners.current

    val navigationBar = WindowInsets.navigationBars.get()
    val cutout = WindowInsets.displayCutout.get()
    val statusBar = WindowInsets.statusBars.get()
    val common = WindowInsets { statusBars + navigationBars }.get()
    val tappable = WindowInsets.tappableElement.get()
    val transparent = transparent.get()
    val leftInset = resolve(common.left, navigationBar.left, tappable.left, transparent.left)
    val rightInset = resolve(common.right, navigationBar.right, tappable.right, transparent.right)
    val bottomInset = resolve(common.bottom, navigationBar.bottom, tappable.bottom, transparent.bottom)

    if (drawStatusBar) Canvas(modifier.fillMaxSize()) {

        val leftInset = leftInset.toFloat()
        val rightInset = rightInset.toFloat()
        val bottomInset = bottomInset.toFloat()

        val width = size.width
        val height = size.height

        val navigationHorizontalTop = drawStatusBar(statusBar, cutout, navigationBar, screenCorners, color, size).toFloat()
        if (navigationLeft) {
            val offset = Offset(0f, navigationHorizontalTop)
            drawRect(color, offset, offset.calcSize(leftInset, height - bottomInset))
        }
        if (navigationRight) {
            val offset = Offset(width - rightInset, navigationHorizontalTop)
            drawRect(color, offset, offset.calcSize(width, height - bottomInset))
        }
        if (navigationBottom) {
            val offset = Offset(0f, height - bottomInset)
            drawRect(color, offset, offset.calcSize(width, height))
        }
    }
}

private fun DrawScope.drawStatusBar(
    statusBar: Insets,
    cutout: Insets,
    navigationBar: Insets,
    screenCorners: ScreenCorners,
    color: Color,
    size: Size,
): Int {
    calcStatusBarPadding(
        statusBar.top,
        maxStatusBar = Dimens.maxStatusBar.roundToPx(),
        statusBarMinPadding = Dimens.statusBarMinPadding.roundToPx(),
        cutout,
        navigationBar,
        screenCorners,
    ) { left, vertical, right ->
        val bottom = statusBar.top - vertical
        val radius = statusBar.top / 2f - vertical
        val offset = Offset(left.toFloat(), vertical.toFloat())
        drawRoundRect(color, offset, offset.calcSize(size.width - right.toFloat(), bottom.toFloat()), CornerRadius(radius))
        return 0
    }
    drawRect(color, Offset.Zero, Size(size.width, statusBar.top.toFloat()))
    return statusBar.top
}

private inline fun calcStatusBarPadding(
    statusBar: Int,
    maxStatusBar: Int,
    statusBarMinPadding: Int,
    cutout: Insets,
    navigationBar: Insets,
    screenCorners: ScreenCorners,
    action: (left: Int, vertical: Int, right: Int) -> Unit,
): Boolean {
    val padding = (statusBar - maxStatusBar) / 2
    return if (padding <= statusBarMinPadding) { // it's important
        false
    } else {
        val cutout = max(cutout.left, cutout.right)
        val marginLeft = max(screenCorners.topLeft * 0.6f - cutout, 0f).toInt()
        val marginRight = max(screenCorners.topRight * 0.6f - cutout, 0f).toInt()
        val rawLeft = max(navigationBar.left, cutout)
        val rawRight = max(navigationBar.right, cutout)
        val left = rawLeft + max(marginLeft, padding)
        val right = rawRight + max(marginRight, padding)
        action(left, padding, right)
        true
    }
}

@Composable
private fun WindowInsets.get(): Insets {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    return Insets(
        getLeft(density, direction),
        getTop(density),
        getRight(density, direction),
        getBottom(density),
    )
}

private fun resolve(
    target: Int,
    navigation: Int,
    tappable: Int,
    transparent: Int,
): Int = when {
    transparent > target -> 0 // something is greater than target
    target != navigation -> target
    target == tappable -> target
    else -> 0 // gesture navigation bar
}