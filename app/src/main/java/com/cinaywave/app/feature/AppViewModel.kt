package com.cinaywave.app.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinaywave.app.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Load<T>(val data: T? = null, val loading: Boolean = false, val error: String? = null)
data class ProfileEditor(val user: User? = null, val name: String = "", val avatar: String? = null, val loading: Boolean = false, val saving: Boolean = false, val error: String? = null) {
    val canSave get() = user != null && name.trim().length in 2..80 && (name.trim() != user.name || avatar != null) && !saving && !loading
}
data class CatalogState(val query: Query = Query(), val items: List<Movie> = emptyList(), val page: Int = 0, val hasMore: Boolean = true, val loading: Boolean = false, val error: String? = null)
class AppViewModel(val repo: Repository) : ViewModel() {
    val auth = repo.auth
    val theme = repo.theme
    private val _home = MutableStateFlow(Load<Home>()); val home = _home.asStateFlow()
    private val _filters = MutableStateFlow(Load<Filters>()); val filters = _filters.asStateFlow()
    private val _catalog = MutableStateFlow(CatalogState()); val catalog = _catalog.asStateFlow()
    private val _lists = MutableStateFlow(CatalogState()); val lists = _lists.asStateFlow()
    private val _favorites = MutableStateFlow(Load<List<Movie>>()); val favorites = _favorites.asStateFlow()
    private val _history = MutableStateFlow(Load<List<Progress>>()); val history = _history.asStateFlow()
    val message = MutableStateFlow<String?>(null)
    val authBusy = MutableStateFlow(false)
    private val _profile = MutableStateFlow(ProfileEditor()); val profile = _profile.asStateFlow()
    private var profileOwner: String? = null
    private var profileJob: Job? = null
    private fun syncProfileOwner() {
        val owner = auth.value?.user?.id
        if (owner != profileOwner) { profileJob?.cancel(); profileOwner = owner; _profile.value = ProfileEditor() }
    }
    fun loadProfile() {
        syncProfileOwner()
        val owner = profileOwner ?: return
        if (_profile.value.loading || _profile.value.saving || _profile.value.user != null) return
        _profile.value = _profile.value.copy(loading = true, error = null)
        profileJob = viewModelScope.launch {
            try { val user = repo.loadProfile(); if (auth.value?.user?.id == owner) _profile.value = ProfileEditor(user, user.name) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (auth.value?.user?.id == owner) _profile.value = _profile.value.copy(loading = false, error = errorMessage(e, repo.language.value)) }
        }
    }
    fun editProfileName(name: String) { syncProfileOwner(); if (!_profile.value.saving) _profile.value = _profile.value.copy(name = name, error = null) }
    fun editProfileAvatar(uri: String?, owner: String?) { syncProfileOwner(); if (owner == profileOwner && !_profile.value.saving) _profile.value = _profile.value.copy(avatar = uri, error = null) }
    fun saveProfile() {
        syncProfileOwner()
        val draft = _profile.value; val owner = profileOwner ?: return
        if (!draft.canSave) return
        _profile.value = draft.copy(saving = true, error = null)
        profileJob = viewModelScope.launch {
            try {
                val user = repo.saveProfile(draft.name.trim(), draft.avatar?.let(android.net.Uri::parse))
                if (auth.value?.user?.id == owner) { _profile.value = ProfileEditor(user, user.name); message.value = repo.text(com.cinaywave.app.R.string.profile_saved) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (auth.value?.user?.id == owner) _profile.value = draft.copy(error = errorMessage(e, repo.language.value)) }
        }
    }
    private var searchJob: Job? = null
    private var listJob: Job? = null
    private var searchGeneration = 0
    private var listGeneration = 0
    init {
        loadHome(); loadFilters(); search(Query())
        viewModelScope.launch { auth.collect { syncProfileOwner(); _favorites.value = Load(); _history.value = Load(); if (it != null) refreshLibrary() } }
    }
    fun loadHome() { if (_home.value.loading) return; viewModelScope.launch { _home.value = _home.value.copy(loading = true, error = null); try { _home.value = Load(repo.api.home()) } catch (e: Exception) { _home.value = Load(error = errorMessage(e, repo.language.value)) } } }
    fun loadFilters() { if (_filters.value.loading) return; viewModelScope.launch { _filters.value = Load(loading = true); try { _filters.value = Load(repo.api.filters()) } catch (e: Exception) { _filters.value = Load(error = errorMessage(e, repo.language.value)) } } }
    fun search(query: Query) {
        val generation = ++searchGeneration; searchJob?.cancel()
        _catalog.value = _catalog.value.copy(query = query, page = 0, hasMore = false, loading = true, error = null)
        searchJob = viewModelScope.launch {
            delay(350)
            try { val page = repo.movies(query, 1); if (generation == searchGeneration) { _catalog.value = CatalogState(query, page.items, page.page, page.hasMore); repo.rememberSearch(query.keyword) } }
            catch (e: CancellationException) { throw e } catch (e: Exception) { if (generation == searchGeneration) _catalog.value = _catalog.value.copy(loading = false, error = errorMessage(e, repo.language.value)) }
        }
    }
    fun openList(type: String) {
        val generation = ++listGeneration; listJob?.cancel(); val q = Query(type = type); _lists.value = CatalogState(query = q, loading = true)
        listJob = viewModelScope.launch { try { val p = repo.movies(q, 1); if (generation == listGeneration) _lists.value = CatalogState(q, p.items, p.page, p.hasMore) } catch (e: CancellationException) { throw e } catch (e: Exception) { _lists.value = _lists.value.copy(loading = false, error = errorMessage(e, repo.language.value)) } }
    }
    fun more(list: Boolean = false) {
        val flow = if (list) _lists else _catalog; val state = flow.value
        if (state.loading || !state.hasMore) return
        val generation = if (list) listGeneration else searchGeneration
        flow.value = state.copy(loading = true, error = null)
        val job = viewModelScope.launch {
            try {
                val p = repo.movies(state.query, state.page + 1)
                if (generation == (if (list) listGeneration else searchGeneration)) flow.value = state.copy(items = (state.items + p.items).distinctBy { it.slug }, page = p.page, hasMore = p.hasMore, loading = false)
            } catch (e: CancellationException) { throw e } catch (e: Exception) { if (generation == (if (list) listGeneration else searchGeneration)) flow.value = state.copy(loading = false, error = errorMessage(e, repo.language.value)) }
        }
        if (list) listJob = job else searchJob = job
    }
    fun refreshLibrary() {
        val owner = auth.value?.user?.id ?: return
        viewModelScope.launch {
            _favorites.value = _favorites.value.copy(loading = true, error = null)
            try { val items = repo.favorites(); if (owner == auth.value?.user?.id) _favorites.value = Load(items) } catch (e: Exception) { if (owner == auth.value?.user?.id) _favorites.value = Load(error = errorMessage(e, repo.language.value)) }
        }
        refreshHistory()
    }
    fun refreshHistory() {
        val owner = auth.value?.user?.id ?: return
        viewModelScope.launch {
            _history.value = _history.value.copy(loading = true, error = null)
            try { val items = repo.history(); if (owner == auth.value?.user?.id) _history.value = Load(items) } catch (e: Exception) { if (owner == auth.value?.user?.id) _history.value = Load(error = errorMessage(e, repo.language.value)) }
        }
    }
    fun favorite(movie: Movie) {
        if (auth.value == null) return
        viewModelScope.launch { try { val removing = _favorites.value.data.orEmpty().any { it.slug == movie.slug }; if (removing) repo.api.unfavorite(movie.slug) else repo.api.favorite(movie.slug); message.value = if (removing) repo.text(com.cinaywave.app.R.string.favorite_removed) else repo.text(com.cinaywave.app.R.string.favorite_saved); refreshLibrary() } catch (e: Exception) { message.value = errorMessage(e, repo.language.value) } }
    }
    fun deleteHistory(slug: String? = null) { viewModelScope.launch { try { repo.deleteHistory(slug); refreshHistory() } catch (e: Exception) { message.value = errorMessage(e, repo.language.value) } } }
    fun signIn(email: String, password: String, name: String?, done: () -> Unit) {
        if (authBusy.value) return
        viewModelScope.launch { authBusy.value = true; try { repo.login(email, password, name); done() } catch (e: Exception) { message.value = errorMessage(e, repo.language.value) } finally { authBusy.value = false } }
    }
    fun logout() { viewModelScope.launch { try { repo.logout() } catch (e: Exception) { message.value = errorMessage(e, repo.language.value) } } }
    fun updateName(name: String) { viewModelScope.launch { try { repo.updateName(name); message.value = repo.text(com.cinaywave.app.R.string.profile_updated) } catch (e: Exception) { message.value = errorMessage(e, repo.language.value) } } }
}
