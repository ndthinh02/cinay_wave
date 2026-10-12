package com.cinaywave.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinaywave.app.data.*
import com.cinaywave.app.feature.*

@Composable fun HomeScreen(vm: AppViewModel, open: (Movie) -> Unit, all: (String) -> Unit, resume: (Progress) -> Unit, search: () -> Unit, active: Boolean = true, watch: (Movie) -> Unit = open, details: (Movie) -> Unit = open) {
    val uiText = androidx.compose.ui.platform.LocalContext.current.resources

    val state by vm.home.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val scroll = rememberLazyListState()
    val collapsed by remember { derivedStateOf { scroll.firstVisibleItemIndex > 0 || scroll.firstVisibleItemScrollOffset > 48 } }
    Box {
    LazyColumn(state = scroll, contentPadding = PaddingValues(bottom = 24.dp)) {
        item { PageTitle("CinayWave", uiText.getString(com.cinaywave.app.R.string.ui_a_world_of_cinema_at_your_fingertips_61), action = { IconButton(onClick = search) { Icon(Icons.Rounded.Search, uiText.getString(com.cinaywave.app.R.string.ui_search_movies_62)) } }) }
        if (state.data == null) item { Feedback(state.loading, state.error, retry = vm::loadHome) }
        state.data?.let { home ->
            item {
                val banners = remember(home) { (listOfNotNull(home.featured) + home.sections.flatMap { it.items.take(3) }).distinctBy { it.slug }.take(6) }
                BannerSlider(banners, active, scroll.isScrollInProgress, open, watch, details)
            }
            if (history.data.orEmpty().any { !it.completed }) item {
                Column {
                    PageTitle(uiText.getString(com.cinaywave.app.R.string.ui_continue_watching_63))
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(history.data.orEmpty().filterNot { it.completed }, key = { it.movie.slug }) { p ->
                            ContinueWatchingCard(p) { resume(p) }
                        }
                    }
                }
            }
            items(home.sections, key = { it.slug }) { section ->
                if (section.error != null) { PageTitle(section.name); Feedback(error = section.error, retry = vm::loadHome) }
                else MovieRow(section.name, section.items, open) { all(section.slug) }
            }
        }
    }
    androidx.compose.animation.AnimatedVisibility(collapsed) { CinematicBar("CinayWave", true, search = search) }
    }
}
@Composable
private fun ContinueWatchingCard(progress: Progress, resume: () -> Unit) {
    val resources = androidx.compose.ui.platform.LocalContext.current.resources
    Card(
        onClick = resume,
        modifier = Modifier.width(260.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Movie, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            MovieArtwork(
                model = progress.movie.backdrop.ifBlank { progress.movie.poster },
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Surface(shape = androidx.compose.foundation.shape.CircleShape,
                color = androidx.compose.ui.graphics.Color.Black.copy(alpha = .55f),
                contentColor = androidx.compose.ui.graphics.Color.White) {
                Icon(Icons.Rounded.PlayArrow, null, Modifier.padding(10.dp).size(24.dp))
            }
        }
        LinearProgressIndicator(
            progress = { if (progress.durationMs > 0) (progress.positionMs.toFloat() / progress.durationMs).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = Gold,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Butt,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(progress.movie.name, style = MaterialTheme.typography.titleSmall, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(resources.getString(com.cinaywave.app.R.string.episode_label, progress.episodeName),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
@Composable fun SelectMenu(label: String, selected: String, choices: List<Label>, onSelect: (String) -> Unit) {
    val uiText = androidx.compose.ui.platform.LocalContext.current.resources
    val motion = MotionAllowed()
    val selectedColor by androidx.compose.animation.animateColorAsState(if (selected.isNotBlank()) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface, androidx.compose.animation.core.tween(if (motion) 160 else 0), label = "filter selection")
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, colors = ButtonDefaults.outlinedButtonColors(containerColor = selectedColor), modifier = Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 12.dp)) { Text(choices.find { it.slug == selected }?.name ?: label, maxLines = 1); Icon(Icons.Rounded.ExpandMore, null, Modifier.size(18.dp)) }
        DropdownMenu(expanded, { expanded = false }, modifier = Modifier.heightIn(max = 360.dp)) {
            DropdownMenuItem(text = { Text(uiText.getString(com.cinaywave.app.R.string.all_filter_label, label)) }, onClick = { onSelect(""); expanded = false })
            choices.forEach { item -> DropdownMenuItem(text = { Text(item.name) }, onClick = { onSelect(item.slug); expanded = false }) }
        }
    }
}
@Composable fun ExploreScreen(vm: AppViewModel, open: (Movie) -> Unit) {
    val uiText = androidx.compose.ui.platform.LocalContext.current.resources

    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val filters by vm.filters.collectAsStateWithLifecycle()
    val searches by vm.repo.searchHistory.collectAsStateWithLifecycle()
    val q = catalog.query
    var showFilters by rememberSaveable { mutableStateOf(true) }
    Column {
        PageTitle(uiText.getString(com.cinaywave.app.R.string.ui_explore_64), uiText.getString(com.cinaywave.app.R.string.ui_find_your_next_story_65), action = { IconButton(onClick = { showFilters = !showFilters }) { Icon(Icons.Rounded.Tune, if (showFilters) uiText.getString(com.cinaywave.app.R.string.ui_collapse_filters_66) else uiText.getString(com.cinaywave.app.R.string.ui_show_filters_67), tint = if (q.category.isNotBlank() || q.country.isNotBlank() || q.year.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) } })
        OutlinedTextField((q.keyword as Any?)?.toString() ?: "", { vm.search(q.copy(keyword = it, type = if (it.isNotBlank()) "phim-moi" else q.type)) }, Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = MaterialTheme.shapes.medium, singleLine = true, placeholder = { Text(uiText.getString(com.cinaywave.app.R.string.ui_movie_title_original_title_68)) }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, trailingIcon = { if (q.keyword.orEmpty().isNotEmpty()) IconButton(onClick = { vm.search(q.copy(keyword = "")) }) { Icon(Icons.Rounded.Close, uiText.getString(com.cinaywave.app.R.string.ui_clear_search_term_69)) } })
        filters.data?.let { f ->
            androidx.compose.animation.AnimatedVisibility(showFilters) {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (q.keyword.isBlank()) item { SelectMenu(uiText.getString(com.cinaywave.app.R.string.ui_type_70), q.type, f.types) { vm.search(q.copy(type = it.ifBlank { "phim-moi" })) } }
                item { SelectMenu(uiText.getString(com.cinaywave.app.R.string.ui_genre_71), q.category, f.categories) { vm.search(q.copy(category = it)) } }
                item { SelectMenu(uiText.getString(com.cinaywave.app.R.string.ui_country_72), q.country, f.countries) { vm.search(q.copy(country = it)) } }
                item { SelectMenu(uiText.getString(com.cinaywave.app.R.string.ui_year_73), q.year, f.years.map { Label(it.toString(), it.toString()) }) { vm.search(q.copy(year = it)) } }
            }
            }
        }
        if (filters.error != null) TextButton(onClick = vm::loadFilters, modifier = Modifier.padding(horizontal = 20.dp)) { Text(uiText.getString(com.cinaywave.app.R.string.ui_could_not_load_filters_retry_74)) }
        if (q.keyword.isBlank() && searches.isNotEmpty()) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) { Text(uiText.getString(com.cinaywave.app.R.string.ui_recent_searches_75), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge); TextButton(onClick = { vm.repo.removeSearch() }) { Text(uiText.getString(com.cinaywave.app.R.string.ui_clear_all_8)) } }
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(searches, key = { it }) { value -> InputChip(selected = false, onClick = { vm.search(q.copy(keyword = value, type = "phim-moi")) }, label = { Text(value) }, trailingIcon = { Icon(Icons.Rounded.Close, uiText.getString(com.cinaywave.app.R.string.ui_remove_search_76), Modifier.size(48.dp).clickable { vm.repo.removeSearch(value) }.padding(14.dp)) }) } }
        }
        MovieGrid(catalog, open, { vm.more() }, { vm.search(q) }, Modifier.weight(1f))
    }
}
@Composable fun MovieGrid(state: CatalogState, open: (Movie) -> Unit, more: () -> Unit, retry: () -> Unit, modifier: Modifier = Modifier) {
    val uiText = androidx.compose.ui.platform.LocalContext.current.resources

    val grid = rememberLazyGridState()
    var lastQuery by rememberSaveable { mutableStateOf(state.query.toString()) }
    LaunchedEffect(state.query) { if (lastQuery != state.query.toString()) { grid.scrollToItem(0); lastQuery = state.query.toString() } }
    if (state.items.isEmpty()) { Box(modifier.fillMaxWidth().verticalScroll(rememberScrollState())) { Feedback(state.loading, state.error, uiText.getString(com.cinaywave.app.R.string.ui_no_matching_movies_found_77), retry) }; return }
    Box(modifier) {
    LazyVerticalGrid(columns = GridCells.Adaptive(140.dp), state = grid, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        items(state.items, key = { it.slug }) { movie -> Poster(movie, Modifier.fillMaxWidth()) { open(movie) } }
        item(span = { GridItemSpan(maxLineSpan) }) {
            if (state.loading && state.page > 0) CircularProgressIndicator(Modifier.padding(20.dp).size(28.dp))
            else if (state.error != null) Feedback(error = state.error, retry = if (state.page == 0) retry else more)
            else if (state.hasMore) TextButton(onClick = more, modifier = Modifier.fillMaxWidth()) { Text(uiText.getString(com.cinaywave.app.R.string.ui_load_more_movies_78)) }
        }
    }
    if (state.loading && state.page == 0) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
    }
    val nearEnd by remember(grid, state.items.size) { derivedStateOf { (grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= state.items.size - 6 } }
    LaunchedEffect(nearEnd, state.page, state.loading) { if (nearEnd && !state.loading && state.error == null && state.hasMore) more() }
}
@Composable fun ListScreen(vm: AppViewModel, open: (Movie) -> Unit, back: () -> Unit) {
    val uiText = androidx.compose.ui.platform.LocalContext.current.resources

    val state by vm.lists.collectAsStateWithLifecycle()
    val names = mapOf("phim-moi" to uiText.getString(com.cinaywave.app.R.string.ui_new_releases_79), "phim-bo" to uiText.getString(com.cinaywave.app.R.string.ui_series_80), "phim-le" to uiText.getString(com.cinaywave.app.R.string.ui_movies_81), "hoat-hinh" to uiText.getString(com.cinaywave.app.R.string.ui_animation_82), "tv-shows" to "TV Show")
    Column { PageTitle(names[state.query.type] ?: uiText.getString(com.cinaywave.app.R.string.ui_movie_list_83), back = back); MovieGrid(state, open, { vm.more(true) }, { vm.openList(state.query.type) }, Modifier.weight(1f)) }
}
