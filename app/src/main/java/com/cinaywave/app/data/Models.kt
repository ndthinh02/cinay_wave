package com.cinaywave.app.data

data class Label(val slug: String = "", val name: String = "")
data class Movie(
    val slug: String = "", val name: String = "", val originName: String = "",
    val poster: String = "", val backdrop: String = "", val year: Int? = null,
    val type: String = "", val quality: String = "", val language: String = "",
    val time: String = "", val episodeCurrent: String = "",
    val categories: List<Label> = emptyList(), val countries: List<Label> = emptyList()
)
data class MoviePage(val items: List<Movie> = emptyList(), val page: Int = 1, val totalPages: Int = 0, val total: Int = 0, val hasMore: Boolean = false)
data class Section(val slug: String, val name: String, val items: List<Movie>, val error: String? = null)
data class Home(val featured: Movie?, val sections: List<Section>)
data class Filters(val types: List<Label> = emptyList(), val categories: List<Label> = emptyList(), val countries: List<Label> = emptyList(), val years: List<Int> = emptyList())
data class Episode(val slug: String, val name: String, val url: String)
data class MovieServer(val name: String, val episodes: List<Episode>)
// Gson can invoke the no-arg constructor when every field has a default. This
// preserves defaults for fields absent from older backend responses.
data class Detail(
    val movie: Movie = Movie(), val description: String = "",
    val servers: List<MovieServer> = emptyList(), val related: List<Movie> = emptyList(),
    val actors: List<String>? = emptyList()
)
data class User(val id: String, val name: String, val email: String, val avatarUrl: String? = null)
class ProfileInputException(message: String) : Exception(message)
data class Auth(val accessToken: String, val refreshToken: String, val user: User)
data class Credentials(val email: String, val password: String, val name: String? = null)
data class Refresh(val refreshToken: String)
data class PendingEmail(val email: String, val resendAfter: Int = 60, val expiresIn: Int = 300)
data class VerifyEmail(val email: String, val code: String)
data class GoogleToken(val idToken: String)
class AuthFlowException(message: String) : Exception(message)
data class Progress(
    val movie: Movie, val server: String, val episode: String, val episodeName: String,
    val positionMs: Long, val durationMs: Long, val watchedAt: String, val completed: Boolean = false
)
data class ProgressBody(val server: String, val episode: String, val episodeName: String, val positionMs: Long, val durationMs: Long, val watchedAt: String)
data class HistoryPage(val items: List<Progress> = emptyList(), val page: Int = 1, val hasMore: Boolean = false)
data class Query(val type: String = "phim-moi", val keyword: String = "", val category: String = "", val country: String = "", val year: String = "")

data class ReviewAuthor(val id: String, val name: String, val avatarUrl: String = "")
data class FilmReview(val id: String, val rating: Int, val content: String, val updatedAt: String, val author: ReviewAuthor)
data class ReviewPage(val average: Double = 0.0, val total: Int = 0, val items: List<FilmReview> = emptyList(), val nextCursor: String? = null)
data class OwnReview(val review: FilmReview? = null)
data class ReviewInput(val rating: Int, val content: String)
