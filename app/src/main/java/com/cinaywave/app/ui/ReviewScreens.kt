package com.cinaywave.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cinaywave.app.data.*
import com.cinaywave.app.feature.ReviewViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun reviewViewModel(slug: String, repo: Repository): ReviewViewModel = viewModel(key = "reviews/$slug", factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T = ReviewViewModel(repo, slug, extras.createSavedStateHandle()) as T
})

@Composable
internal fun ReviewSummary(slug: String, repo: Repository, write: () -> Unit) {
    val vm = reviewViewModel(slug, repo)
    val state by vm.reviews.collectAsStateWithLifecycle()
    Card(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(cinemaText("Đánh giá", "Ratings"), style = MaterialTheme.typography.titleMedium)
            when {
                state.loading && state.total == 0 -> CircularProgressIndicator(Modifier.size(24.dp))
                state.error != null -> {
                    Text(state.error.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = vm::refresh) { Text(cinemaText("Thử lại", "Retry")) }
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.Star, null, tint = MaterialTheme.colorScheme.primary)
                        if (state.total == 0) Text(cinemaText("Chưa có đánh giá", "No ratings yet"), style = MaterialTheme.typography.titleSmall)
                        else {
                            Text(String.format(Locale.US, "%.1f / 5", state.average), style = MaterialTheme.typography.titleLarge)
                            Text(cinemaText("${state.total} lượt", "${state.total} ratings"), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Button(onClick = write, enabled = !state.loading && !state.deleting, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                        Text(if (state.mine == null) cinemaText("Viết đánh giá", "Write a review") else cinemaText("Sửa đánh giá", "Edit your review"))
                    }
                }
            }
        }
    }
}

@Composable
internal fun ReviewSection(slug: String, repo: Repository) {
    val vm = reviewViewModel(slug, repo)
    val state by vm.reviews.collectAsStateWithLifecycle()
    if (state.items.isEmpty() && state.mine == null && (state.loading || state.error != null)) return
    var confirmDelete by rememberSaveable(slug) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        HorizontalDivider()
        Text(cinemaText("Nhận xét", "Reviews"), style = MaterialTheme.typography.titleMedium)
        if (state.loading) CircularProgressIndicator(Modifier.size(28.dp))
        else if (state.error != null) {
            Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = vm::refresh) { Text(cinemaText("Thử lại", "Retry")) }
        } else {
            state.ownError?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = vm::refresh) { Text(cinemaText("Thử lại", "Retry")) } }
            val reviews = remember(state.mine, state.items) {
                (listOfNotNull(state.mine) + state.items).distinctBy { it.id }
                    .sortedWith(compareByDescending<FilmReview> { it.updatedAt }.thenByDescending { it.id })
            }
            if (reviews.isEmpty()) Text(cinemaText("Chưa có nhận xét. Hãy chia sẻ cảm nhận của bạn.", "No reviews yet. Share your thoughts."), color = MaterialTheme.colorScheme.onSurfaceVariant)
            reviews.forEach { review ->
                key(review.id) {
                    ReviewCard(review, deleting = state.deleting,
                        delete = if (review.id == state.mine?.id) ({ confirmDelete = true }) else null)
                }
            }
            state.moreError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.cursor != null) OutlinedButton(onClick = vm::more, enabled = !state.moreLoading && !state.deleting, modifier = Modifier.fillMaxWidth()) {
                if (state.moreLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(if (state.moreError != null) cinemaText("Thử tải lại", "Retry") else cinemaText("Tải thêm nhận xét", "Load more reviews"))
            }
        }
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false },
        title = { Text(cinemaText("Xóa đánh giá?", "Delete your rating?")) },
        text = { Text(cinemaText("Điểm và nhận xét của bạn sẽ được xóa.", "Your rating and review will be removed.")) },
        confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete() }) { Text(cinemaText("Xóa", "Delete")) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(cinemaText("Hủy", "Cancel")) } })
}

