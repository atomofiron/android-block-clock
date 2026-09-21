package app.blockclock.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.runtime.Composable

@Composable
fun WindowInsets.Companion.displayCutout(
    action: WindowInsetsSides.Companion.() -> WindowInsetsSides,
): WindowInsets = WindowInsets.displayCutout.only(WindowInsetsSides.action())

@Composable
fun WindowInsets.Companion.navigationBars(
    action: WindowInsetsSides.Companion.() -> WindowInsetsSides,
): WindowInsets = WindowInsets.navigationBars.only(WindowInsetsSides.action())
