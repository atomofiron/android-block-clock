package app.blockclock.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.min

data class Gap(
    val left: Boolean = false,
    val top: Boolean = false,
    val right: Boolean = false,
    val bottom: Boolean = false,
)

/**
 * A widget part: the [weight] (cell multipliers), the [gap] flags,
 * extra inner gaps [gapInside] and the time flag.
 */
data class Part(
    val weight: Weight,
    val gap: Gap,
    val gapInside: Int = 0,
    val time: Boolean = false,
)

/** The structure proportions and the number of gap slots along one axis. */
data class Dimension(
    val weight: Int,
    val gaps: Int,
)

data class Weight(
    val horizontal: Int,
    val vertical: Int,
)

sealed interface Structure {

    val horizontal: Dimension
    val vertical: Dimension

    sealed interface Time {
        val hours: Part
        val minutes: Part
    }

    sealed interface Weekday {
        val weekday: Part
    }

    sealed interface Date {
        val first: Part
        val second: Part
        val year: Part
    }

    sealed interface TimeAndDate : Time, Weekday, Date

    data object OneLevel : TimeAndDate, Structure {
        override val hours: Part = Part(Weight(2, 2), Gap(right = true), gapInside = 1, time = true)
        override val minutes: Part = Part(Weight(2, 2), Gap(left = true, right = true), gapInside = 1, time = true)
        override val weekday: Part = Part(Weight(4, 1), Gap(left = true, bottom = true), gapInside = 3)
        override val first: Part = Part(Weight(1, 1), Gap(left = true, top = true, right = true))
        override val second: Part = Part(Weight(1, 1), Gap(left = true, top = true, right = true))
        override val year: Part = Part(Weight(2, 1), Gap(left = true, top = true), gapInside = 1)

        override val horizontal = Dimension(8, 7)
        override val vertical = Dimension(2, 1)
    }

    data object TwoLevel : TimeAndDate, Structure {
        override val hours: Part = Part(Weight(4, 4), Gap(right = true, bottom = true), gapInside = 1, time = true)
        override val minutes: Part = Part(Weight(4, 4), Gap(left = true, bottom = true), gapInside = 1, time = true)
        override val weekday: Part = Part(Weight(4, 1), Gap(top = true, right = true), gapInside = 1)
        override val first: Part = Part(Weight(1, 1), Gap(left = true, top = true, right = true))
        override val second: Part = Part(Weight(1, 1), Gap(left = true, top = true, right = true))
        override val year: Part = Part(Weight(2, 1), Gap(left = true, top = true), gapInside = -1)

        override val horizontal = Dimension(8, 3)
        override val vertical = Dimension(5, 2)
    }

    data object ThreeLevel : TimeAndDate, Structure {
        override val hours: Part = Part(Weight(2, 2), Gap(right = true, bottom = true), gapInside = 1, time = true)
        override val minutes: Part = Part(Weight(2, 2), Gap(left = true, bottom = true), gapInside = 1, time = true)
        override val weekday: Part = Part(Weight(4, 1), Gap(top = true, bottom = true), gapInside = 3)
        override val first: Part = Part(Weight(1, 1), Gap(top = true, right = true))
        override val second: Part = Part(Weight(1, 1), Gap(left = true, top = true, right = true))
        override val year: Part = Part(Weight(2, 1), Gap(left = true, top = true), gapInside = 1)

        override val horizontal = Dimension(4, 3)
        override val vertical = Dimension(4, 3)
    }

    data object TimeOnly : Time, Structure {
        override val hours: Part = Part(Weight(1, 1), Gap(right = true), time = true)
        override val minutes: Part = Part(Weight(1, 1), Gap(left = true), time = true)

        override val horizontal = Dimension(2, 1)
        override val vertical = Dimension(1, 0)
    }

    data object DateOnly : Date, Weekday, Structure {
        override val weekday: Part = Part(Weight(4, 1), Gap(bottom = true), gapInside = 3)
        override val first: Part = Part(Weight(1, 1), Gap(top = true, right = true))
        override val second: Part = Part(Weight(1, 1), Gap(left = true, top = true, right = true))
        override val year: Part = Part(Weight(2, 1), Gap(left = true, top = true), gapInside = 1)

        override val horizontal = Dimension(4, 3)
        override val vertical = Dimension(2, 1)
    }
}

/**
 * Resolves the structure sizes in the available space: [Dimension.gaps] ×
 * [gap] are subtracted from the available size, the remainder is divided
 * by the [Dimension.weight] (horizontal to vertical) — the cell; the gaps
 * are added back to the grid.
 *
 * @return a pair: the cell size (one weight unit) and the grid size (container).
 */
fun Structure.resolve(available: DpSize, gap: Dp): Pair<DpSize, DpSize> {
    val gapWidth = gap * horizontal.gaps
    val gapHeight = gap * vertical.gaps
    val min = min(
        (available.width - gapWidth).value / horizontal.weight.toFloat(),
        (available.height - gapHeight).value / vertical.weight.toFloat(),
    ).coerceAtLeast(0f)
    val cell = DpSize(min.dp, min.dp)
    return cell to DpSize(
        width = (horizontal.weight * min).dp + gapWidth,
        height = (vertical.weight * min).dp + gapHeight,
    )
}

/**
 * The part size: the [cellSize] cell times the [Weight] plus the gaps —
 * [gap]/2 on each flagged side, and parts wider than one cell get
 * [gapInside] × [gap] more.
 */
fun Part.calcSize(cellSize: DpSize, gap: Dp): DpSize {
    var hGap = (if (this.gap.left) gap / 2 else 0.dp) + (if (this.gap.right) gap / 2 else 0.dp)
    var vGap = (if (this.gap.top) gap / 2 else 0.dp) + (if (this.gap.bottom) gap / 2 else 0.dp)
    if (weight.horizontal > 1) {
        hGap += gap * gapInside
    }
    if (weight.vertical > 1) {
        vGap += gap * gapInside
    }
    return DpSize(
        width = cellSize.width * weight.horizontal + hGap,
        height = cellSize.height * weight.vertical + vGap,
    )
}