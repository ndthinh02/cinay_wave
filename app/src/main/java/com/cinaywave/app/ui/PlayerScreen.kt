package com.cinaywave.app.ui

import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import com.cinaywave.app.playback.PlaybackManager
import java.util.Locale
import kotlinx.coroutines.delay

fun timeLabel(ms: Long): String {
    val seconds = ms / 1000
    return if (seconds >= 3600) String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60) else String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
}
@Composable fun VideoSurface(manager: PlaybackManager, modifier: Modifier) {
    val context = LocalContext.current
    val view = remember { TextureView(context) }
    val state by manager.state.collectAsStateWithLifecycle()
    DisposableEffect(view, state.current != null) { manager.attach(view); onDispose { manager.detach(view) } }
    BoxWithConstraints(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        val ratio = state.videoRatio.coerceAtLeast(.1f)
        val fit = if (maxWidth / maxHeight > ratio) Modifier.height(maxHeight).width(maxHeight * ratio) else Modifier.width(maxWidth).height(maxWidth / ratio)
        AndroidView(factory = { view }, modifier = fit, update = { manager.attach(it) })
        if (state.subtitles.isNotBlank()) Text(state.subtitles, Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).background(Color.Black.copy(alpha = .65f)).padding(4.dp), color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}
@Composable fun MiniPlayer(manager: PlaybackManager, expand: () -> Unit) {
    val uiText = androidx.compose.ui.platform.LocalContext.current.resources

    val state by manager.state.collectAsStateWithLifecycle()
    var displayed by remember { mutableStateOf(state.current) }
    state.current?.let { active -> SideEffect { displayed = active } }
    val current = state.current ?: displayed ?: return
    Surface(Modifier.padding(horizontal = 10.dp, vertical = 6.dp).clip(RoundedCornerShape(16.dp)), color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 6.dp) {
        Column {
            Row(Modifier.fillMaxWidth().height(74.dp).clickable(enabled = state.current != null, onClick = expand), verticalAlignment = Alignment.CenterVertically) {
                MovieArtwork(current.detail.movie.backdrop.ifBlank { current.detail.movie.poster }, null, Modifier.width(88.dp).height(66.dp).clip(RoundedCornerShape(12.dp)))
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) { Text(current.detail.movie.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall); Text(uiText.getString(com.cinaywave.app.R.string.episode_label, current.episode.name), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = manager::toggle, enabled = state.current != null) { Icon(if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (state.playing) uiText.getString(com.cinaywave.app.R.string.ui_pause_111) else uiText.getString(com.cinaywave.app.R.string.ui_play_112)) }
                IconButton(onClick = manager::close, enabled = state.current != null) { Icon(Icons.Rounded.Close, uiText.getString(com.cinaywave.app.R.string.ui_close_player_113)) }
            }
            LinearProgressIndicator(progress = { if (state.duration > 0) (state.position.toFloat() / state.duration).coerceIn(0f, 1f) else 0f }, modifier = Modifier.fillMaxWidth().height(2.dp))
        }
    }
}
@Composable
fun PlayerScreen(manager: PlaybackManager, back: () -> Unit, pip: () -> Unit, fullscreen: (Boolean) -> Unit) {
    val state by manager.state.collectAsStateWithLifecycle()
    val current = state.current
    var locked by rememberSaveable { mutableStateOf(false) }
    var panel by remember { mutableStateOf<String?>(null) }
    var dragging by remember { mutableStateOf<Float?>(null) }
    var controls by remember { mutableStateOf(true) }
    var interaction by remember { mutableIntStateOf(0) }
    var skipFeedback by remember { mutableStateOf<String?>(null) }
    var skipLeft by remember { mutableStateOf(false) }
    val motion = MotionAllowed()
    val english = LocalContext.current.resources.configuration.locales[0].language == "en"
    LaunchedEffect(controls, state.playing, state.buffering, state.error, locked, dragging, panel, interaction) {
        if (controls && state.playing && !state.buffering && state.error == null && !locked && dragging == null && panel == null) {
            delay(3000); controls = false
        }
    }
    LaunchedEffect(skipFeedback, interaction) { if (skipFeedback != null) { delay(800); skipFeedback = null } }
    LaunchedEffect(current?.detail?.movie?.slug, current?.server, current?.episode?.slug) {
        dragging = null; skipFeedback = null; controls = true; interaction++
    }
    fun minimize() { manager.save(); if (state.fullscreen) fullscreen(false); back() }
    BackHandler {
        when {
            panel != null -> panel = null
            locked -> { locked = false; controls = true; interaction++ }
            else -> minimize()
        }
    }
    if (current == null) {
        Column { PageTitle(cinemaText("Trình phát", "Player"), back = back); Feedback(empty = cinemaText("Phiên xem đã kết thúc.", "Your session has ended.")) }
        return
    }
    val episodes = current.detail.servers.find { it.name == current.server }?.episodes.orEmpty()
    val index = episodes.indexOfFirst { it.slug == current.episode.slug }
    val next = episodes.getOrNull(index + 1)?.takeIf { index >= 0 && it.url.isNotBlank() }
    var listServer by remember(current.detail.movie.slug, current.server) { mutableStateOf(current.server) }
    val columns = episodeColumns()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            val videoModifier = if (state.fullscreen) Modifier.weight(1f).fillMaxWidth() else Modifier.fillMaxWidth().aspectRatio(16f / 9f)
            CinemaVideo(manager, state, videoModifier, controls, locked, dragging, skipFeedback, skipLeft,
                onTap = { controls = !controls; interaction++ },
                onSkip = { left -> if (!locked) { manager.skip(if (left) -10000 else 10000); skipLeft = left; skipFeedback = if (left) "−10 s" else "+10 s"; interaction++ } },
                onToggle = { manager.toggle(); interaction++ },
                onLock = { locked = !locked; controls = true; interaction++ },
                onDrag = { dragging = it; interaction++ },
                onSeek = { dragging?.let { manager.seek(it.toLong()) }; dragging = null; interaction++ },
                onPanel = { panel = it; interaction++ }, onClose = ::minimize,
                onFullscreen = { fullscreen(!state.fullscreen) }, onPip = pip,
                next = next?.let { { manager.start(current.detail, current.server, it); interaction++; Unit } })
            if (!state.fullscreen) {
                androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                    item {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(current.detail.movie.name, style = MaterialTheme.typography.titleLarge)
                            Text(current.server + " · " + episodeTitle(current.episode.name, english), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item {
                        androidx.compose.foundation.lazy.LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(current.detail.servers.size) { i ->
                                val server = current.detail.servers[i]
                                FilterChip(selected = listServer == server.name, onClick = { listServer = server.name }, label = { Text(server.name) })
                            }
                        }
                    }
                    val listed = current.detail.servers.find { it.name == listServer }?.episodes.orEmpty()
                    item { Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) { EpisodeHeader(listed.size) } }
                    episodeGrid(listed, columns,
                        selected = { listServer == current.server && it.slug == current.episode.slug },
                        playing = state.playing, buffering = state.buffering) { manager.start(current.detail, listServer, it) }
                }
            }
        }
        AnimatedVisibility(panel == "episodes", Modifier.fillMaxSize(), enter = fadeIn(tween(if (motion) 180 else 0)) + slideInVertically(tween(if (motion) 180 else 0)) { it / 12 }, exit = fadeOut(tween(if (motion) 140 else 0))) {
            PlayerEpisodePanel(manager, state) { panel = null; controls = true; interaction++ }
        }
    }
    if (panel != null && panel != "episodes") PlayerTrackPanel(manager, state, panel!!) { panel = null; controls = true; interaction++ }
}
