package app.blockclock.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable

@Composable
fun WindowInsets.Companion.displayCutout(
    action: WindowInsetsSides.Companion.() -> WindowInsetsSides,
): WindowInsets = WindowInsets.displayCutout.only(WindowInsetsSides.action())

@Composable
fun WindowInsets.Companion.navigationBars(
    action: WindowInsetsSides.Companion.() -> WindowInsetsSides,
): WindowInsets = WindowInsets.navigationBars.only(WindowInsetsSides.action())

@Composable
fun WindowInsets.Companion.systemBars(
    action: WindowInsetsSides.Companion.() -> WindowInsetsSides,
): WindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.action())

@Composable
fun WindowInsets.Companion.statusBars(
    action: WindowInsetsSides.Companion.() -> WindowInsetsSides,
): WindowInsets = WindowInsets.statusBars.only(WindowInsetsSides.action())

@Composable
fun WindowInsets.Companion.ime(
    action: WindowInsetsSides.Companion.() -> WindowInsetsSides,
): WindowInsets = WindowInsets.ime.only(WindowInsetsSides.action())
