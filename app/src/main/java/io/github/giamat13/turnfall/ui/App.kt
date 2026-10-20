package io.github.giamat13.turnfall.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import io.github.giamat13.turnfall.GameViewModel
import io.github.giamat13.turnfall.Screen
import io.github.giamat13.turnfall.Sheet
import io.github.giamat13.turnfall.data.ThemeChoice

/**
 * The whole app: the main menu, or the play screen with its top bar, with the win card and the sheets over them.
 * The page's keys work on the play screen: the arrow keys turn, Z undoes, H hints, and Escape closes a sheet.
 */
@Composable
fun TurnfallApp(vm: GameViewModel) {
    val dark = when (vm.settings.theme) {
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
        ThemeChoice.AUTO -> isSystemInDarkTheme()
    }
    TurnfallTheme(dark) {
        val p = LocalPalette.current
        val config = LocalConfiguration.current
        val wideShort = config.screenWidthDp > config.screenHeightDp && config.screenHeightDp <= 560
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { focus.requestFocus() }
        // Android's back button closes a sheet first; on the play screen it goes back to the menu, and the menu exits.
        BackHandler(enabled = vm.sheets.isNotEmpty() || vm.screen == Screen.PLAY) {
            if (vm.sheets.isNotEmpty()) vm.closeTop() else vm.showMenu()
        }
        SystemBarsStyle(dark)
        Box(
            Modifier
                .fillMaxSize()
                .background(p.paper)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .focusRequester(focus)
                .focusable()
                .onKeyEvent { event -> handleKey(vm, event) },
            contentAlignment = Alignment.TopCenter,
        ) {
            if (vm.screen == Screen.PLAY) {
                Column(
                    Modifier
                        .widthIn(max = if (wideShort) 840.dp else 460.dp)
                        .fillMaxHeight()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = if (wideShort) 6.dp else 8.dp,
                            bottom = if (wideShort) 8.dp else 12.dp,
                        ),
                ) {
                    TopBar(vm)
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        PlayArea(vm)
                    }
                }
                if (vm.winShown) WinOverlay(vm)
            } else {
                Box(Modifier.widthIn(max = 460.dp).fillMaxHeight()) {
                    MainMenu(vm)
                }
            }
            for (sheet in vm.sheets) {
                when (sheet) {
                    Sheet.LEVELS -> LevelsSheet(vm)
                    Sheet.SETTINGS -> SettingsSheet(vm)
                    Sheet.MAP -> MapSheet(vm)
                }
            }
        }
    }
}

/** The status bar and navigation bar icons are dark on a light theme and light on a dark one. */
@Composable
private fun SystemBarsStyle(dark: Boolean) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}

private fun handleKey(vm: GameViewModel, event: KeyEvent): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    if (vm.sheets.isNotEmpty()) {
        if (event.key == Key.Escape) {
            vm.closeTop()
            return true
        }
        return false
    }
    if (vm.screen != Screen.PLAY || vm.winShown) return false
    return when (event.key) {
        Key.DirectionRight -> {
            vm.turn(1)
            true
        }
        Key.DirectionLeft -> {
            vm.turn(-1)
            true
        }
        Key.Z -> {
            vm.undo()
            true
        }
        Key.H -> {
            vm.useHint()
            true
        }
        else -> false
    }
}
