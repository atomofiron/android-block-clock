package app.blockclock.licenses

import android.content.Intent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import app.blockclock.R
import app.blockclock.model.License
import app.blockclock.settings.PickerScreen
import app.blockclock.ui.values.Dimens

/**
 * A full-screen OSS licenses list: a tap on a text license opens its
 * content in a dialog, a tap on a link opens the browser.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<License.Text?>(null) }
    PickerScreen(
        title = stringResource(R.string.licenses),
        cellMinWidth = Dimens.WidePickerColumnMinWidth,
        provider = { LicensesParser.readLicenses(context.assets) },
        names = License::name,
        keys = { it },
        onPick = {
            when (it) {
                is License.Text -> selected = it
                is License.Url -> context.startActivity(Intent(Intent.ACTION_VIEW, it.url.toUri()))
                null -> Unit
            }
        },
        onClose = onClose,
    ) { modifier, item ->
        Text(
            modifier = modifier,
            text = item.name,
            style = MaterialTheme.typography.titleMedium,
        )
    }
    selected?.let { license ->
        LicenseDialog(license = license, onDismiss = { selected = null })
    }
}
