package io.github.giamat13.turnfall

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.giamat13.turnfall.ui.TurnfallApp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Runs the real screens on the JVM, with Robolectric: the main menu, the play screen, the levels, the settings and the
 * daily puzzle open and close without a crash. This does not replace a run on a phone; see the README.
 */
// Robolectric's Android 16 runtime needs Java 21, and this project builds on Java 17, so these run on Android 15.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppSmokeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theMenuAndTheScreensOpenAndCloseWithoutCrashing() {
        val game = GameViewModel(RuntimeEnvironment.getApplication())
        compose.setContent { TurnfallApp(game) }
        compose.waitForIdle()

        compose.onNodeWithText("Play").assertIsDisplayed()
        compose.onNodeWithText("Daily puzzle").assertIsDisplayed()

        compose.onNodeWithText("Levels").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Chapter 1: Falling").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Play").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Level 1").assertIsDisplayed()

        compose.onNodeWithContentDescription("Settings").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Speed").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Main menu").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Daily puzzle").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Turns").assertIsDisplayed()
    }
}