@Composable
private fun ReviewCard(review: FilmReview, deleting: Boolean = false, delete: (() -> Unit)? = null) {
    var expanded by rememberSaveable(review.id, review.updatedAt) { mutableStateOf(false) }
    var overflowing by remember(review.id, review.updatedAt) { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    if (review.author.avatarUrl.isNotBlank()) AsyncImage(review.author.avatarUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Column(Modifier.weight(1f)) {
                    Text(review.author.name, style = MaterialTheme.typography.titleSmall)
                    val date = remember(review.updatedAt) { runCatching { DateTimeFormatter.ofPattern("dd/MM/yyyy").format(Instant.parse(review.updatedAt).atZone(ZoneId.systemDefault())) }.getOrDefault("") }
                    Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row { repeat(5) { index -> Icon(if (index < review.rating) Icons.Rounded.Star else Icons.Rounded.StarBorder, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) } }
            }
            if (review.content.isNotBlank()) {
                Text(review.content, maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis,
                    onTextLayout = { if (!expanded) overflowing = it.hasVisualOverflow }, style = MaterialTheme.typography.bodyMedium)
                if (expanded || overflowing) TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
                    Text(if (expanded) cinemaText("Thu gọn", "Show less") else cinemaText("Xem thêm", "Read more"))
                }
            }
            if (delete != null) TextButton(onClick = delete, enabled = !deleting, contentPadding = PaddingValues(0.dp)) {
                if (deleting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(cinemaText("Xóa đánh giá", "Delete your rating"))
            }
        }
    }
}

@Composable
internal fun ReviewEditorScreen(slug: String, repo: Repository, back: () -> Unit, login: () -> Unit) {
    val vm = reviewViewModel(slug, repo)
    val state by vm.editor.collectAsStateWithLifecycle()
    val auth by repo.auth.collectAsStateWithLifecycle()
    val movie by vm.movie.collectAsStateWithLifecycle()
    val movieError by vm.movieError.collectAsStateWithLifecycle()
    val motion = MotionAllowed()
    BackHandler(state.saving) { }
    LaunchedEffect(slug) { vm.loadEditor() }
    LaunchedEffect(state.saved) { if (state.saved) back() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { PageTitle(if (state.existing) cinemaText("Sửa đánh giá", "Edit review") else cinemaText("Viết đánh giá", "Write review"), back = { if (!state.saving) back() }) }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                movie?.let { film ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MovieArtwork(film.poster, film.name, Modifier.size(width = 56.dp, height = 84.dp).clip(MaterialTheme.shapes.small))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(film.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                                Text(listOfNotNull(film.year?.toString(), film.quality.takeIf(String::isNotBlank)).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                movieError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error); TextButton(onClick = vm::loadMovie) { Text(cinemaText("Tải lại thông tin phim", "Reload movie details")) } }
                when {
                    auth == null -> Button(onClick = login) { Text(cinemaText("Đăng nhập để đánh giá", "Sign in to rate")) }
                    state.loading -> CircularProgressIndicator()
                    else -> {
                        Text(cinemaText("Bạn thấy bộ phim thế nào?", "How was the movie?"), style = MaterialTheme.typography.titleMedium)
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val starWidth = (maxWidth / 5).coerceAtMost(64.dp)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                for (value in 1..5) {
                                    val scale by animateFloatAsState(if (value <= state.rating && motion) 1.08f else 1f, tween(if (motion) 120 else 0), label = "rating star $value")
                                    IconToggleButton(checked = value <= state.rating, onCheckedChange = { vm.rating(value) }, enabled = !state.saving, modifier = Modifier.width(starWidth).heightIn(min = 56.dp)) {
                                        Icon(if (value <= state.rating) Icons.Rounded.Star else Icons.Rounded.StarBorder, cinemaText("$value sao", "$value stars"), Modifier.size(32.dp).graphicsLayer { scaleX = scale; scaleY = scale }, tint = if (value <= state.rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        Text(when (state.rating) {
                            1 -> cinemaText("Không thích", "Poor")
                            2 -> cinemaText("Chưa hay", "Fair")
                            3 -> cinemaText("Ổn", "Good")
                            4 -> cinemaText("Rất hay", "Very good")
                            5 -> cinemaText("Tuyệt vời", "Excellent")
                            else -> cinemaText("Chọn từ 1 đến 5 sao", "Choose 1 to 5 stars")
                        }, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(state.content, vm::content, Modifier.fillMaxWidth(), enabled = !state.saving,
                            label = { Text(cinemaText("Nhận xét (không bắt buộc)", "Review (optional)")) }, minLines = 5,
                            supportingText = { Text("${state.content.length} / 2000") }, shape = MaterialTheme.shapes.medium)
                        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = vm::loadEditor, enabled = !state.saving) { Text(cinemaText("Thử tải lại", "Retry loading")) } }
                        Button(onClick = vm::save, enabled = state.ready && state.rating in 1..5 && !state.saving, modifier = Modifier.fillMaxWidth()) {
                            if (state.saving) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary); Spacer(Modifier.width(10.dp)) }
                            Text(if (state.saving) cinemaText("Đang lưu…", "Saving…") else cinemaText("Lưu đánh giá", "Save review"))
                        }
                    }
                }
            }
        }
    }
}
