package io.github.giamat13.turnfall

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.giamat13.turnfall.ui.TurnfallApp
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Saves pictures of the screens to app/build/screenshots, so the look can be compared with the browser version. The
 * pictures are drawn on the JVM by Robolectric, not on a phone.
 */
// Robolectric's Android 16 runtime needs Java 21, and this project builds on Java 17, so these run on Android 15.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w390dp-h844dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun saveTheScreens() {
        val game = GameViewModel(RuntimeEnvironment.getApplication())
        compose.setContent { TurnfallApp(game) }
        compose.waitForIdle()
        save("menu-390x844")

        compose.onNodeWithText("Play").performClick()
        compose.waitForIdle()
        save("play-390x844")
        for (label in listOf("Level 1", "Turns", "Goal")) {
            val box = compose.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            println("BOUNDS $label top=${box.top} bottom=${box.bottom}")
        }

        compose.onNodeWithContentDescription("Levels").performClick()
        compose.waitForIdle()
        save("levels-390x844")
        compose.onNodeWithContentDescription("Close").performClick()
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Settings").performClick()
        compose.waitForIdle()
        save("settings-390x844")
    }

    /** Lays the window out at the phone size, then draws it into a bitmap. */
    private fun save(name: String) {
        compose.waitForIdle()
        val view: View = compose.activity.window.decorView
        val width = 390
        val height = 844
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, width, height)
        compose.waitForIdle()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
