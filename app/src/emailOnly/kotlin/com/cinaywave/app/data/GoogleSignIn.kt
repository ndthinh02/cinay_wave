package com.cinaywave.app.data

import android.content.Context

/** Explicit offline verification build; no simulated Google identities or tokens. */
object GoogleSignIn {
    suspend fun token(context: Context): String? = throw AuthFlowException("Bản kiểm tra ngoại tuyến chưa tích hợp thư viện Google. Vui lòng dùng email.")
    suspend fun clear(context: Context) = Unit
}
