package com.cinaywave.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.cinaywave.app.R
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.animation.core.tween
import com.cinaywave.app.data.Movie

val Gold = Color(0xFFD7B47B)
private val LocalMotionAllowed = staticCompositionLocalOf<Boolean?> { null }
@Composable fun WaveTheme(mode: String, content: @Composable () -> Unit) {
    val motion = MotionAllowed()
    val dark = mode == "dark" || (mode == "system" && androidx.compose.foundation.isSystemInDarkTheme())
    val view = LocalView.current
    SideEffect {
        (view.context as? android.app.Activity)?.window?.let { window ->
            androidx.core.view.WindowCompat.getInsetsController(window, view).apply { isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark }
        }
    }
    val colors = if (dark) darkColorScheme(primary = Gold, onPrimary = Color(0xFF272014), secondary = Gold, tertiary = Gold, tertiaryContainer = Color(0xFF393126), onTertiaryContainer = Gold, primaryContainer = Color(0xFF393126), onPrimaryContainer = Gold, secondaryContainer = Color(0xFF393126), onSecondaryContainer = Gold, background = Color(0xFF101116), surface = Color(0xFF191B22), surfaceVariant = Color(0xFF252831), surfaceContainer = Color(0xFF191B22), surfaceContainerHigh = Color(0xFF20232B), surfaceContainerHighest = Color(0xFF252831), onSurface = Color(0xFFF1F0ED), onSurfaceVariant = Color(0xFFAEAEB7))
    else lightColorScheme(primary = Color(0xFF816031), secondary = Color(0xFF816031), tertiary = Color(0xFF816031), tertiaryContainer = Color(0xFFE9DDC9), onTertiaryContainer = Color(0xFF654A24), primaryContainer = Color(0xFFE9DDC9), onPrimaryContainer = Color(0xFF654A24), secondaryContainer = Color(0xFFE9DDC9), onSecondaryContainer = Color(0xFF654A24), background = Color(0xFFF9F7F3), surface = Color.White, surfaceVariant = Color(0xFFECE8E2), surfaceContainer = Color(0xFFF9F7F3), surfaceContainerHigh = Color(0xFFF2EEE7), surfaceContainerHighest = Color(0xFFECE8E2))
    val typography = Typography(
        headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.7).sp),
        headlineMedium = TextStyle(fontSize = 27.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
        headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 26.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
        bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
        labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
        labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
        labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    )
    CompositionLocalProvider(LocalMotionAllowed provides motion) {
        MaterialTheme(colorScheme = colors, typography = typography, shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp)), content = content)
    }
}

