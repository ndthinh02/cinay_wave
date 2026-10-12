package com.cinaywave.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import coil.compose.AsyncImage
import com.cinaywave.app.playback.PlaybackManager
import com.cinaywave.app.playback.PlaybackState

@Composable
internal fun PlayerEpisodePanel(manager: PlaybackManager, state: PlaybackState, close: () -> Unit) {
    val current = state.current ?: return
    val english = LocalContext.current.resources.configuration.locales[0].language == "en"
    var serverName by remember(current.detail.movie.slug) { mutableStateOf(current.server) }
    val episodes = current.detail.servers.find { it.name == serverName }?.episodes.orEmpty()
    val currentIndex = episodes.indexOfFirst { serverName == current.server && it.slug == current.episode.slug }
    var group by remember { mutableIntStateOf(currentIndex.coerceAtLeast(0) / 30) }
    val row = rememberLazyListState()
    val tabs = rememberLazyListState()
    val groups = remember(episodes.size) { episodeGroups(episodes.size) }
    val groupCount = groups.size
    LaunchedEffect(serverName, current.episode.slug) { group = currentIndex.coerceAtLeast(0) / 30 }
    LaunchedEffect(group, serverName, current.episode.slug) {
        row.scrollToItem(if (currentIndex / 30 == group && currentIndex >= 0) currentIndex % 30 else 0)
        if (groupCount > 0) tabs.animateScrollToItem(group.coerceIn(0, groupCount - 1))
    }
    BackHandler(onBack = close)
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .94f)).then(if (state.fullscreen) Modifier.safeDrawingPadding() else Modifier)) {
        val cardWidth = if (maxWidth > 600.dp) 260.dp else (maxWidth - 56.dp).coerceAtMost(280.dp)
        val panelScroll = rememberLazyListState()
        LazyColumn(state = panelScroll, contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp), modifier = Modifier.fillMaxSize()) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(current.detail.movie.name, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = Color.White,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = close) { Icon(Icons.Rounded.Close, cinemaText("Đóng danh sách tập", "Close episodes"), tint = Color.White) }
                }
            }
            if (current.detail.servers.size > 1) item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(current.detail.servers, key = { it.name }) { server ->
                        FilterChip(selected = serverName == server.name, onClick = { serverName = server.name }, label = { Text(server.name) },
                            colors = FilterChipDefaults.filterChipColors(labelColor = Color.White, selectedLabelColor = EpisodeAccent,
                                selectedContainerColor = EpisodeAccent.copy(alpha = .15f)))
                    }
                }
            }
            item {
                LazyRow(state = tabs, contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(groupCount) { number ->
                        val start = groups[number].first
                        val end = groups[number].last
                        TextButton(onClick = { group = number }, colors = ButtonDefaults.textButtonColors(contentColor = if (group == number) EpisodeAccent else Color.LightGray)) {
                            Text("${start.toString().padStart(2, '0')}–${end.toString().padStart(2, '0')}")
                        }
                    }
                }
            }
            if (episodes.isEmpty()) item { Text(cinemaText("Nguồn này chưa có tập phim.", "No episodes available."), Modifier.padding(20.dp), color = Color.LightGray) }
            else item {
                LazyRow(state = row, contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(episodes.drop(group * 30).take(30), key = { it.slug }) { episode ->
                        val selected = current.server == serverName && current.episode.slug == episode.slug
                        Card(onClick = { manager.start(current.detail, serverName, episode); close() }, enabled = episode.url.isNotBlank(),
                            modifier = Modifier.width(cardWidth), shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF17171C), contentColor = Color.White)) {
                            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Movie, null, Modifier.size(32.dp), tint = Color.LightGray)
                                MovieArtwork(current.detail.movie.backdrop.ifBlank { current.detail.movie.poster }, null,
                                    Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                Icon(if (selected && state.playing) Icons.Rounded.Equalizer else Icons.Rounded.PlayCircleOutline, null,
                                    Modifier.size(42.dp).background(Color.Black.copy(alpha = .4f), RoundedCornerShape(30.dp)),
                                    tint = if (selected) EpisodeAccent else Color.White)
                            }
                            if (selected) LinearProgressIndicator(
                                progress = { if (state.duration > 0) (state.position.toFloat() / state.duration).coerceIn(0f, 1f) else 0f },
                                modifier = Modifier.fillMaxWidth().height(2.dp), color = EpisodeAccent,
                                trackColor = Color.White.copy(alpha = .25f), gapSize = 0.dp, drawStopIndicator = {})
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(episodeTitle(episode.name, english), color = if (selected) EpisodeAccent else Color.White,
                                    style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(when {
                                    episode.url.isBlank() -> cinemaText("Chưa có nguồn", "Unavailable")
                                    selected && state.buffering -> cinemaText("Đang tải", "Buffering")
                                    selected && state.playing -> cinemaText("Đang xem", "Playing")
                                    selected -> cinemaText("Tạm dừng", "Paused")
                                    else -> cinemaText("Nhấn để xem", "Tap to watch")
                                }, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PlayerTrackPanel(manager: PlaybackManager, state: PlaybackState, panel: String, close: () -> Unit) {
    val speed = manager.player?.playbackParameters?.speed ?: 1f
    AlertDialog(onDismissRequest = close,
        title = { Text(when (panel) {
            "speed" -> cinemaText("Tốc độ phát", "Playback speed")
            "quality" -> cinemaText("Chất lượng", "Quality")
            else -> cinemaText("Âm thanh & phụ đề", "Audio & subtitles")
        }) }, text = {
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                if (panel == "speed") items(listOf(.5f, .75f, 1f, 1.25f, 1.5f, 2f)) { value ->
                    TextButton(onClick = { manager.speed(value); close() }) { Text("${value}×" + if (value == speed) " ✓" else "") }
                } else {
                    val types = if (panel == "quality") listOf(C.TRACK_TYPE_VIDEO) else listOf(C.TRACK_TYPE_AUDIO, C.TRACK_TYPE_TEXT)
                    types.forEach { type ->
                        val groups = state.tracks.groups.filter { it.type == type }
                        val supported = groups.sumOf { group -> (0 until group.length).count { group.isTrackSupported(it) } }
                        item {
                            Text(when (type) { C.TRACK_TYPE_VIDEO -> cinemaText("Video", "Video"); C.TRACK_TYPE_AUDIO -> cinemaText("Âm thanh", "Audio"); else -> cinemaText("Phụ đề", "Subtitles") }, style = MaterialTheme.typography.titleSmall)
                            if (supported == 0) Text(cinemaText("Nguồn này không có lựa chọn.", "No tracks available from this source."), style = MaterialTheme.typography.bodySmall)
                            else if (supported == 1) Text(cinemaText("Nguồn này chỉ có một lựa chọn.", "This source has only one track."), style = MaterialTheme.typography.bodySmall)
                        }
                        if (supported > 0) item {
                            TextButton(onClick = { manager.autoTrack(type); close() }) { Text(cinemaText("Tự động", "Automatic")) }
                        }
                        groups.forEach { group ->
                            for (index in 0 until group.length) if (group.isTrackSupported(index)) item {
                                val format = group.getTrackFormat(index)
                                val label = if (type == C.TRACK_TYPE_VIDEO) listOfNotNull(format.height.takeIf { it > 0 }?.let { "${it}p" },
                                    format.bitrate.takeIf { it > 0 }?.let { "${it / 1000} kbps" }).joinToString(" · ").ifBlank { format.sampleMimeType ?: "Video" }
                                else format.label ?: format.language ?: format.sampleMimeType ?: "Track ${index + 1}"
                                TextButton(onClick = { manager.selectTrack(group, index); close() }) { Text(label + if (group.isTrackSelected(index)) " ✓" else "") }
                            }
                        }
                        if (type == C.TRACK_TYPE_TEXT && supported > 0) item {
                            TextButton(onClick = { manager.disableSubtitles(); close() }) { Text(cinemaText("Tắt phụ đề", "Turn off subtitles")) }
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = close) { Text(cinemaText("Đóng", "Close")) } })
}
