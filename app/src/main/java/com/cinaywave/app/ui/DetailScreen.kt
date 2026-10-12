package com.cinaywave.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cinaywave.app.data.*
import com.cinaywave.app.feature.*
import com.cinaywave.app.playback.PlaybackManager
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    slug: String,
    vm: AppViewModel,
    playback: PlaybackManager,
    back: () -> Unit,
    open: (Movie) -> Unit,
    favorite: (Movie) -> Unit,
    play: () -> Unit,
    writeReview: () -> Unit
) {
    var load by remember(slug) { mutableStateOf(Load<Detail>(loading = true)) }
    var retry by remember { mutableIntStateOf(0) }
    var serverName by rememberSaveable(slug) { mutableStateOf("") }
    var expanded by rememberSaveable(slug) { mutableStateOf(false) }
    var episodeQuery by rememberSaveable(slug) { mutableStateOf("") }
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val playing by playback.state.collectAsStateWithLifecycle()
    val motion = MotionAllowed()
    val columns = episodeColumns()
    val reviewsModel = reviewViewModel(slug, vm.repo)
    val reviewRevision by vm.repo.reviewChanges.collectAsStateWithLifecycle()
    LaunchedEffect(slug, reviewRevision) { reviewsModel.refresh() }
    val scroll = rememberLazyListState()
    var collapseProgress by rememberSaveable(slug) { mutableFloatStateOf(0f) }
    val collapseRange = with(LocalDensity.current) { 269.dp.toPx() }
    val scrollConnection = remember(scroll, collapseRange, slug) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (load.data == null) return Offset.Zero
                val mayExpand = scroll.firstVisibleItemIndex == 0 && scroll.firstVisibleItemScrollOffset == 0
                if (available.y >= 0f && !mayExpand) return Offset.Zero
                val before = collapseProgress * collapseRange
                val after = (before - available.y).coerceIn(0f, collapseRange)
                collapseProgress = after / collapseRange
                return Offset(0f, before - after)
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y <= 0f || scroll.canScrollBackward || load.data == null) return Offset.Zero
                val before = collapseProgress * collapseRange
                val after = (before - available.y).coerceIn(0f, collapseRange)
                collapseProgress = after / collapseRange
                return Offset(0f, before - after)
            }
        }
    }
    LaunchedEffect(slug, retry) {
        load = Load(loading = true)
        try {
            val detail = vm.repo.api.detail(slug)
            load = Load(detail)
            if (detail.servers.none { it.name == serverName }) serverName = detail.servers.firstOrNull()?.name.orEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            load = Load(error = errorMessage(e, vm.repo.language.value))
        }
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
    LazyColumn(
        state = scroll,
        modifier = Modifier.fillMaxSize().padding(top = 56.dp).nestedScroll(scrollConnection),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        if (load.data == null) {
            item { PageTitle(cinemaText("Chi tiết phim", "Movie details"), back = back) }
            item { Feedback(load.loading, load.error, retry = { retry++ }) }
        }
        load.data?.let { detail ->
            val movie = detail.movie
            val server = detail.servers.find { it.name == serverName }
            val saved =
                history.data.orEmpty().find { it.movie.slug == slug && it.server == serverName && !it.completed }
            val firstPlayable = server?.episodes?.firstOrNull { it.url.isNotBlank() }
            val playEpisode =
                server?.episodes?.find { it.slug == saved?.episode && it.url.isNotBlank() } ?: firstPlayable
            item {
                Box(Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .height(270.dp - 269.dp * collapseProgress)
                    .clipToBounds()
                    .background(MaterialTheme.colorScheme.surfaceVariant)) {
                    MovieArtwork(
                        movie.backdrop.ifBlank { movie.poster },
                        movie.name,
                        Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true)
                            .requiredHeight(270.dp).graphicsLayer { translationY = -collapseRange * collapseProgress * .25f },
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = .2f),
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.surface.copy(alpha = .65f)
                                    )
                                )
                            )
                    )
                    MovieArtwork(movie.poster, movie.name,
                        Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 14.dp)
                            .wrapContentSize(Alignment.BottomStart, unbounded = true)
                            .requiredSize(width = 64.dp, height = 96.dp).clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop)
                    FilledIconButton(
                        onClick = {
                            if (server != null && playEpisode != null) {
                                playback.start(detail, server.name, playEpisode, saved); play()
                            }
                        },
                        enabled = playEpisode != null,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .requiredSize(64.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Black.copy(alpha = .55f),
                            contentColor = Gold
                        )
                    ) {
                        Icon(Icons.Rounded.PlayArrow, cinemaText("Xem phim", "Watch movie"), Modifier.size(40.dp))
                    }
                }
            }
            item {
                Column(
                    Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(movie.name, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                        if (movie.quality.isNotBlank()) DetailBadge(movie.quality)
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (movie.time.isNotBlank()) Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.Schedule, null, Modifier.size(14.dp)); Text(
                            movie.time,
                            style = MaterialTheme.typography.bodySmall
                        )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Rounded.VideoLibrary,
                                null,
                                Modifier.size(14.dp)
                            ); Text(
                            cinemaText(
                                "${server?.episodes?.size ?: 0} tập",
                                "${server?.episodes?.size ?: 0} episodes"
                            ), style = MaterialTheme.typography.bodySmall
                        )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f))
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                cinemaText("Năm phát hành", "Release year"),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                movie.year?.toString() ?: "—",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(cinemaText("Thể loại", "Genre"), style = MaterialTheme.typography.titleSmall)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                movie.categories.forEach { DetailBadge(it.name) }
                            }
                        }
                    }
                    Text(cinemaText("Giới thiệu", "Synopsis"), style = MaterialTheme.typography.titleMedium)
                    Column(Modifier.animateContentSize(tween(if (motion) 200 else 0))) {
                        Text(
                            detail.description.ifBlank {
                                cinemaText(
                                    "Chưa có nội dung giới thiệu.",
                                    "No synopsis available."
                                )
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (expanded) Int.MAX_VALUE else 5,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (detail.description.length > 180) TextButton(
                            onClick = { expanded = !expanded },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                if (expanded) cinemaText("Thu gọn", "Show less") else cinemaText(
                                    "Xem thêm",
                                    "Read more"
                                )
                            )
                        }
                    }
                    val actors = detail.actors.orEmpty().map(String::trim).filter(String::isNotBlank).distinct()
                    if (actors.isNotEmpty()) {
                        Text(cinemaText("Diễn viên", "Cast"), style = MaterialTheme.typography.titleMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            actors.forEach { DetailBadge(it) }
                        }
                    }
                }
            }
            item { ReviewSummary(slug, vm.repo, writeReview) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(detail.servers, key = { it.name }) { source ->
                        FilterChip(
                            selected = source.name == serverName,
                            onClick = { serverName = source.name; episodeQuery = "" },
                            label = { Text(source.name) })
                    }
                }
            }
            item {
                Box(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                    EpisodeHeader(
                        server?.episodes?.size ?: 0
                    )
                }
            }
            if (server == null || server.episodes.isEmpty()) item {
                Feedback(
                    empty = cinemaText(
                        "Nguồn này chưa có tập phim.",
                        "No episodes on this server."
                    )
                )
            }
            else {
                if (server.episodes.size > 12) item {
                    OutlinedTextField(
                        (episodeQuery as Any?)?.toString() ?: "",
                        { episodeQuery = it },
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        singleLine = true,
                        label = { Text(cinemaText("Tìm tập", "Find episode")) },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) })
                }
                val episodes = server.episodes.filter { episodeQuery.isBlank() || it.name.contains(episodeQuery, true) }
                if (episodes.isEmpty()) item {
                    Text(
                        cinemaText("Không tìm thấy tập.", "No matching episodes."),
                        Modifier.padding(18.dp)
                    )
                }
                episodeGrid(
                    episodes,
                    columns,
                    selected = { ep -> playing.current?.let { it.detail.movie.slug == slug && it.server == server.name && it.episode.slug == ep.slug } == true },
                    playing = playing.playing,
                    buffering = playing.buffering
                ) { episode ->
                    val progress = history.data.orEmpty()
                        .find { it.movie.slug == slug && it.server == server.name && it.episode == episode.slug }
                    playback.start(detail, server.name, episode, progress); play()
                }
            }
            item { ReviewSection(slug, vm.repo) }
            if (detail.related.isNotEmpty()) {
                item {
                    Text(
                        cinemaText("Phim liên quan", "Related movies"),
                        Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(detail.related, key = { it.slug }) { related ->
                            Card(
                                onClick = { open(related) },
                                modifier = Modifier.width(180.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    MovieArtwork(
                                        related.backdrop.ifBlank { related.poster },
                                        null,
                                        Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Text(
                                    related.name,
                                    Modifier.fillMaxWidth().padding(top = 8.dp),
                                    minLines = 2,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    Surface(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .65f + .35f * collapseProgress)) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) { Icon(Icons.Rounded.ChevronLeft, cinemaText("Quay lại", "Back")) }
            Text(if (collapseProgress >= 1f) load.data?.movie?.name.orEmpty() else cinemaText("Chi tiết phim", "Movie details"),
                Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
            load.data?.movie?.let { movie ->
                val liked = favorites.data.orEmpty().any { it.slug == slug }
                IconButton(onClick = { favorite(movie) }) { Icon(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    cinemaText("Yêu thích", "Favorite"), tint = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
            }
        }
    }
    }
}

@Composable
private fun DetailBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

