package com.cinaywave.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import coil.compose.AsyncImage
import com.cinaywave.app.data.Movie
import kotlinx.coroutines.delay

@Composable fun BannerSlider(movies: List<Movie>, active: Boolean, scrolling: Boolean, open: (Movie) -> Unit, watch: (Movie) -> Unit, details: (Movie) -> Unit = open) {
    val uiText = androidx.compose.ui.platform.LocalContext.current.resources

    if (movies.isEmpty()) return
    val pager = rememberPagerState(pageCount = { movies.size })
    val dragging by pager.interactionSource.collectIsDraggedAsState()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val motion = MotionAllowed()
    LaunchedEffect(active, lifecycle, dragging, scrolling, pager.isScrollInProgress, pager.currentPage, movies.size, motion) {
        if (active && lifecycle == Lifecycle.State.RESUMED && !dragging && !scrolling && !pager.isScrollInProgress && motion && movies.size > 1) {
            delay(7000)
            pager.animateScrollToPage((pager.currentPage + 1) % movies.size)
        }
    }
    Column {
        HorizontalPager(pager, contentPadding = PaddingValues(horizontal = 20.dp), pageSpacing = 12.dp, modifier = Modifier.fillMaxWidth()) { index ->
            val movie = movies[index]
            Box(Modifier.fillMaxWidth().heightIn(min = 430.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { open(movie) }) {
                MovieArtwork(movie.backdrop.ifBlank { movie.poster }, movie.name, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .08f), Color.Black.copy(alpha = .3f), Color.Black.copy(alpha = .94f)))))
                Column(Modifier.align(Alignment.BottomStart).padding(top = 180.dp, start = 20.dp, end = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(uiText.getString(com.cinaywave.app.R.string.ui_today_s_spotlight_59), style = MaterialTheme.typography.labelMedium, color = Gold)
                    Text(movie.name, color = Color.White, style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(listOfNotNull(movie.year?.toString(), movie.quality.takeIf(String::isNotBlank), movie.language.takeIf(String::isNotBlank)).joinToString("  ·  "), color = Color(0xFFE0DED8), style = MaterialTheme.typography.bodyMedium)
                    FlowRowCompat(movie, details, watch)
                }
            }
        }
        if (movies.size > 1) Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
            repeat(movies.size) { index -> Box(Modifier.width(if (index == pager.currentPage) 24.dp else 6.dp).height(6.dp).clip(CircleShape).background(if (index == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) }
        }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun FlowRowCompat(movie: Movie, open: (Movie) -> Unit, watch: (Movie) -> Unit) {
    val uiText = androidx.compose.ui.platform.LocalContext.current.resources

    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = { watch(movie) }, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF272014))) { Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Xem ngay") }
        OutlinedButton(onClick = { open(movie) }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Text(uiText.getString(com.cinaywave.app.R.string.ui_details_60)) }
    }
}
