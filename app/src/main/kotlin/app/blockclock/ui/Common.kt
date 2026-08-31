package app.blockclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ShapeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import app.blockclock.R
import app.blockclock.ui.values.Colors
import app.blockclock.ui.values.Dimens
import app.blockclock.util.ifNotNull

@Composable
fun BackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back),
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
