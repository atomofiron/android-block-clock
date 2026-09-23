package app.blockclock.settings

import android.os.Build.VERSION_CODES.Q
import androidx.annotation.RequiresApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import app.blockclock.R
import app.blockclock.model.WidgetFont
import app.blockclock.ui.values.Dimens
import app.blockclock.util.getSystemFonts
import app.blockclock.util.toFontFamily

/**
 * A full-screen picker of the system fonts: each row shows the font file
 * name rendered in that font; a tap passes the picked [WidgetFont] up,
 * null means the system default font.
 */
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Q)
@Composable
fun FontPickerScreen(
    onPick: (WidgetFont?) -> Unit,
    onClose: () -> Unit,
) = PickerScreen(
    title = stringResource(R.string.font),
    cellMinWidth = Dimens.WidePickerColumnMinWidth,
    provider = { getSystemFonts().sortedBy { it.name } },
    names = { name },
    keys = { path },
    showSearch = true,
    showDefault = true,
    onPick = onPick,
    onClose = onClose,
) { modifier, style, item ->
    Text(
        modifier = modifier,
        text = item.name,
        fontFamily = item.toFontFamily(),
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
