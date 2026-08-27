package app.blockclock.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.blockclock.R
import app.blockclock.ui.values.Colors
import app.blockclock.ui.values.Dimens
import app.blockclock.ui.values.Padding
import kotlin.math.roundToInt

private const val HueDegrees = 360f

/**
 * The color picker dialog: a saturation × value (SV) gradient field at the
 * selected hue (H) + a hue slider + the current color and its HEX.
 */
@Composable
internal fun ColorPickerDialog(
    title: String,
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
) {
    val initialHsv = remember(initialColor) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor.toArgb(), it) }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    val color = remember(hue, sat, value) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Padding.Semi)) {
                SaturationValueBox(
                    hue = hue,
                    sat = sat,
                    value = value,
                    onChange = { s, v ->
                        sat = s
                        value = v
                    },
                )
                HueSlider(hue = hue, onChange = { hue = it })
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.DialogSwatchSize)
                            .clip(ShapeDefaults.Medium)
                            .background(color)
                            .border(Dimens.SwatchBorderWidth, Colors.SwatchBorder, ShapeDefaults.Medium),
                    )
                    Text(
                        modifier = Modifier.padding(start = Padding.Semi),
                        text = "#%06X".format(color.toArgb() and 0xFFFFFF),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(color) }) {
                Text(stringResource(R.string.button_done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        },
    )
}

/** The saturation (horizontal) × value (vertical) square. */
@Composable
private fun SaturationValueBox(
    hue: Float,
    sat: Float,
    value: Float,
    onChange: (sat: Float, value: Float) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.SvBoxHeight)
            .clip(ShapeDefaults.Medium)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.White,
                        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                    )
                )
            )
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            .pointerInput(Unit) {
                detectTapGestures { position ->
                    val (s, v) = satValueOf(size, position)
                    onChange(s, v)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { position ->
                        val (s, v) = satValueOf(size, position)
                        onChange(s, v)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val (s, v) = satValueOf(size, change.position)
                        onChange(s, v)
                    },
                )
            },
    ) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (sat * maxWidth.value * density - Dimens.MarkerRadius.value * density).roundToInt(),
                        y = ((1f - value) * maxHeight.value * density - Dimens.MarkerRadius.value * density).roundToInt(),
                    )
                }
                .size(Dimens.MarkerSize)
                .border(Dimens.MarkerBorderWidth, Colors.MarkerBorder, CircleShape),
        )
    }
}

private fun satValueOf(size: IntSize, position: Offset): Pair<Float, Float> = Pair(
    (position.x / size.width).coerceIn(0f, 1f),
    1f - (position.y / size.height).coerceIn(0f, 1f),
)

private fun hueOf(size: IntSize, position: Offset): Float =
    (position.x / size.width * HueDegrees).coerceIn(0f, HueDegrees)

@Composable
private fun HueSlider(hue: Float, onChange: (Float) -> Unit) {
    val rainbow = Color.run { listOf(Red, Yellow, Green, Cyan, Blue, Magenta, Red) }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.HueSliderHeight)
            .clip(ShapeDefaults.Medium)
            .background(Brush.horizontalGradient(rainbow))
            .pointerInput(Unit) {
                detectTapGestures { position -> onChange(hueOf(size, position)) }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { position -> onChange(hueOf(size, position)) },
                    onDrag = { change, _ ->
                        change.consume()
                        onChange(hueOf(size, change.position))
                    },
                )
            },
    ) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (hue / HueDegrees * maxWidth.value * density - Dimens.MarkerRadius.value * density).roundToInt(),
                        y = (maxHeight.value * density / 2 - Dimens.MarkerRadius.value * density).roundToInt(),
                    )
                }
                .size(Dimens.MarkerSize)
                .clip(CircleShape)
                .border(Dimens.MarkerBorderWidth, Color.White, CircleShape),
        )
    }
}
