package app.blockclock.settings

import android.graphics.Color.HSVToColor
import android.graphics.Color.colorToHSV
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.blockclock.R
import app.blockclock.model.ColorSource
import app.blockclock.model.WallpaperColors
import app.blockclock.ui.ColorBox
import app.blockclock.ui.values.Colors
import app.blockclock.ui.values.Dimens
import app.blockclock.ui.values.Padding
import kotlin.math.roundToInt

private const val HueDegrees = 360f

/** The digits of the hex color: the RGB, the alpha of the widget is its transparency. */
private const val HexColorLength = 6

/** A hex digit carries a nibble. */
private const val HexDigitBits = 4

private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

/** The decoration of a pasted color: the `#` and the spaces around it. */
private fun Char.isHexDecoration(): Boolean = this == '#' || isWhitespace()

private fun hsvColor(hue: Float, sat: Float, value: Float): Color =
    Color(HSVToColor(floatArrayOf(hue, sat, value)))

/**
 * The input filter of the hex field: a pasted color comes with the `#` and the spaces around it,
 * which are dropped, and the input that has anything else in it is rejected as a whole, so that a
 * wrong character cannot even overwrite what is already typed. A rejected change is reverted along
 * with the selection, so the caret stays where it was. The transformation is applied to the user
 * input only, so [onDigits] is the typing and never the field rewritten by the picker.
 */
private class HexColorInputTransformation(
    private val onDigits: (String) -> Unit,
) : InputTransformation {

    @OptIn(ExperimentalFoundationApi::class)
    override fun TextFieldBuffer.transformInput() {
        var rejected = false
        for (index in changes.changeCount - 1 downTo 0) {
            val range = changes.getRange(index)
            if (range.collapsed) continue
            val input = asCharSequence().subSequence(range.min, range.max).toString()
            val digits = input.filter(Char::isHexDigit).take(HexColorLength)
            when {
                digits.isEmpty() -> rejected = true
                input.any { !it.isHexDigit() && !it.isHexDecoration() } -> rejected = true
                digits != input -> replace(range.min, range.max, digits)
            }
        }
        if (rejected || length > HexColorLength) {
            revertAllChanges()
        } else {
            onDigits(asCharSequence().toString())
        }
    }
}

/**
 * The color of the [digits]: a single digit is a nibble, so they fill the RGB
 * from the left and any amount of them is already a color, which lets the color
 * follow the typing. Null for no digits at all.
 */
private fun hexColor(digits: String): Color? = digits.toLongOrNull(16)?.let { value ->
    val shift = (HexColorLength - digits.length) * HexDigitBits
    Color(0xFF000000.toInt() or (value shl shift).toInt())
}

/** The hex digits of the [color], e.g. `3A7BD5`. */
private fun hexDigits(color: Color): String = "%06X".format(color.toArgb() and 0xFFFFFF)

/**
 * The color picker dialog: a saturation × value (SV) gradient field at the
 * selected hue (H) + a hue slider + the current color and its HEX, which is
 * a field to paste or type a color into, with or without the leading `#`.
 */
@Composable
internal fun ColorPickerDialog(
    title: String,
    initialColor: Color,
    wallpaper: WallpaperColors?,
    onDismiss: () -> Unit,
    onConfirm: (Color, ColorSource?) -> Unit,
) {
    var source: ColorSource? by remember { mutableStateOf(null) }
    val hsv = remember {
        FloatArray(3).also { colorToHSV(initialColor.toArgb(), it) }
    }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }
    val hex = rememberTextFieldState(hexDigits(initialColor))
    val color = remember(hue, sat, value) { hsvColor(hue, sat, value) }
    fun set(color: Color) {
        colorToHSV(color.toArgb(), hsv)
        hue = hsv[0]
        sat = hsv[1]
        value = hsv[2]
    }

    /** The picker has been used: the color follows and the field comes back to its digits. */
    fun pick(color: Color) {
        set(color)
        hex.setTextAndPlaceCursorAtEnd(hexDigits(color))
    }
    // The typed digits own the color: every one of them is already a color.
    val inputTransformation = remember {
        HexColorInputTransformation { entered ->
            hexColor(entered)?.let { typed ->
                source = ColorSource.Manual
                set(typed)
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Padding.Common)) {
                if (wallpaper != null) Row(horizontalArrangement = Arrangement.spacedBy(Padding.Common)) {
                    wallpaper.primary.let { color ->
                        ColorBox(
                            modifier = Modifier
                            .height(Dimens.WallpaperColorHeight)
                            .weight(1f),
                            color,
                        ) {
                            source = ColorSource.Primary
                            pick(color)
                        }
                    }
                    wallpaper.secondary?.let { color ->
                        ColorBox(
                            modifier = Modifier
                                .height(Dimens.WallpaperColorHeight)
                                .weight(1f),
                            color,
                        ) {
                            source = ColorSource.Secondary
                            pick(color)
                        }
                    }
                    wallpaper.tertiary?.let { color ->
                        ColorBox(
                            modifier = Modifier
                                .height(Dimens.WallpaperColorHeight)
                                .weight(1f),
                            color,
                        ) {
                            source = ColorSource.Tertiary
                            pick(color)
                        }
                    }
                }
                SaturationValueBox(
                    hue = hue,
                    sat = sat,
                    value = value,
                    onChange = { s, v ->
                        source = ColorSource.Manual
                        pick(hsvColor(hue, s, v))
                    },
                )
                HueSlider(hue = hue) {
                    source = ColorSource.Manual
                    pick(hsvColor(it, sat, value))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ColorBox(Modifier.size(Dimens.SwatchSize), color)
                    HexColorField(
                        modifier = Modifier
                            .padding(start = Padding.Common)
                            .weight(1f),
                        state = hex,
                        inputTransformation = inputTransformation,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(color, source) }) {
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

/** The hex color field: the `#` is its prefix decoration, the value is the digits alone. */
@Composable
private fun HexColorField(
    modifier: Modifier = Modifier,
    state: TextFieldState,
    inputTransformation: InputTransformation,
) {
    OutlinedTextField(
        modifier = modifier,
        state = state,
        prefix = { Text("#") },
        textStyle = MaterialTheme.typography.bodyLarge,
        inputTransformation = inputTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        lineLimits = TextFieldLineLimits.SingleLine,
        shape = ShapeDefaults.Medium,
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
                        Color(HSVToColor(floatArrayOf(hue, 1f, 1f)))
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
