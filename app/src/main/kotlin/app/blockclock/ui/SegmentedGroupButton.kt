package app.blockclock.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SegmentedButton(
    modifier: Modifier = Modifier,
    content: @Composable SingleChoiceSegmentedButtonRowScope.() -> Unit,
) = BoxWithConstraints(modifier = modifier) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .widthIn(min = maxWidth),
        space = (-2).dp,
        content = content,
    )
}

/**
 * One item of a single-choice group, drawn the way Material 3 expressive draws the connected
 * button group: the picked item is filled with the accent color, the rest sit on the surface
 * container, the ends of the group are pills and the corner of the picked item grows to the half
 * of the item while the inner corners of the rest stay small. The values are those of the
 * `ConnectedButtonGroupSmallTokens` of Material 3 1.4, which its own [SegmentedButton] still
 * ignores in favour of the outlined tokens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleChoiceSegmentedButtonRowScope.GroupItem(
    index: Int,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val density = LocalDensity.current
    var full by remember { mutableStateOf(20.dp) }
    SegmentedButton(
        modifier = Modifier.onSizeChanged { full = with(density) { (it.height / 2).toDp() } },
        selected = selected,
        onClick = onClick,
        shape = animatedGroupItemShape(
            index = index,
            count = count,
            selected = selected,
            pressed = pressed,
            full = full,
        ),
        colors = SegmentedButtonDefaults.colors(
            activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = SegmentedButtonDefaults.borderStroke(Color.Transparent),
        interactionSource = interactionSource,
        icon = {},
        label = label,
    )
}

/**
 * The corners of the item at [index] of a group of [count]: the ends of the group are pills of the
 * half of [full], the rest of the corners are [inner] — the half of the item makes it a pill of its
 * own, which is the picked look.
 */
private fun groupItemShape(index: Int, count: Int, full: Dp, inner: Dp): RoundedCornerShape {
    val pill = CornerSize(full)
    val corner = CornerSize(inner)
    val start = if (index == 0) pill else corner
    val end = if (index == count - 1) pill else corner
    return RoundedCornerShape(
        topStart = start,
        topEnd = end,
        bottomEnd = end,
        bottomStart = start,
    )
}

/**
 * The shape of the item on its way to the [pressed] or [selected] look: its inner corners travel
 * there instead of jumping, with the fast spatial spring of the expressive motion scheme — the
 * `MotionScheme` of the 1.4 library is still internal, so the spring is spelled out here. The look
 * of an item differs by that single corner size alone, hence a press that is released half way
 * carries on from where the corners are, the way `AnimatedShapeState` of the connected button
 * group of Material 3 does it.
 */
@Composable
private fun animatedGroupItemShape(
    index: Int,
    count: Int,
    selected: Boolean,
    pressed: Boolean,
    full: Dp,
): Shape {
    val target = when {
        pressed -> 4.dp
        selected -> full
        else -> 8.dp
    }
    val inner by animateDpAsState(
        targetValue = target,
        animationSpec = spring<Dp>(dampingRatio = 0.6f, stiffness = 800f),
        label = "groupItemCorner",
    )
    return remember(index, count, full, inner) { groupItemShape(index, count, full, inner) }
}
