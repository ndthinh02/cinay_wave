package com.cinaywave.app.data

import android.content.Context
import android.content.MutableContextWrapper
import androidx.credentials.*
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import com.cinaywave.app.BuildConfig
import com.google.android.libraries.identity.googleid.*
import kotlinx.coroutines.CancellationException

object GoogleSignIn {
    suspend fun token(context: Context): String? {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) throw AuthFlowException("Đăng nhập Google chưa được cấu hình. Vui lòng dùng email.")
        try {
            val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val result = CredentialManager.create(context).getCredential(MutableContextWrapper(context), request)
            val credential = result.credential as? CustomCredential ?: throw AuthFlowException("Google trả về thông tin đăng nhập không hợp lệ.")
            if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) throw AuthFlowException("Google trả về thông tin đăng nhập không hợp lệ.")
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (_: GetCredentialCancellationException) { return null }
        catch (_: NoCredentialException) { throw AuthFlowException("Không tìm thấy tài khoản Google. Hãy thêm tài khoản Google trong Cài đặt điện thoại và thử lại.") }
        catch (_: GetCredentialProviderConfigurationException) { throw AuthFlowException("Đăng nhập Google chưa khả dụng. Hãy cập nhật Google Play Services và thử lại.") }
        catch (_: GoogleIdTokenParsingException) { throw AuthFlowException("Thông tin đăng nhập Google không hợp lệ. Vui lòng chọn lại tài khoản.") }
        catch (e: CancellationException) { throw e }
        catch (e: AuthFlowException) { throw e }
        catch (_: Exception) { throw AuthFlowException("Không mở được đăng nhập Google. Kiểm tra tài khoản Google trên thiết bị và thử lại.") }
    }
    suspend fun clear(context: Context) { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
}
