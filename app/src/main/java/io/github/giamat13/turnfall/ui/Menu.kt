package io.github.giamat13.turnfall.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.giamat13.turnfall.GameViewModel
import io.github.giamat13.turnfall.game.dateKey
import io.github.giamat13.turnfall.game.dayLabel
import io.github.giamat13.turnfall.game.starText
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The main menu, the first screen of the app: the logo and the title, then the four ways in, then a line of figures.
 * The menu is an Android addition; the browser page starts on the game.
 */
@Composable
fun MainMenu(vm: GameViewModel) {
    val p = LocalPalette.current
    val now = LocalDate.now(ZoneOffset.UTC)
    val today = dateKey(now)
    val stats = vm.progress.stats(now)
    val todayStars = vm.progress.daily[today]?.s ?: 0
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Logo(Modifier.size(104.dp))
        GameText(
            text = "Turnfall",
            style = TextStyle(
                color = p.ink,
                fontFamily = SecularFamily,
                fontSize = 52.sp,
                lineHeight = 56.sp,
                textAlign = TextAlign.Center,
            ),
        )
        GameText(
            text = "Turn the world. Roll the ball.",
            style = TextStyle(
                color = p.ink2,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(Modifier.height(18.dp))
        Column(
            Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MenuButton(
                title = "Play",
                subtitle = "Level ${vm.nextLevelNumber()}",
                icon = IconPlay,
                primary = true,
                onClick = { vm.playFromMenu() },
            )
            MenuButton(
                title = "Daily puzzle",
                subtitle = "${dayLabel(today)} · ${starText(todayStars)}",
                icon = IconCalendar,
                onClick = { vm.playToday() },
            )
            MenuButton(
                title = "Levels",
                subtitle = "Chapters, levels and the map",
                icon = IconLevels,
                onClick = { vm.openLevels() },
            )
            MenuButton(
                title = "Settings",
                subtitle = "Speed, theme and feedback",
                icon = IconSettings,
                onClick = { vm.openSettings() },
            )
        }
        Spacer(Modifier.height(10.dp))
        GameText(
            text = "Solved ${stats.solved} · Three stars ${stats.threeStars} · Streak ${stats.streak}",
            style = TextStyle(
                color = p.ink2,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

/** One way in: a large button with its name, a line under it, and an icon. The first one is the sun-coloured Play. */
@Composable
private fun MenuButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    val ink = if (primary) p.onSun else p.ink
    Chunky(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        shape = RoundedCornerShape(22.dp),
        color = if (primary) p.sun else p.surface,
        edgeHeight = 6.dp,
        onClick = onClick,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            VectorIcon(icon, ink, Modifier.size(26.dp))
            Column(Modifier.weight(1f)) {
                GameText(
                    text = title,
                    style = TextStyle(
                        color = ink,
                        fontFamily = SecularFamily,
                        fontSize = 22.sp,
                        lineHeight = 26.sp,
                    ),
                )
                GameText(
                    text = subtitle,
                    style = TextStyle(
                        color = if (primary) p.onSun else p.ink2,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    ),
                )
            }
        }
    }
}

/** The logo: the ball of the brand, with its shine and a lower edge. */
@Composable
private fun Logo(modifier: Modifier) {
    val p = LocalPalette.current
    Canvas(modifier) {
        val r = size.minDimension * 0.36f
        val center = Offset(size.width / 2, size.height / 2)
        drawCircle(p.sunEdge, radius = r, center = Offset(center.x, center.y + 5.dp.toPx()))
        drawCircle(p.sun, radius = r, center = center)
        drawCircle(
            Color.White.copy(alpha = 0.8f),
            radius = r * 0.2f,
            center = Offset(center.x - r * 0.35f, center.y - r * 0.35f),
        )
    }
}
