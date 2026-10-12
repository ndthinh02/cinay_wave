package com.cinaywave.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cinaywave.app.data.*
import com.cinaywave.app.feature.AppViewModel

@Composable
fun FavoritesScreen(vm: AppViewModel, open: (Movie) -> Unit, login: () -> Unit, toggle: (Movie) -> Unit) {
    val uiText = androidx.compose.ui.platform.LocalContext.current.resources

    val motion = MotionAllowed()
    val auth by vm.auth.collectAsStateWithLifecycle()
    val state by vm.favorites.collectAsStateWithLifecycle()
    Column {
        PageTitle(
            uiText.getString(com.cinaywave.app.R.string.ui_favorites_0),
            uiText.getString(com.cinaywave.app.R.string.ui_your_personal_movie_collection_1),
            action = {
                if (auth != null) IconButton(onClick = vm::refreshLibrary) {
                    Icon(
                        Icons.Rounded.Refresh,
                        uiText.getString(com.cinaywave.app.R.string.ui_refresh_2)
                    )
                }
            })
        if (auth == null) {
            Feedback(empty = uiText.getString(com.cinaywave.app.R.string.ui_sign_in_to_sync_your_favorites_3)); Button(
                onClick = login,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text(uiText.getString(com.cinaywave.app.R.string.ui_sign_in_4)) }
        } else if (state.data.isNullOrEmpty()) Feedback(
            state.loading,
            state.error,
            uiText.getString(com.cinaywave.app.R.string.ui_no_favorites_yet_add_movies_from_their_detail_page_5),
            vm::refreshLibrary
        )
        else LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.data.orEmpty(), key = { it.slug }) { m ->
                Surface(modifier = Modifier.animateItem(fadeInSpec = tween(if (motion) 150 else 0), placementSpec = tween(if (motion) 220 else 0), fadeOutSpec = tween(if (motion) 150 else 0)), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    ListItem(
                        headlineContent = { Text(m.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        supportingContent = {
                            Text(
                                listOfNotNull(
                                    m.year?.toString(),
                                    m.quality.takeIf(String::isNotBlank)
                                ).joinToString(" · ")
                            )
                        },
                        leadingContent = {
                            MovieArtwork(
                                m.poster,
                                m.name,
                                Modifier
                                    .width(60.dp)
                                    .height(90.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { toggle(m) }) {
                                Icon(
                                    Icons.Rounded.Favorite,
                                    uiText.getString(com.cinaywave.app.R.string.ui_remove_from_favorites_6),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { open(m) })
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(vm: AppViewModel, back: () -> Unit, resume: (Progress) -> Unit, open: (Movie) -> Unit) {
    val uiText = androidx.compose.ui.platform.LocalContext.current.resources

    val motion = MotionAllowed()
    val state by vm.history.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { vm.refreshHistory() }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            PageTitle(
                uiText.getString(com.cinaywave.app.R.string.ui_watch_history_7),
                back = back,
                action = {
                    if (!state.data.isNullOrEmpty()) IconButton(onClick = {
                        deleting = "*"
                    }) { Icon(Icons.Rounded.DeleteSweep, uiText.getString(com.cinaywave.app.R.string.ui_clear_all_8)) }
                })
        }
        if (state.data.isNullOrEmpty()) item {
            Feedback(
                state.loading,
                state.error,
                uiText.getString(com.cinaywave.app.R.string.ui_you_have_no_watch_history_yet_9),
                vm::refreshHistory
            )
        }
        items(state.data.orEmpty(), key = { it.movie.slug }) { p ->
            Surface(
                Modifier.animateItem(fadeInSpec = tween(if (motion) 150 else 0), placementSpec = tween(if (motion) 220 else 0), fadeOutSpec = tween(if (motion) 150 else 0)).padding(horizontal = 20.dp, vertical = 6.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                ListItem(
                    headlineContent = { Text(p.movie.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                uiText.getString(
                                    com.cinaywave.app.R.string.episode_label,
                                    p.episodeName
                                ) + " · " + (if (p.completed) uiText.getString(com.cinaywave.app.R.string.ui_watched_149) else timeLabel(
                                    p.positionMs
                                ))
                            ); LinearProgressIndicator(progress = {
                            if (p.durationMs > 0) (p.positionMs.toFloat() / p.durationMs).coerceIn(
                                0f,
                                1f
                            ) else 0f
                        }, modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp))
                        }
                    },
                    leadingContent = {
                        MovieArtwork(
                            p.movie.poster,
                            p.movie.name,
                            Modifier
                                .width(60.dp)
                                .height(90.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = {
                            deleting = p.movie.slug
                        }) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                uiText.getString(com.cinaywave.app.R.string.ui_remove_from_history_10)
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { if (p.completed) open(p.movie) else resume(p) })
            }
        }
    }
    deleting?.let { slug ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = {
                Text(
                    if (slug == "*") uiText.getString(com.cinaywave.app.R.string.ui_clear_all_watch_history_11) else uiText.getString(
                        com.cinaywave.app.R.string.ui_remove_this_movie_from_history_12
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteHistory(slug.takeUnless { it == "*" }); deleting = null
                }) { Text(uiText.getString(com.cinaywave.app.R.string.ui_delete_13)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    deleting = null
                }) { Text(uiText.getString(com.cinaywave.app.R.string.ui_cancel_14)) }
            })
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel, back: () -> Unit) {
    val uiText = androidx.compose.ui.platform.LocalContext.current.resources

    val motion = MotionAllowed()
    val theme by vm.theme.collectAsStateWithLifecycle()
    val language by vm.repo.language.collectAsStateWithLifecycle()
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            PageTitle(
                uiText.getString(com.cinaywave.app.R.string.ui_settings_15),
                uiText.getString(com.cinaywave.app.R.string.ui_make_the_experience_your_own_16),
                back = back
            ); Text(
            uiText.getString(com.cinaywave.app.R.string.ui_language_17),
            Modifier.padding(20.dp),
            style = MaterialTheme.typography.titleLarge
        )
        }
        items(
            listOf(
                "vi" to uiText.getString(com.cinaywave.app.R.string.ui_ti_ng_vi_t_18),
                "en" to "English"
            ), key = { it.first }
        ) { (value, label) ->
            val selected = language == value
            val selectionColor by animateColorAsState(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, tween(if (motion) 160 else 0), label = "language selection")
            Card(
                onClick = { vm.repo.setLanguage(value) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = selectionColor)
            ) {
                Row(Modifier
                    .fillMaxWidth()
                    .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Language, null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        label,
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                    RadioButton(selected = language == value, onClick = null)
                }
            }
        }
        item {
            Text(
                uiText.getString(com.cinaywave.app.R.string.ui_appearance_19),
                Modifier.padding(20.dp),
                style = MaterialTheme.typography.titleLarge
            )
        }
        items(
            listOf(
                "dark" to uiText.getString(com.cinaywave.app.R.string.ui_cinema_dark_20),
                "light" to uiText.getString(com.cinaywave.app.R.string.ui_daylight_light_21),
                "system" to uiText.getString(com.cinaywave.app.R.string.ui_follow_device_22)
            ), key = { it.first }
        ) { (value, label) ->
            val selected = theme == value
            val selectionColor by animateColorAsState(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, tween(if (motion) 160 else 0), label = "theme selection")
            Card(
                onClick = { vm.repo.setTheme(value) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = selectionColor)
            ) {
                Row(Modifier
                    .fillMaxWidth()
                    .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (value) {
                            "dark" -> Icons.Rounded.DarkMode; "light" -> Icons.Rounded.LightMode; else -> Icons.Rounded.SettingsBrightness
                        }, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary
                    )
                    Column(Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)) {
                        Text(
                            label,
                            style = MaterialTheme.typography.titleMedium
                        ); Text(
                        when (value) {
                            "dark" -> uiText.getString(com.cinaywave.app.R.string.ui_dark_background_with_warm_gold_accents_23); "light" -> uiText.getString(
                            com.cinaywave.app.R.string.ui_light_background_with_clear_contrast_24
                        ); else -> uiText.getString(com.cinaywave.app.R.string.ui_match_your_device_appearance_25)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    }
                    RadioButton(selected = theme == value, onClick = null)
                }
            }
        }
        item {
            Text(
                uiText.getString(com.cinaywave.app.R.string.ui_animations_follow_your_device_settings_26),
                Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

