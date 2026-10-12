package com.cinaywave.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cinaywave.app.playback.PlaybackManager
import com.cinaywave.app.playback.PlaybackState

@Composable
internal fun CinemaVideo(
    manager: PlaybackManager, state: PlaybackState, modifier: Modifier,
    controls: Boolean, locked: Boolean, dragging: Float?, feedback: String?, feedbackLeft: Boolean,
    onTap: () -> Unit, onSkip: (Boolean) -> Unit, onToggle: () -> Unit, onLock: () -> Unit,
    onDrag: (Float) -> Unit, onSeek: () -> Unit, onPanel: (String) -> Unit,
    onClose: () -> Unit, onFullscreen: () -> Unit, onPip: () -> Unit, next: (() -> Unit)?
) {
    val current = state.current ?: return
    val english = LocalContext.current.resources.configuration.locales[0].language == "en"
    val tap by rememberUpdatedState(onTap)
    val skip by rememberUpdatedState(onSkip)
    val motion = MotionAllowed()
    BoxWithConstraints(modifier.background(Color.Black).pointerInput(locked) {
        detectTapGestures(onTap = { tap() }, onDoubleTap = { if (!locked) skip(it.x < size.width / 2) })
    }) {
        val compact = maxWidth < 600.dp
        VideoSurface(manager, Modifier.fillMaxSize())
        AnimatedVisibility(feedback != null, Modifier.align(if (feedbackLeft) Alignment.CenterStart else Alignment.CenterEnd).padding(horizontal = 32.dp),
            enter = fadeIn(tween(if (motion) 100 else 0)) + scaleIn(tween(if (motion) 100 else 0)), exit = fadeOut(tween(if (motion) 100 else 0))) {
            Surface(shape = CircleShape, color = Color.Black.copy(alpha = .7f)) {
                Text(feedback.orEmpty(), Modifier.padding(22.dp), color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }
        if (locked) {
            Column(Modifier.align(Alignment.CenterStart).then(if (state.fullscreen) Modifier.safeDrawingPadding() else Modifier).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onLock) { Icon(Icons.Rounded.Lock, cinemaText("Mở khóa", "Unlock"), tint = Color.White) }
                Text(cinemaText("Mở khóa", "Unlock"), color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }
        AnimatedVisibility(!locked && (controls || !state.playing || state.buffering || state.error != null), Modifier.fillMaxSize(),
            enter = fadeIn(tween(if (motion) 160 else 0)), exit = fadeOut(tween(if (motion) 160 else 0))) {
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .28f))) {
                    Box(Modifier.fillMaxWidth().height(100.dp).align(Alignment.TopCenter)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .7f), Color.Transparent))))
                    Box(Modifier.fillMaxWidth().height(150.dp).align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .85f)))))
                    Box(Modifier.fillMaxSize().then(if (state.fullscreen) Modifier.safeDrawingPadding() else Modifier)) {
                        Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(current.detail.movie.name + " · " + episodeTitle(current.episode.name, english),
                                Modifier.weight(1f).padding(start = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                            if (compact) IconButton(onClick = onLock) { Icon(Icons.Rounded.LockOpen, cinemaText("Khóa", "Lock")) }
                            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, cinemaText("Thu về trình phát nhỏ", "Minimize player")) }
                        }
                        if (!compact) Column(Modifier.align(Alignment.CenterStart).padding(start = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = onLock) { Icon(Icons.Rounded.LockOpen, cinemaText("Khóa", "Lock")) }
                            Text(cinemaText("Khóa", "Lock"), style = MaterialTheme.typography.labelSmall)
                        }
                        Row(Modifier.align(Alignment.Center).offset(y = if (compact) (-8).dp else 0.dp),
                            horizontalArrangement = Arrangement.spacedBy(if (compact) 28.dp else 64.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onSkip(true) }, modifier = Modifier.size(if (compact) 48.dp else 64.dp), enabled = state.duration > 0) {
                                Icon(Icons.Rounded.Replay10, cinemaText("Lùi 10 giây", "Rewind 10 seconds"), Modifier.size(if (compact) 30.dp else 42.dp))
                            }
                            Box(Modifier.size(if (compact) 60.dp else 72.dp), contentAlignment = Alignment.Center) {
                                if (state.buffering) CircularProgressIndicator(Modifier.size(36.dp), color = EpisodeAccent)
                                else if (state.error == null) IconButton(onClick = onToggle, modifier = Modifier.fillMaxSize()) {
                                    Icon(if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        if (state.playing) cinemaText("Tạm dừng", "Pause") else cinemaText("Phát", "Play"), Modifier.size(if (compact) 42.dp else 56.dp))
                                }
                            }
                            IconButton(onClick = { onSkip(false) }, modifier = Modifier.size(if (compact) 48.dp else 64.dp), enabled = state.duration > 0) {
                                Icon(Icons.Rounded.Forward10, cinemaText("Tiến 10 giây", "Forward 10 seconds"), Modifier.size(if (compact) 30.dp else 42.dp))
                            }
                        }
                        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = if (compact) 12.dp else 28.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                val duration = state.duration.coerceAtLeast(1).toFloat()
                                Text(timeLabel((dragging ?: state.position.toFloat()).toLong()), style = MaterialTheme.typography.labelSmall)
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    LinearProgressIndicator(progress = { (state.bufferedPosition / duration).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(3.dp), color = Color.White.copy(alpha = .65f), trackColor = Color.White.copy(alpha = .25f),
                                        gapSize = 0.dp, drawStopIndicator = {})
                                    Slider(value = (dragging ?: state.position.toFloat()).coerceIn(0f, duration),
                                        onValueChange = onDrag, onValueChangeFinished = onSeek, valueRange = 0f..duration, enabled = state.duration > 0,
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                        colors = SliderDefaults.colors(thumbColor = EpisodeAccent, activeTrackColor = EpisodeAccent, inactiveTrackColor = Color.Transparent))
                                }
                                Text(timeLabel(state.duration), style = MaterialTheme.typography.labelSmall)
                            }
                            LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 12.dp)) {
                                item { PlayerAction(Icons.Rounded.Speed, cinemaText("Tốc độ phát", "Speed")) { onPanel("speed") } }
                                item { PlayerAction(Icons.Rounded.HighQuality, cinemaText("Chất lượng", "Quality")) { onPanel("quality") } }
                                item { PlayerAction(Icons.Rounded.VideoLibrary, cinemaText("Danh sách tập", "Episodes")) { onPanel("episodes") } }
                                item { PlayerAction(if (state.fullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen, if (state.fullscreen) cinemaText("Thoát toàn màn hình", "Exit fullscreen") else cinemaText("Toàn màn hình", "Fullscreen"), click = onFullscreen) }
                                item { PlayerAction(Icons.Rounded.SkipNext, cinemaText("Tập tiếp theo", "Next episode"), next != null) { next?.invoke() } }
                                item { PlayerAction(Icons.Rounded.PictureInPictureAlt, "PiP", click = onPip) }
                            }
                        }
                    }
                }
            }
        }
        state.error?.let { error ->
            Column(Modifier.align(Alignment.Center).background(Color.Black.copy(alpha = .9f)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error, color = Color.White, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = manager::retry) { Text(cinemaText("Thử lại", "Retry")) }
            }
        }
    }
}

@Composable
private fun PlayerAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, enabled: Boolean = true, click: () -> Unit) {
    TextButton(onClick = click, enabled = enabled, contentPadding = PaddingValues(horizontal = 8.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = Color.White, disabledContentColor = Color.White.copy(alpha = .35f)),
        modifier = Modifier.heightIn(min = 48.dp)) {
        Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text(title, style = MaterialTheme.typography.labelMedium)
    }
}