/** Coil skips crossfade for memory-cache hits; remembering the request keeps recomposition stable. */
@Composable
internal fun MovieArtwork(model: String, contentDescription: String?, modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop, error: Painter? = null) {
    val context = LocalContext.current
    val motion = MotionAllowed()
    val request = remember(model, context, motion) {
        ImageRequest.Builder(context).data(model).crossfade(if (motion) 180 else 0).build()
    }
    AsyncImage(request, contentDescription, modifier, contentScale = contentScale, error = error)
}
@Composable fun PageTitle(title: String, subtitle: String? = null, back: (() -> Unit)? = null, action: (@Composable () -> Unit)? = null) {
    val uiText = androidx.compose.ui.platform.LocalContext.current.resources

    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (back != null) IconButton(onClick = back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, uiText.getString(com.cinaywave.app.R.string.ui_back_84)) }
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        action?.invoke()
    }
}
@Composable fun Poster(movie: Movie, modifier: Modifier = Modifier.width(138.dp), open: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val motion = MotionAllowed()
    val scale by animateFloatAsState(if (pressed && motion) .98f else 1f, animationSpec = tween(if (motion) 120 else 0), label = "poster press")
    Column(modifier.graphicsLayer { scaleX = scale; scaleY = scale }.clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), onClick = open)) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            MovieArtwork(movie.poster, movie.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, error = painterResource(R.drawable.ic_wave))
            if (movie.quality.isNotBlank()) Surface(Modifier.align(Alignment.TopStart).padding(8.dp), color = Color.Black.copy(alpha = .65f), shape = RoundedCornerShape(4.dp)) { Text(movie.quality, Modifier.padding(horizontal = 6.dp, vertical = 3.dp), color = Color.White, style = MaterialTheme.typography.labelSmall) }
        }
        Text(movie.name, Modifier.fillMaxWidth().padding(top = 8.dp), minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
        Text(listOfNotNull(movie.year?.toString(), movie.episodeCurrent.takeIf { it.isNotBlank() }).joinToString(" · "), maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}
@Composable fun MovieRow(title: String, movies: List<Movie>, open: (Movie) -> Unit, all: (() -> Unit)? = null) {
    val uiText = androidx.compose.ui.platform.LocalContext.current.resources

    Column {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (all != null) TextButton(onClick = all, modifier = Modifier.heightIn(min = 48.dp), colors = ButtonDefaults.textButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Text(uiText.getString(com.cinaywave.app.R.string.ui_view_all_85)); Spacer(Modifier.width(4.dp)); Icon(Icons.Rounded.ChevronRight, null, Modifier.size(18.dp)) }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(movies, key = { it.slug }) { movie -> Poster(movie) { open(movie) } } }
    }
}
@Composable fun Feedback(loading: Boolean = false, error: String? = null, empty: String = androidx.compose.ui.res.stringResource(com.cinaywave.app.R.string.ui_no_content_yet_86), retry: (() -> Unit)? = null) {
    val uiText = androidx.compose.ui.platform.LocalContext.current.resources

    if (loading) { LoadingSkeleton(); return }
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (loading) { CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp); Text(uiText.getString(com.cinaywave.app.R.string.ui_loading_87), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        else { Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Icon(if (error != null) Icons.Rounded.WifiOff else Icons.Rounded.Movie, null, Modifier.padding(20.dp).size(36.dp), tint = MaterialTheme.colorScheme.primary) }; Text(if (error != null) uiText.getString(com.cinaywave.app.R.string.ui_connection_interrupted_88) else uiText.getString(com.cinaywave.app.R.string.ui_a_story_is_waiting_for_you_89), style = MaterialTheme.typography.titleMedium); Text(error ?: empty, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant); if (error != null && retry != null) OutlinedButton(onClick = retry) { Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(uiText.getString(com.cinaywave.app.R.string.ui_retry_90)) } }
    }
}
@Composable fun LoadingSkeleton() {
    val uiText = androidx.compose.ui.platform.LocalContext.current.resources

    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(uiText.getString(com.cinaywave.app.R.string.ui_preparing_content_91), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { repeat(3) { Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) { Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant)); Box(Modifier.fillMaxWidth(.8f).height(14.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) } } }
    }
}
@Composable fun MotionAllowed(): Boolean {
    LocalMotionAllowed.current?.let { return it }
    val context = LocalContext.current
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f) }
    DisposableEffect(context, lifecycle) {
        val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { enabled = android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
        }
        context.contentResolver.registerContentObserver(android.provider.Settings.Global.getUriFor(android.provider.Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return enabled
}
@Composable fun CinematicBar(title: String, visible: Boolean, back: (() -> Unit)? = null, search: (() -> Unit)? = null) {
    val uiText = androidx.compose.ui.platform.LocalContext.current.resources

    val color by animateColorAsState(if (visible) MaterialTheme.colorScheme.background.copy(alpha = .97f) else Color.Transparent, label = "appbar")
    Surface(color = color) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (back != null) FilledTonalIconButton(onClick = back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, uiText.getString(com.cinaywave.app.R.string.ui_back_84)) }
            androidx.compose.animation.AnimatedVisibility(visible, Modifier.weight(1f)) { Text(title, Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            if (!visible) Spacer(Modifier.weight(1f))
            if (search != null) FilledTonalIconButton(onClick = search) { Icon(Icons.Rounded.Search, uiText.getString(com.cinaywave.app.R.string.ui_search_movies_62)) }
        }
    }
}
@Composable fun Hero(movie: Movie, open: () -> Unit) {
    val uiText = androidx.compose.ui.platform.LocalContext.current.resources

    Box(Modifier.fillMaxWidth().height(430.dp).padding(horizontal = 20.dp).clip(RoundedCornerShape(20.dp)).clickable(onClick = open)) {
        AsyncImage(movie.backdrop, movie.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF101116).copy(alpha = .45f), Color(0xFF101116)))))
        Column(Modifier.align(Alignment.BottomStart).padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(uiText.getString(com.cinaywave.app.R.string.ui_recently_updated_92), style = MaterialTheme.typography.labelMedium, color = Gold)
            Text(movie.name, color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(movie.year?.toString(), movie.quality.takeIf(String::isNotBlank), movie.language.takeIf(String::isNotBlank)).joinToString(" · "), color = Color(0xFFCECED2), style = MaterialTheme.typography.bodySmall)
            Button(onClick = open, shape = RoundedCornerShape(10.dp)) { Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text(uiText.getString(com.cinaywave.app.R.string.ui_explore_movie_93)) }
        }
    }
}
