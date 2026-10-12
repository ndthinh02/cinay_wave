package com.cinaywave.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.cinaywave.app.data.*
import com.cinaywave.app.feature.AppViewModel
import com.cinaywave.app.playback.PlaybackManager
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.tween

@Composable fun WaveApp(repo: Repository, playback: PlaybackManager, pip: () -> Unit, fullscreen: (Boolean) -> Unit) {
    val language by repo.language.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val localized = remember(context, configuration, language) {
        val resources = context.createConfigurationContext(android.content.res.Configuration(configuration).apply { setLocale(java.util.Locale.forLanguageTag(language)) }).resources
        object : android.content.ContextWrapper(context) {
            override fun getResources() = resources
            override fun getAssets() = resources.assets
        }
    }
    CompositionLocalProvider(androidx.compose.ui.platform.LocalContext provides localized, androidx.compose.ui.platform.LocalConfiguration provides localized.resources.configuration) {
        WaveAppContent(repo, playback, pip, fullscreen)
    }
}

@Composable private fun WaveAppContent(repo: Repository, playback: PlaybackManager, pip: () -> Unit, fullscreen: (Boolean) -> Unit) {
    val uiText = androidx.compose.ui.platform.LocalContext.current.resources

    val vm: AppViewModel = viewModel(factory = object : ViewModelProvider.Factory { @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(repo) as T })
    val theme by vm.theme.collectAsStateWithLifecycle()
    val playing by playback.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var startingMovie by remember { mutableStateOf(false) }
    val message by vm.message.collectAsStateWithLifecycle()
    var pendingFavorite by remember { mutableStateOf<Movie?>(null) }
    var pendingReview by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTab by rememberSaveable { mutableStateOf("home") }
    fun details(movie: Movie) { nav.navigate("detail/${movie.slug}") }
    fun openPlayer() { nav.navigate("player") { launchSingleTop = true } }
    fun watch(movie: Movie) {
        if (startingMovie) return
        startingMovie = true
        scope.launch {
            try {
                val detail = repo.api.detail(movie.slug)
                val current = playback.state.value.current?.takeIf { it.detail.movie.slug == movie.slug }
                val progress = vm.history.value.data.orEmpty().firstOrNull { it.movie.slug == movie.slug && !it.completed }
                val preferredServer = current?.server ?: progress?.server
                val preferredEpisode = current?.episode?.slug ?: progress?.episode
                val preferred = detail.servers.firstOrNull { it.name == preferredServer && it.episodes.any { ep -> ep.slug == preferredEpisode && ep.url.isNotBlank() } }
                val server = preferred ?: detail.servers.firstOrNull { it.episodes.any { ep -> ep.url.isNotBlank() } }
                val episode = server?.episodes?.firstOrNull { it.slug == preferredEpisode && it.url.isNotBlank() } ?: server?.episodes?.firstOrNull { it.url.isNotBlank() }
                if (server != null && episode != null) { playback.start(detail, server.name, episode, progress); openPlayer() }
                else vm.message.value = uiText.getString(com.cinaywave.app.R.string.ui_this_movie_has_no_playable_episodes_yet_open_details_to_sele_166)
            } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { vm.message.value = errorMessage(e, vm.repo.language.value) }
            finally { startingMovie = false }
        }
    }
    fun favorite(movie: Movie) {
        if (repo.auth.value == null) { pendingFavorite = movie; nav.navigate("auth") }
        else vm.favorite(movie)
    }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.message.value = null } }
    LaunchedEffect(route) { if (route in setOf("home", "explore", "favorites", "profile")) selectedTab = route; if (route != "player" && playing.fullscreen) fullscreen(false); if (route == "home" && repo.auth.value != null) vm.refreshHistory() }
    BackHandler(playing.fullscreen) { fullscreen(false) }
    WaveTheme(theme) {
        val motion = MotionAllowed()
        val mainRoutes = setOf("home", "explore", "favorites", "profile")
        if (playing.pip) { VideoSurface(playback, Modifier.fillMaxSize()); return@WaveTheme }
        if (route == "player" && playing.fullscreen) { PlayerScreen(playback, { playback.save(); nav.popBackStack() }, pip, fullscreen); return@WaveTheme }
        val tabs = listOf("home" to uiText.getString(com.cinaywave.app.R.string.ui_home_167), "explore" to uiText.getString(com.cinaywave.app.R.string.ui_explore_64), "favorites" to uiText.getString(com.cinaywave.app.R.string.ui_favorites_0), "profile" to uiText.getString(com.cinaywave.app.R.string.ui_me_168))
        val icons = listOf(Icons.Rounded.Home, Icons.Rounded.Explore, Icons.Rounded.FavoriteBorder, Icons.Rounded.PersonOutline)
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                Column {
                    AnimatedVisibility(playing.current != null && route !in setOf("player", "auth", "profile/edit", "review/{slug}"),
                        enter = expandVertically(tween(if (motion) 250 else 0)) + fadeIn(tween(if (motion) 180 else 0)),
                        exit = shrinkVertically(tween(if (motion) 250 else 0)) + fadeOut(tween(if (motion) 180 else 0))) { MiniPlayer(playback, ::openPlayer) }
                    if (route !in setOf("auth", "profile/edit", "review/{slug}")) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        tabs.forEachIndexed { i, (tab, title) -> NavigationBarItem(selected = selectedTab == tab, onClick = {
                            selectedTab = tab
                            playback.save()
                            nav.navigate(tab) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
                        }, icon = { Icon(icons[i], title) }, alwaysShowLabel = false) }
                    }
                }
            }
        ) { padding ->
            NavHost(navController = nav, startDestination = "home", modifier = Modifier.padding(padding).consumeWindowInsets(padding).imePadding().fillMaxSize(),
                enterTransition = {
                    if (initialState.destination.route in mainRoutes && targetState.destination.route in mainRoutes) fadeIn(tween(if (motion) 150 else 0))
                    else fadeIn(tween(if (motion) 250 else 0)) + slideInHorizontally(tween(if (motion) 250 else 0)) { it / 14 }
                }, exitTransition = {
                    if (initialState.destination.route in mainRoutes && targetState.destination.route in mainRoutes) fadeOut(tween(if (motion) 150 else 0))
                    else fadeOut(tween(if (motion) 180 else 0)) + slideOutHorizontally(tween(if (motion) 250 else 0)) { -it / 14 }
                }, popEnterTransition = { fadeIn(tween(if (motion) 250 else 0)) + slideInHorizontally(tween(if (motion) 250 else 0)) { -it / 14 } },
                popExitTransition = { fadeOut(tween(if (motion) 180 else 0)) + slideOutHorizontally(tween(if (motion) 250 else 0)) { it / 14 } }) {
                composable("home") { HomeScreen(vm, ::details, { type -> vm.openList(type); nav.navigate("list/$type") }, { p -> playback.resume(p, ::openPlayer, { vm.message.value = it }) }, { nav.navigate("explore") }, route == "home", ::watch, ::details) }
                composable("explore") { ExploreScreen(vm, ::details) }
                composable("favorites") { FavoritesScreen(vm, ::details, { nav.navigate("auth") }, ::favorite) }
                composable("profile") { ProfileScreen(vm, { nav.navigate("auth") }, { nav.navigate("history") }, { nav.navigate("settings") }, { nav.navigate("profile/edit") }) }
                composable("profile/edit") { ProfileEditScreen(vm) { nav.popBackStack() } }
                composable("list/{type}") { ListScreen(vm, ::details) { nav.popBackStack() } }
                composable("detail/{slug}") { backStack ->
                    val slug = backStack.arguments?.getString("slug").orEmpty()
                    DetailScreen(slug, vm, playback, { nav.popBackStack() }, ::details, ::favorite, ::openPlayer) {
                        if (repo.auth.value == null) { pendingReview = slug; nav.navigate("auth") }
                        else nav.navigate("review/$slug")
                    }
                }
                composable("auth") { AuthScreen(vm, { pendingFavorite = null; pendingReview = null; nav.popBackStack() }) {
                    nav.popBackStack(); pendingFavorite?.let(vm::favorite); pendingFavorite = null
                    pendingReview?.let { slug -> nav.navigate("review/$slug") }; pendingReview = null
                } }
                composable("review/{slug}") { backStack ->
                    val slug = backStack.arguments?.getString("slug").orEmpty()
                    ReviewEditorScreen(slug, repo, { nav.popBackStack() }) { nav.navigate("auth") }
                }
                composable("history") { HistoryScreen(vm, { nav.popBackStack() }, { p -> playback.resume(p, ::openPlayer, { vm.message.value = it }) }, ::details) }
                composable("settings") { SettingsScreen(vm) { nav.popBackStack() } }
                composable("player") { PlayerScreen(playback, { playback.save(); nav.popBackStack() }, pip, fullscreen) }
            }
        }
        if (startingMovie) androidx.compose.ui.window.Dialog(onDismissRequest = {}) { Surface(shape = MaterialTheme.shapes.large) { Row(Modifier.padding(24.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(24.dp)); Spacer(Modifier.width(16.dp)); Text(uiText.getString(com.cinaywave.app.R.string.ui_opening_movie_169)) } } }
    }
}
