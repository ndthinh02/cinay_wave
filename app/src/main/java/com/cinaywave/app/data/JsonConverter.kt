package com.cinaywave.app.data

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class JsonConverter(private val gson: Gson) : Converter.Factory() {
    override fun responseBodyConverter(type: Type, annotations: Array<Annotation>, retrofit: Retrofit): Converter<ResponseBody, *> =
        Converter<ResponseBody, Any> { body -> body.use { gson.fromJson(it.charStream(), type) } }
    override fun requestBodyConverter(type: Type, parameterAnnotations: Array<Annotation>, methodAnnotations: Array<Annotation>, retrofit: Retrofit): Converter<*, RequestBody> =
        Converter<Any, RequestBody> { gson.toJson(it, type).toRequestBody("application/json; charset=UTF-8".toMediaType()) }
}
