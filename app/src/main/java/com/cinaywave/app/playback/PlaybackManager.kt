package com.cinaywave.app.playback

import android.content.Context
import android.view.TextureView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.ExoPlayer
import com.cinaywave.app.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

data class Playing(val detail: Detail, val server: String, val episode: Episode, val owner: String?)
data class PlaybackState(
    val current: Playing? = null, val playing: Boolean = false, val buffering: Boolean = false,
    val position: Long = 0, val duration: Long = 0, val error: String? = null,
    val tracks: Tracks = Tracks.EMPTY, val fullscreen: Boolean = false, val pip: Boolean = false,
    val videoRatio: Float = 16f / 9f, val subtitles: String = "", val bufferedPosition: Long = 0
)
/** Application-owned session. UI surfaces attach/detach; only close or process teardown releases. */
class PlaybackManager(private val context: Context, private val repo: Repository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PlaybackState())
    val state = _state.asStateFlow()
    var player: ExoPlayer? = null
        private set
    private var surface: TextureView? = null
    private var pendingSeek: Long? = null
    private var tick: Job? = null
    private var switchJob: Job? = null
    private var generation = 0
    private var resumeAfterBackground = false
    private var switching = false
    private var lastSaved = ""
    private var lastSavedAt = 0L
    init {
        scope.launch {
            repo.auth.collect { session ->
                val current = _state.value.current
                if (session != null && current != null && current.owner == null) {
                    _state.value = _state.value.copy(current = current.copy(owner = session.user.id))
                    save()
                }
            }
        }
    }
    private fun createPlayer(): ExoPlayer = ExoPlayer.Builder(context).build().apply {
        setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
        setHandleAudioBecomingNoisy(true)
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(playing = isPlaying)
                if (!isPlaying) save()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                _state.value = _state.value.copy(buffering = playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY) {
                    pendingSeek?.let { seekTo(if (duration > 0) it.coerceAtMost(duration) else it) }; pendingSeek = null
                    update()
                }
                if (playbackState == Player.STATE_ENDED) save()
            }
            override fun onPlayerError(error: PlaybackException) { _state.value = _state.value.copy(error = "Không phát được nguồn này. Thử lại hoặc chọn server/tập khác.", buffering = false) }
            override fun onTracksChanged(tracks: Tracks) { _state.value = _state.value.copy(tracks = tracks) }
            override fun onVideoSizeChanged(videoSize: VideoSize) { if (videoSize.height > 0) _state.value = _state.value.copy(videoRatio = videoSize.width.toFloat() * videoSize.pixelWidthHeightRatio / videoSize.height) }
            override fun onCues(cueGroup: CueGroup) { _state.value = _state.value.copy(subtitles = cueGroup.cues.mapNotNull { it.text?.toString() }.joinToString("\n")) }
        })
    }
    fun start(detail: Detail, serverName: String, episode: Episode, resume: Progress? = null) {
        val current = _state.value.current
        if (current?.detail?.movie?.slug == detail.movie.slug && current.server == serverName && current.episode.slug == episode.slug) {
            player?.let { if (it.playbackState == Player.STATE_ENDED) it.seekTo(0); it.play() }
            update()
            return
        }
        save()
        generation++
        switchJob?.cancel()
        if (episode.url.isBlank()) {
            _state.value = _state.value.copy(error = "Tập này chưa có liên kết HLS. Hãy chọn tập hoặc nguồn khác.")
            return
        }
        val p = player ?: createPlayer().also { player = it }
        switching = true
        p.stop()
        _state.value = PlaybackState(current = Playing(detail, serverName, episode, repo.auth.value?.user?.id), buffering = true, fullscreen = _state.value.fullscreen)
        pendingSeek = resume?.takeIf { !it.completed && it.server == serverName && it.episode == episode.slug }?.positionMs
        p.setMediaItem(MediaItem.fromUri(episode.url))
        p.prepare(); p.play()
        switching = false
        surface?.let(p::setVideoTextureView)
        if (tick == null) tick = scope.launch {
            var seconds = 0
            while (isActive) {
                delay(500); update(); seconds++
                if (seconds >= 30) {
                    seconds = 0
                    if (player?.isPlaying == true) save() else scope.launch(Dispatchers.IO) { repo.flushProgress() }
                }
            }
        }
    }
    fun resume(progress: Progress, onReady: () -> Unit, onError: (String) -> Unit) {
        val version = ++generation
        switchJob?.cancel()
        switchJob = scope.launch {
            try {
                val detail = repo.api.detail(progress.movie.slug)
                if (version != generation) return@launch
                val s = detail.servers.find { it.name == progress.server }
                val e = s?.episodes?.find { it.slug == progress.episode }
                if (s == null || e == null || e.url.isBlank()) { onError("Tập đang xem không còn trong nguồn. Hãy chọn lại tập."); return@launch }
                // Clear the job before start so it does not cancel itself.
                switchJob = null
                start(detail, s.name, e, progress); onReady()
            } catch (e: CancellationException) { throw e } catch (e: Exception) { onError(errorMessage(e, repo.language.value)) }
        }
    }
    fun attach(view: TextureView) {
        if (surface === view) return
        // ExoPlayer replaces the previous TextureView itself. Never clear a newer target
        // when the outgoing Compose surface is disposed later in the same frame.
        surface = view; player?.setVideoTextureView(view)
    }
    fun detach(view: TextureView) {
        if (surface !== view) return
        player?.clearVideoTextureView(view); surface = null
    }
    private fun update() { val p = player ?: return; _state.value = _state.value.copy(position = p.currentPosition.coerceAtLeast(0), duration = p.duration.takeIf { it > 0 } ?: 0, playing = p.isPlaying, bufferedPosition = p.bufferedPosition.coerceAtLeast(0)) }
    fun save() {
        if (switching || pendingSeek != null) return
        val c = _state.value.current ?: return
        val p = player ?: return
        val position = p.currentPosition.coerceAtLeast(0)
        val duration = p.duration.takeIf { it > 0 } ?: 0
        val signature = "${c.owner}|${c.detail.movie.slug}|${c.server}|${c.episode.slug}|$position|$duration"
        val now = System.currentTimeMillis()
        if (signature == lastSaved && now - lastSavedAt < 1000) return
        lastSaved = signature; lastSavedAt = now
        // Owner is captured at episode start; signing in/out cannot reassign this episode's queue.
        val progress = Progress(c.detail.movie, c.server, c.episode.slug, c.episode.name, position, duration, Instant.now().toString(), duration > 0 && position.toDouble() / duration > .95)
        repo.enqueue(c.owner, progress)
        scope.launch(Dispatchers.IO) { repo.flushProgress() }
    }
    fun toggle() { player?.let { if (it.playWhenReady) it.pause() else it.play() }; update(); save() }
    fun seek(position: Long) {
        val duration = _state.value.duration
        if (duration <= 0) return
        player?.seekTo(position.coerceIn(0, duration)); update(); save()
    }
    fun skip(ms: Long) = seek((player?.currentPosition ?: 0) + ms)
    fun speed(value: Float) { player?.setPlaybackSpeed(value) }
    fun selectTrack(group: Tracks.Group, index: Int) {
        player?.let { p -> p.trackSelectionParameters = p.trackSelectionParameters.buildUpon().setTrackTypeDisabled(group.type, false).setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, index)).build() }
    }
    fun autoTrack(type: Int) { player?.let { it.trackSelectionParameters = it.trackSelectionParameters.buildUpon().clearOverridesOfType(type).setTrackTypeDisabled(type, false).build() } }
    fun disableSubtitles() { player?.let { it.trackSelectionParameters = it.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build() } }
    fun retry() { _state.value = _state.value.copy(error = null); player?.prepare(); player?.play() }
    fun fullscreen(value: Boolean) { _state.value = _state.value.copy(fullscreen = value) }
    fun pip(value: Boolean) { _state.value = _state.value.copy(pip = value) }
    fun background() {
        if (_state.value.pip) return
        resumeAfterBackground = player?.playWhenReady == true; player?.pause(); save()
    }
    fun foreground() { if (resumeAfterBackground) { player?.play(); resumeAfterBackground = false }; scope.launch(Dispatchers.IO) { repo.flushProgress() } }
    fun close() {
        save(); generation++; switchJob?.cancel(); tick?.cancel(); tick = null
        switching = true
        surface?.let { player?.clearVideoTextureView(it) }; surface = null
        player?.release(); player = null; pendingSeek = null; resumeAfterBackground = false; _state.value = PlaybackState(); switching = false
    }
}
