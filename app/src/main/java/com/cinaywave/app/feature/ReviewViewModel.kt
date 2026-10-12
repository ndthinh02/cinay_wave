package com.cinaywave.app.feature

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinaywave.app.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReviewsState(
    val average: Double = 0.0, val total: Int = 0, val items: List<FilmReview> = emptyList(),
    val mine: FilmReview? = null, val cursor: String? = null, val loading: Boolean = true,
    val moreLoading: Boolean = false, val error: String? = null, val moreError: String? = null,
    val ownError: String? = null, val deleting: Boolean = false
)
data class ReviewEditorState(val rating: Int = 0, val content: String = "", val loading: Boolean = true,
    val saving: Boolean = false, val error: String? = null, val saved: Boolean = false, val existing: Boolean = false,
    val ready: Boolean = false)

class ReviewViewModel(private val repo: Repository, private val slug: String, private val savedState: SavedStateHandle) : ViewModel() {
    private val _movie = MutableStateFlow<Movie?>(null); val movie = _movie.asStateFlow()
    private val _movieError = MutableStateFlow<String?>(null); val movieError = _movieError.asStateFlow()
    private var movieJob: Job? = null
    private val _reviews = MutableStateFlow(ReviewsState()); val reviews = _reviews.asStateFlow()
    private val _editor = MutableStateFlow(ReviewEditorState()); val editor = _editor.asStateFlow()
    private var loadJob: Job? = null
    private var moreJob: Job? = null
    private var writeJob: Job? = null
    private var owner: String? = repo.auth.value?.user?.id
    private var editorStarted = false
    init {
        viewModelScope.launch {
            repo.auth.collect { auth ->
                if (owner != auth?.user?.id) {
                    owner = auth?.user?.id
                    writeJob?.cancel(); loadJob?.cancel(); moreJob?.cancel()
                    savedState.remove<Int>("rating"); savedState.remove<String>("content")
                    _editor.value = ReviewEditorState()
                    _reviews.value = ReviewsState()
                    refresh()
                    if (editorStarted) loadEditor()
                }
            }
        }
    }
    fun refresh() {
        if (_reviews.value.deleting) return
        loadJob?.cancel(); moreJob?.cancel()
        _reviews.value = _reviews.value.copy(loading = true, moreLoading = false, error = null, moreError = null, ownError = null)
        loadJob = viewModelScope.launch {
            try {
                val page = repo.reviews(slug)
                _reviews.value = ReviewsState(page.average, page.total, page.items.distinctBy { it.id }, cursor = page.nextCursor, loading = false)
                if (repo.auth.value != null) {
                    try { _reviews.value = _reviews.value.copy(mine = repo.ownReview(slug)) }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { _reviews.value = _reviews.value.copy(ownError = errorMessage(e, repo.language.value)) }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _reviews.value = _reviews.value.copy(loading = false, error = errorMessage(e, repo.language.value)) }
        }
    }
    fun more() {
        val state = _reviews.value
        val cursor = state.cursor ?: return
        if (state.loading || state.moreLoading || state.deleting) return
        _reviews.value = state.copy(moreLoading = true, moreError = null)
        moreJob = viewModelScope.launch {
            try {
                val page = repo.reviews(slug, cursor)
                _reviews.value = _reviews.value.copy(items = (_reviews.value.items + page.items).distinctBy { it.id },
                    average = page.average, total = page.total, cursor = page.nextCursor, moreLoading = false)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _reviews.value = _reviews.value.copy(moreLoading = false, moreError = errorMessage(e, repo.language.value)) }
        }
    }
    fun delete() {
        if (_reviews.value.deleting || repo.auth.value == null) return
        loadJob?.cancel(); moreJob?.cancel()
        _reviews.value = _reviews.value.copy(deleting = true, moreLoading = false, ownError = null)
        writeJob = viewModelScope.launch {
            try { repo.deleteReview(slug); _reviews.value = _reviews.value.copy(deleting = false, mine = null); refresh() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _reviews.value = _reviews.value.copy(deleting = false, ownError = errorMessage(e, repo.language.value)) }
        }
    }
    fun loadEditor() {
        editorStarted = true
        if (_movie.value == null) loadMovie()
        if (loadJob?.isActive == true || _editor.value.saving) return
        _editor.value = _editor.value.copy(loading = true, error = null)
        loadJob = viewModelScope.launch {
            try {
                check(repo.auth.value != null)
                val mine = repo.ownReview(slug)
                _editor.value = ReviewEditorState(savedState["rating"] ?: mine?.rating ?: 0,
                    savedState["content"] ?: mine?.content.orEmpty(), loading = false, existing = mine != null, ready = true)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _editor.value = _editor.value.copy(loading = false, error = errorMessage(e, repo.language.value)) }
        }
    }
    fun loadMovie() {
        if (movieJob?.isActive == true) return
        _movieError.value = null
        movieJob = viewModelScope.launch {
            try { _movie.value = repo.api.detail(slug).movie }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _movieError.value = errorMessage(e, repo.language.value) }
        }
    }
    fun rating(value: Int) { if (!_editor.value.saving) { savedState["rating"] = value; _editor.value = _editor.value.copy(rating = value) } }
    fun content(value: String) { if (!_editor.value.saving && value.length <= 2000) { savedState["content"] = value; _editor.value = _editor.value.copy(content = value) } }
    fun save() {
        val draft = _editor.value
        if (!draft.ready || draft.loading || draft.saving || draft.rating !in 1..5 || repo.auth.value == null) return
        _editor.value = draft.copy(saving = true, error = null)
        writeJob = viewModelScope.launch {
            try { repo.saveReview(slug, draft.rating, draft.content); _editor.value = _editor.value.copy(saving = false, saved = true) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _editor.value = _editor.value.copy(saving = false, error = errorMessage(e, repo.language.value)) }
        }
    }
}
