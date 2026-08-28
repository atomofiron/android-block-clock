package app.blockclock.licenses

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.blockclock.R
import app.blockclock.ui.BackButton
import app.blockclock.ui.values.Padding

/**
 * A full-screen OSS licenses list: a tap on a text license opens its
 * content in a dialog, a tap on a link opens the browser.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val licenses = remember { LicensesParser.readLicenses(context.assets) }
    var selected by remember { mutableStateOf<License.Text?>(null) }
    BackHandler(onBack = onClose)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    BackButton(onClose)
                },
                title = { Text(stringResource(R.string.licenses)) },
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = WindowInsets.navigationBars
                    .only(WindowInsetsSides.Bottom)
                    .add(WindowInsets(left = Padding.Common, right = Padding.Common, bottom = Padding.Common))
                    .asPaddingValues(),
            ) {
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
        }
    }
    selected?.let { license ->
        LicenseDialog(license = license, onDismiss = { selected = null })
    }
}
