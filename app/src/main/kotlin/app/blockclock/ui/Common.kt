package app.blockclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.blockclock.R
import app.blockclock.ui.values.Colors
import app.blockclock.ui.values.Dimens
import app.blockclock.util.ifNotNull

@Composable
fun BackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_back),
            contentDescription = stringResource(R.string.back),
        )
    }
}

@Composable
fun SearchButton(
    searching: Boolean,
    searchQuery: String,
    onToggle: () -> Unit,
    onReset: () -> Unit,
) {
    IconButton(onClick = {
        when {
            searchQuery.isEmpty() -> onToggle()
            else -> onReset()
        }
    }) {
        Icon(
            painter = painterResource(if (searching) R.drawable.ic_cross else R.drawable.ic_search),
            contentDescription = stringResource(
                if (searching) R.string.clear_search else R.string.search,
            ),
        )
    }
}

@Composable
fun ColorBox(
    modifier: Modifier = Modifier,
    color: Color,
    onClick: (() -> Unit)? = null,
) = Box(
    modifier = modifier
        .height(Dimens.SwatchSize)
        .clip(ShapeDefaults.Medium)
        .background(color)
        .border(Dimens.SwatchBorderWidth, Colors.SwatchBorder, ShapeDefaults.Medium)
        .ifNotNull(onClick) { clickable(onClick = it) },
)

/** The search field shown in the toolbar of the picker screens. */
@Composable
fun SearchField(
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
