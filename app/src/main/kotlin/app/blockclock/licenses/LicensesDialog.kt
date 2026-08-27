package app.blockclock.licenses

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.blockclock.R
import app.blockclock.ui.Padding

/**
 * Диалог OSS-лицензий: список библиотек. Тап по лицензии с текстом
 * показывает её содержимое, тап по ссылке открывает браузер.
 */
@Composable
fun LicensesDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val licenses = remember { LicensesParser.readLicenses(context.assets) }
    var selected by remember { mutableStateOf<License.Text?>(null) }

    AlertDialog(
        onDismissRequest = {
            if (selected == null) onDismiss() else selected = null
        },
        title = { Text(selected?.name ?: stringResource(R.string.licenses)) },
        text = {
            val current = selected
            if (current == null) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(licenses, key = { i, _ -> i }) { _, license ->
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (license) {
                                        is License.Text -> selected = license
                                        is License.Url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(license.url)))
                                    }
                                }
                                .padding(vertical = Padding.Half),
                            text = license.name,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        modifier = Modifier.padding(top = Padding.Half),
                        text = current.text,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when (selected) {
                    null -> onDismiss()
                    else -> selected = null
                }
            }) {
                Text(
                    stringResource(
                        when (selected) {
                            null -> R.string.button_close
                            else -> R.string.button_ok
                        },
                    ),
                )
            }
        },
    )
}
