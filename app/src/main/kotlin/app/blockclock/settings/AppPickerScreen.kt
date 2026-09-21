package app.blockclock.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import app.blockclock.R
import app.blockclock.model.AppPickerTarget
import app.blockclock.model.TargetApp
import app.blockclock.model.UserApp
import app.blockclock.ui.values.Dimens
import app.blockclock.ui.values.Padding
import app.blockclock.util.appIcon
import app.blockclock.util.toPainter
import app.blockclock.widget.getInstalledApps

/**
 * A full-screen picker of all launcher apps: each row shows the app icon
 * and label; a tap passes the picked [TargetApp] up.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
    target: AppPickerTarget,
    onPick: (TargetApp?) -> Unit,
    onClose: () -> Unit,
) = PickerScreen(
    title = when (target) {
        AppPickerTarget.Clock -> R.string.clock_app
        AppPickerTarget.Calendar -> R.string.calendar_app
    }.let { stringResource(it) },
    cellMinWidth = Dimens.PickerColumnMinWidth,
    provider = { context ->
        getInstalledApps(context)
            .map { UserApp(it.loadLabel(context.packageManager).toString(), it, context.appIcon(it.activityInfo.packageName)) }
            .sortedBy { it.label }
    },
    names = { label },
    keys = { key },
    showSearch = true,
    onPick = { onPick(it?.toTarget()) },
    onClose = onClose,
) { modifier, item ->
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            modifier = Modifier.size(Dimens.LargeIconSize),
            painter = remember(item.packageName) { item.drawable.toPainter() },
            contentDescription = null,
        )
        Text(
            modifier = Modifier
                .padding(start = Padding.Semi)
                .fillMaxWidth(),
            text = item.label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
