package com.cinaywave.app.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.cinaywave.app.BuildConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Tokens are encrypted with a non-exportable Android Keystore key. */
class SecureSession(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)
    private val key: SecretKey get() {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("wave_session", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("wave_session", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun read(): String? = runCatching {
        val bytes = Base64.decode(prefs.getString("auth", null) ?: return null, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
    }.getOrNull()
    fun write(value: String?) {
        if (value == null) { prefs.edit().clear().commit(); return }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
        prefs.edit().putString("auth", Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)).commit()
    }
}
class Repository(private val context: Context) {
    val reviewChanges = MutableStateFlow(0L)
    suspend fun reviews(slug: String, cursor: String? = null) = api.reviews(slug, cursor)
    suspend fun ownReview(slug: String) = api.ownReview(slug).review
    suspend fun saveReview(slug: String, rating: Int, content: String) {
        api.saveReview(slug, ReviewInput(rating, content.trim()))
        reviewChanges.value++
    }
    suspend fun deleteReview(slug: String) {
        api.deleteReview(slug)
        reviewChanges.value++
    }
    private val gson = Gson()
    private val secure = SecureSession(context)
    private val prefs = context.getSharedPreferences("wave_local", Context.MODE_PRIVATE)
    private val _auth = MutableStateFlow(runCatching { gson.fromJson(secure.read(), Auth::class.java) }.getOrNull())
    val auth = _auth.asStateFlow()
    private val _theme = MutableStateFlow(prefs.getString("theme", "dark") ?: "dark")
    val theme = _theme.asStateFlow()
    private val _language = MutableStateFlow(prefs.getString("language", "vi") ?: "vi")
    val language = _language.asStateFlow()
    fun text(@androidx.annotation.StringRes resource: Int): String {
        val configuration = android.content.res.Configuration(context.resources.configuration).apply { setLocale(java.util.Locale.forLanguageTag(_language.value)) }
        return context.createConfigurationContext(configuration).getString(resource)
    }
    fun setLanguage(value: String) {
        if (value !in setOf("vi", "en")) return
        prefs.edit().putString("language", value).apply()
        _language.value = value
    }
    val searchHistory = MutableStateFlow(readStrings("search"))
    private val refreshLock = Any()
    private val queueLock = Any()
    private val syncLock = Mutex()
    private fun saveAuth(value: Auth?) = synchronized(refreshLock) { secure.write(value?.let(gson::toJson)); _auth.value = value }
    private val refreshApi: Api = Retrofit.Builder().baseUrl(BuildConfig.API_URL).client(OkHttpClient.Builder().callTimeout(35, TimeUnit.SECONDS).build()).addConverterFactory(JsonConverter(gson)).build().create(Api::class.java)
    val api: Api = Retrofit.Builder().baseUrl(BuildConfig.API_URL).client(OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(40, TimeUnit.SECONDS).callTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val snapshot = _auth.value
            val explicit = chain.request().header("Authorization")
            val token = explicit?.removePrefix("Bearer ") ?: snapshot?.accessToken
            val request = chain.request().newBuilder().apply { if (explicit == null) token?.let { header("Authorization", "Bearer $it") } }.build()
            val response = chain.proceed(request)
            if (response.code != 401 || token == null || request.url.encodedPath.contains("/auth/")) response
            else {
                val updated = synchronized(refreshLock) {
                    if (_auth.value?.user?.id != snapshot?.user?.id || (explicit != null && token != snapshot?.accessToken)) null
                    else if (_auth.value?.accessToken != token) _auth.value?.accessToken else {
                        val session = _auth.value
                        try {
                            val result = refreshApiRefresh(session!!.refreshToken)
                            saveAuth(result); result.accessToken
                        } catch (e: Exception) {
                            if (e is retrofit2.HttpException && e.code() == 401) saveAuth(null)
                            null
                        }
                    }
                }
                if (updated == null) response else { response.close(); chain.proceed(request.newBuilder().header("Authorization", "Bearer $updated").build()) }
            }
        }.build()).addConverterFactory(JsonConverter(gson)).build().create(Api::class.java)
    // Retrofit suspend calls execute on OkHttp, separate from this interceptor's worker.
    private fun refreshApiRefresh(token: String): Auth = kotlinx.coroutines.runBlocking { refreshApi.refresh(Refresh(token)) }
    suspend fun login(email: String, password: String, name: String?) {
        val result = api.login(Credentials(email.trim(), password))
        withContext(Dispatchers.IO) { saveAuth(result) }
        flushProgress()
    }
    suspend fun acceptAuth(result: Auth) {
        withContext(Dispatchers.IO) { saveAuth(result) }
        flushProgress()
    }
    suspend fun logout() {
        val token = _auth.value?.refreshToken
        try {
            try { flushProgress() } finally { if (token != null) api.logout(Refresh(token)) }
        } finally {
            withContext(Dispatchers.IO) { saveAuth(null) }
            runCatching { GoogleSignIn.clear(context) }
        }
    }
    fun setTheme(value: String) { prefs.edit().putString("theme", value).apply(); _theme.value = value }
    private fun readStrings(key: String): List<String> = runCatching { gson.fromJson<List<String>>(prefs.getString(key, "[]"), object : TypeToken<List<String>>() {}.type) }.getOrDefault(emptyList())
    fun rememberSearch(value: String) {
        if (value.isBlank()) return
        searchHistory.value = (listOf(value.trim()) + searchHistory.value).distinct().take(12)
        prefs.edit().putString("search", gson.toJson(searchHistory.value)).apply()
    }
    fun removeSearch(value: String? = null) { searchHistory.value = if (value == null) emptyList() else searchHistory.value - value; prefs.edit().putString("search", gson.toJson(searchHistory.value)).apply() }
    suspend fun movies(q: Query, page: Int): MoviePage = api.movies(buildMap {
        put("page", page.toString()); put("type", q.type)
        if (q.keyword.isNotBlank()) put("keyword", q.keyword.trim())
        if (q.category.isNotBlank()) put("category", q.category)
        if (q.country.isNotBlank()) put("country", q.country)
        if (q.year.isNotBlank()) put("year", q.year)
    })
    private fun queue(userId: String): List<Progress> = runCatching { gson.fromJson<List<Progress>>(prefs.getString("pending_$userId", "[]"), object : TypeToken<List<Progress>>() {}.type) }.getOrDefault(emptyList())
    private fun identity(p: Progress) = "${p.movie.slug}|${p.server}|${p.episode}"
    fun enqueue(userId: String?, progress: Progress) {
        if (userId == null) return
        synchronized(queueLock) {
            val existing = queue(userId).filter { identity(it) != identity(progress) }
            prefs.edit().putString("pending_$userId", gson.toJson((existing + progress).takeLast(200))).commit()
        }
    }
    suspend fun flushProgress() = syncLock.withLock {
        val session = _auth.value ?: return@withLock
        val userId = session.user.id
        val pending = synchronized(queueLock) { queue(userId) }
        for (p in pending) {
            if (_auth.value?.user?.id != userId) break
            try {
                val token = _auth.value?.takeIf { it.user.id == userId }?.accessToken ?: break
                api.progress(p.movie.slug, ProgressBody(p.server, p.episode, p.episodeName, p.positionMs, p.durationMs, p.watchedAt), "Bearer $token")
                synchronized(queueLock) {
                    val remaining = queue(userId).filterNot { identity(it) == identity(p) && it.watchedAt == p.watchedAt }
                    prefs.edit().putString("pending_$userId", gson.toJson(remaining)).commit()
                }
            } catch (_: Exception) { break }
        }
    }
    suspend fun favorites(): List<Movie> {
        val all = mutableListOf<Movie>(); var page = 1
        do { val result = api.favorites(page++); all.addAll(result.items); if (!result.hasMore) break } while (page <= 10000)
        return all.distinctBy { it.slug }
    }
    suspend fun history(): List<Progress> {
        flushProgress()
        val all = mutableListOf<Progress>(); var page = 1
        do { val result = api.history(page++); all.addAll(result.items); if (!result.hasMore) break } while (page <= 10000)
        val userId = _auth.value?.user?.id ?: return emptyList()
        return (all + synchronized(queueLock) { queue(userId) }).sortedByDescending { runCatching { java.time.Instant.parse(it.watchedAt).toEpochMilli() }.getOrDefault(0) }.distinctBy { it.movie.slug }
    }
    suspend fun deleteHistory(slug: String? = null) {
        if (slug == null) api.clearHistory() else api.deleteHistory(slug)
        val userId = _auth.value?.user?.id ?: return
        synchronized(queueLock) { prefs.edit().putString("pending_$userId", gson.toJson(if (slug == null) emptyList() else queue(userId).filterNot { it.movie.slug == slug })).commit() }
    }
    suspend fun updateName(name: String) {
        saveProfile(name, null)
    }
    private suspend fun rememberProfile(user: User, owner: String): User = withContext(Dispatchers.IO) {
        synchronized(refreshLock) {
            val session = _auth.value?.takeIf { it.user.id == owner && user.id == owner } ?: throw kotlinx.coroutines.CancellationException("Account changed")
            saveAuth(session.copy(user = user))
        }
        user
    }
    suspend fun loadProfile(): User {
        val session = _auth.value ?: throw kotlinx.coroutines.CancellationException("Signed out")
        return rememberProfile(api.me("Bearer ${session.accessToken}"), session.user.id)
    }
    suspend fun saveProfile(name: String, avatar: android.net.Uri?): User {
        val session = _auth.value ?: throw kotlinx.coroutines.CancellationException("Signed out")
        val updated = if (avatar == null) api.updateMe(mapOf("name" to name), "Bearer ${session.accessToken}") else {
            val body = withContext(Dispatchers.IO) {
                val bytes = try {
                    context.contentResolver.openInputStream(avatar)?.use { input ->
                        val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer); if (count < 0) break
                            if (output.size() + count > 5 * 1024 * 1024) throw ProfileInputException("Ảnh đại diện tối đa 5 MB.")
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    } ?: throw ProfileInputException("Không đọc được ảnh. Hãy chọn lại ảnh.")
                } catch (e: ProfileInputException) { throw e } catch (_: Exception) { throw ProfileInputException("Không đọc được ảnh. Hãy chọn lại ảnh.") }
                if (bytes.isEmpty()) throw ProfileInputException("Ảnh đã chọn không có dữ liệu.")
                val mime = avatarMimeType(bytes)
                    ?: throw ProfileInputException("Chỉ hỗ trợ ảnh JPG/JPEG, PNG hoặc WebP.")
                bytes.toRequestBody(mime.toMediaType())
            }
            if (_auth.value?.user?.id != session.user.id) throw kotlinx.coroutines.CancellationException("Account changed")
            api.updateAvatar(body, java.net.URLEncoder.encode(name, "UTF-8").replace("+", "%20"), "Bearer ${session.accessToken}")
        }
        return rememberProfile(updated, session.user.id)
    }
}
fun errorMessage(e: Throwable, language: String = "vi"): String {
    val english = language == "en"
    if (e is ProfileInputException) return if (english) when (e.message) {
        "Chỉ hỗ trợ ảnh JPG/JPEG, PNG hoặc WebP." -> "Choose a JPG/JPEG, PNG or WebP image."
        "Ảnh đại diện tối đa 5 MB." -> "Profile photos must be 5 MB or smaller."
        "Ảnh đã chọn không có dữ liệu." -> "The selected image is empty."
        else -> "Could not read the image. Please choose it again."
    } else e.message.orEmpty()
    if (e is AuthFlowException) return if (english) "Google sign-in is unavailable. Please try again or use email." else e.message ?: "Không đăng nhập được Google. Vui lòng thử lại."
    if (e is retrofit2.HttpException) {
        val message = runCatching { com.google.gson.JsonParser.parseString(e.response()?.errorBody()?.string()).asJsonObject.getAsJsonObject("error").get("message").asString }.getOrNull()
        return message ?: if (english) "Service unavailable (${e.code()}). Please try again." else "Dịch vụ chưa sẵn sàng (${e.code()}). Vui lòng thử lại."
    }
    return if (english) "Could not connect to the server. Check your connection and try again." else "Không kết nối được máy chủ. Kiểm tra mạng và thử lại."
}
