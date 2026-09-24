package app.blockclock.settings

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import app.blockclock.R
import app.blockclock.ui.BackButton
import app.blockclock.ui.SearchButton
import app.blockclock.ui.SearchField
import app.blockclock.ui.overscroll.Overscroll
import app.blockclock.ui.values.Padding
import app.blockclock.util.displayCutout
import app.blockclock.util.navigationBars
import app.blockclock.util.onClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> PickerScreen(
    title: String,
    cellMinWidth: Dp,
    provider: (Context) -> List<T>,
    names: T.() -> String,
    keys: T.(Int) -> Any,
    showSearch: Boolean = false,
    showDefault: Boolean = false,
    onPick: (T?) -> Unit,
    onClose: () -> Unit,
    defaultItemContent: (@Composable (Modifier, TextStyle) -> Unit)? = null,
    itemContent: @Composable (Modifier, TextStyle, T) -> Unit,
) {
    var items by remember { mutableStateOf<List<T>?>(null) }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        items = withContext(Dispatchers.Default) {
            provider(context)
        }
    }
    var searching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val visibleItems = remember(items, searchQuery) {
        val list = items.orEmpty()
        when {
            searchQuery.isEmpty() -> list
            else -> list.filter { names(it).contains(searchQuery, ignoreCase = true) }
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
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
                    if (showSearch) SearchButton(
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
            if (items == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            AnimatedVisibility(
                visible = items != null,
                enter = fadeIn(),
            ) {
                val textStyle = MaterialTheme.typography.bodyLarge
                Overscroll(rememberCoroutineScope()) { columnModifier, itemModifier ->
                    LazyVerticalGrid(
                        modifier = columnModifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        columns = GridCells.Adaptive(cellMinWidth),
                        horizontalArrangement = Arrangement.spacedBy(Padding.Common),
                        contentPadding = WindowInsets.navigationBars { Bottom }
                            .add(WindowInsets.displayCutout { Start + End })
                            .add(
                                WindowInsets(
                                    left = Padding.Common,
                                    right = Padding.Common,
                                    bottom = Padding.Common
                                )
                            )
                            .asPaddingValues(),
                    ) {
                        if (showDefault) item {
                            defaultItemContent?.invoke(itemModifier.onClick { onPick(null) }, textStyle)
                                ?: Text(
                                    modifier = itemModifier.onClick { onPick(null) },
                                    text = stringResource(R.string.font_default),
                                    style = textStyle,
                                )
                        }
                        itemsIndexed(
                            visibleItems,
                            key = { index, it -> keys(it, index) }) { _, it ->
                            itemContent(itemModifier.onClick { onPick(it) }, textStyle, it)
                        }
                    }
                }
            }
        }
    }
}
