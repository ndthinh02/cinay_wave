package com.cinaywave.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cinaywave.app.data.Episode

internal val EpisodeAccent = Gold

@Composable
internal fun cinemaText(vi: String, en: String): String =
    if (LocalContext.current.resources.configuration.locales[0].language == "en") en else vi

@Composable
internal fun episodeColumns(): Int {
    val config = LocalConfiguration.current
    return when {
        config.screenWidthDp / config.fontScale < 300 -> 1
        config.screenWidthDp / config.fontScale < 340 -> 2
        else -> 3
    }
}

@Composable
internal fun EpisodeHeader(count: Int) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = accent.copy(alpha = .12f)) {
                Icon(Icons.Rounded.VideoLibrary, null, Modifier.padding(12.dp).size(24.dp), tint = accent)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(cinemaText("Danh sách tập", "Episodes"), style = MaterialTheme.typography.titleMedium)
                Text(cinemaText("Chọn tập để bắt đầu xem", "Choose an episode to watch"), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(cinemaText("$count tập", "$count episodes"), Modifier.padding(horizontal = 10.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

internal fun LazyListScope.episodeGrid(
    episodes: List<Episode>, columns: Int, selected: (Episode) -> Boolean,
    playing: Boolean, buffering: Boolean, click: (Episode) -> Unit
) {
    items(episodes.chunked(columns), key = { row -> row.first().slug }) { row ->
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 18.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { episode ->
                EpisodeTile(episode, selected(episode), playing, buffering, Modifier.weight(1f).fillMaxHeight()) { click(episode) }
            }
            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun EpisodeTile(episode: Episode, selected: Boolean, playing: Boolean, buffering: Boolean, modifier: Modifier, click: () -> Unit) {
    val english = LocalContext.current.resources.configuration.locales[0].language == "en"
    val motion = MotionAllowed()
    val accent = MaterialTheme.colorScheme.primary
    val animation = androidx.compose.animation.core.tween<Color>(if (motion) 160 else 0)
    val background by animateColorAsState(if (selected) accent.copy(alpha = .10f) else MaterialTheme.colorScheme.surfaceContainer, animationSpec = animation, label = "episodeBackground")
    val border by animateColorAsState(if (selected) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f), animationSpec = animation, label = "episodeBorder")
    Surface(onClick = click, enabled = episode.url.isNotBlank(), modifier = modifier.heightIn(min = 64.dp),
        shape = RoundedCornerShape(16.dp), color = background, border = BorderStroke(1.dp, border)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
            val tint = if (selected) accent else MaterialTheme.colorScheme.onSurface
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(episodeTitle(episode.name, english), modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge, color = tint)
            }
            if (selected || episode.url.isBlank()) Text(when {
                episode.url.isBlank() -> cinemaText("Chưa có nguồn", "Unavailable")
                selected && buffering -> cinemaText("Đang tải", "Buffering")
                selected && playing -> cinemaText("Đang xem", "Playing")
                selected -> cinemaText("Tạm dừng", "Paused")
                else -> ""
            }, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall, color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
