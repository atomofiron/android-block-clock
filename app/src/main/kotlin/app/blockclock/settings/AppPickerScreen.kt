package app.blockclock.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import app.blockclock.R
import app.blockclock.model.TargetApp
import app.blockclock.model.UserApp
import app.blockclock.ui.BackButton
import app.blockclock.ui.values.Dimens
import app.blockclock.ui.values.Padding
import app.blockclock.util.appIcon
import app.blockclock.util.toPainter
import app.blockclock.widget.getInstalledApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A full-screen picker of all launcher apps: each row shows the app icon
 * and label; a tap passes the picked [TargetApp] up.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
    title: String,
    onPick: (TargetApp) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    var apps by remember { mutableStateOf<List<UserApp>?>(null) }
    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.Default) {
            getInstalledApps(context)
                .map { UserApp(it.loadLabel(packageManager).toString(), it, context.appIcon(it.activityInfo.packageName)) }
                .sortedBy { it.label.lowercase() }
        }
    }
    var searching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val visibleApps = remember(apps, searchQuery) {
        val list = apps.orEmpty()
        when {
            searchQuery.isEmpty() -> list
            else -> list.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
    }
    LaunchedEffect(searching) {
        if (searching) focusRequester.requestFocus()
    }
    BackHandler(onBack = {
        if (searching) {
            searching = false
            searchQuery = ""
        } else {
            onClose()
        }
    })
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
                title = {
                    when {
                        searching -> SearchField(
                            Modifier.fillMaxWidth(),
                            searchQuery,
                            focusRequester,
                            onInput = { searchQuery = it },
                        )
                        else -> Text(text = title)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        when {
                            searchQuery.isEmpty() -> searching = !searching
                            else -> searchQuery = ""
                        }
                    }) {
                        Icon(
                            imageVector = if (searching) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = stringResource(
                                if (searching) R.string.clear_search else R.string.search,
                            ),
                        )
                    }
                },
            )
            if (apps == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            AnimatedVisibility(
                visible = apps != null,
                enter = fadeIn(),
            ) {
                AppList(visibleApps, onPick)
            }
        }
    }
}

@Composable
private fun AppList(
    apps: List<UserApp>,
    onPick: (TargetApp) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(Dimens.PickerColumnMinWidth),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(Padding.Common),
        contentPadding = WindowInsets.navigationBars
            .only(WindowInsetsSides.Bottom)
            .add(WindowInsets(left = Padding.Common, right = Padding.Common, bottom = Padding.Common))
            .asPaddingValues(),
    ) {
        items(apps, key = UserApp::key) { app ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ShapeDefaults.Medium)
                    .clickable { onPick(app.toTarget()) }
                    .padding(vertical = Padding.Half),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    modifier = Modifier.size(Dimens.LargeIconSize),
                    painter = remember(app.packageName) { app.drawable.toPainter() },
                    contentDescription = null,
                )
                Text(
                    modifier = Modifier
                        .padding(start = Padding.Semi)
                        .fillMaxWidth(),
                    text = app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SearchField(
    modifier: Modifier,
    searchQuery: String,
    focusRequester: FocusRequester,
    onInput: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = modifier.focusRequester(focusRequester),
        value = searchQuery,
        onValueChange = onInput,
        placeholder = { Text(stringResource(R.string.search)) },
        singleLine = true,
        shape = ShapeDefaults.Medium,
    )
}
