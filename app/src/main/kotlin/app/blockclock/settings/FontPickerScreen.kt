package app.blockclock.settings

import android.os.Build.VERSION_CODES.Q
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import app.blockclock.R
import app.blockclock.model.WidgetFont
import app.blockclock.ui.BackButton
import app.blockclock.ui.SearchButton
import app.blockclock.ui.SearchField
import app.blockclock.ui.values.Dimens
import app.blockclock.ui.values.Padding
import app.blockclock.util.getSystemFonts
import app.blockclock.util.toFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A full-screen picker of the system fonts: each row shows the font file
 * name rendered in that font; a tap passes the picked [WidgetFont] up,
 * null means the system default font.
 */
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Q)
@Composable
fun FontPickerScreen(
    title: String,
    onPick: (WidgetFont?) -> Unit,
    onClose: () -> Unit,
) {
    var fonts by remember { mutableStateOf<List<WidgetFont>?>(null) }
    LaunchedEffect(Unit) {
        fonts = withContext(Dispatchers.Default) {
            getSystemFonts().sortedBy { it.name }
        }
    }
    var searching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val visibleFonts = remember(fonts, searchQuery) {
        val list = fonts.orEmpty()
        when {
            searchQuery.isEmpty() -> list
            else -> list.filter { it.name.contains(searchQuery, ignoreCase = true) }
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
                    SearchButton(
                        searching,
                        searchQuery,
                        onToggle = {
                            searching = !searching
                            searchQuery = ""
                        },
                        onReset = { searchQuery = "" },
                    )
                },
            )
            if (fonts == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            AnimatedVisibility(
                visible = fonts != null,
                enter = fadeIn(),
            ) {
                FontList(showDefault = searchQuery == "", visibleFonts, onPick)
            }
        }
    }
}

@RequiresApi(Q)
@Composable
private fun FontList(
    showDefault: Boolean,
    fonts: List<WidgetFont>,
    onPick: (WidgetFont?) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(Dimens.WidePickerColumnMinWidth),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(Padding.Common),
        contentPadding = WindowInsets.navigationBars
            .only(WindowInsetsSides.Bottom)
            .add(WindowInsets(left = Padding.Common, right = Padding.Common, bottom = Padding.Common))
            .asPaddingValues(),
    ) {
        if (showDefault) item {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ShapeDefaults.Medium)
                    .clickable { onPick(null) }
                    .padding(vertical = Padding.Common),
                text = stringResource(R.string.font_default),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        items(fonts, key = { it.path }) { font ->
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ShapeDefaults.Medium)
                    .clickable { onPick(font) }
                    .padding(vertical = Padding.Common),
                text = font.name,
                fontFamily = font.toFontFamily(),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
