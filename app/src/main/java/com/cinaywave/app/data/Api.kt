package com.cinaywave.app.data

import retrofit2.http.*
import retrofit2.http.Query as QueryParam

interface Api {
    @GET("reviews/{slug}") suspend fun reviews(@Path("slug") slug: String, @QueryParam("cursor") cursor: String? = null): ReviewPage
    @GET("reviews/{slug}/mine") suspend fun ownReview(@Path("slug") slug: String): OwnReview
    @PUT("reviews/{slug}/mine") suspend fun saveReview(@Path("slug") slug: String, @Body body: ReviewInput): OwnReview
    @DELETE("reviews/{slug}/mine") suspend fun deleteReview(@Path("slug") slug: String)
    @GET("catalog/home") suspend fun home(): Home
    @GET("catalog/filters") suspend fun filters(): Filters
    @GET("catalog/movies") suspend fun movies(@QueryMap query: Map<String, String>): MoviePage
    @GET("catalog/movies/{slug}") suspend fun detail(@Path("slug") slug: String): Detail
    @POST("auth/login") suspend fun login(@Body body: Credentials): Auth
    @POST("auth/register") suspend fun register(@Body body: Credentials): PendingEmail
    @POST("auth/resend") suspend fun resend(@Body body: Credentials): PendingEmail
    @POST("auth/verify") suspend fun verify(@Body body: VerifyEmail): Auth
    @POST("auth/google") suspend fun google(@Body body: GoogleToken): Auth
    @POST("auth/refresh") suspend fun refresh(@Body body: Refresh): Auth
    @POST("auth/logout") suspend fun logout(@Body body: Refresh)
    @GET("me") suspend fun me(@Header("Authorization") authorization: String? = null): User
    @PATCH("me") suspend fun updateMe(@Body body: Map<String, String>, @Header("Authorization") authorization: String? = null): User
    @PUT("me/avatar") suspend fun updateAvatar(@Body image: okhttp3.RequestBody, @Header("X-Profile-Name") name: String, @Header("Authorization") authorization: String): User
    @GET("favorites") suspend fun favorites(@QueryParam("page") page: Int, @QueryParam("limit") limit: Int = 64): MoviePage
    @PUT("favorites/{slug}") suspend fun favorite(@Path("slug") slug: String): Movie
    @DELETE("favorites/{slug}") suspend fun unfavorite(@Path("slug") slug: String)
    @GET("history") suspend fun history(@QueryParam("page") page: Int, @QueryParam("limit") limit: Int = 64): HistoryPage
    @PUT("history/{slug}") suspend fun progress(@Path("slug") slug: String, @Body body: ProgressBody, @Header("Authorization") authorization: String): Progress
    @DELETE("history/{slug}") suspend fun deleteHistory(@Path("slug") slug: String)
    @DELETE("history") suspend fun clearHistory()
}
