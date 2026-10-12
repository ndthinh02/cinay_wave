package com.cinaywave.app.ui

import android.content.MutableContextWrapper
import android.graphics.drawable.GradientDrawable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.text.Editable
import android.widget.EditText
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinaywave.app.BuildConfig
import com.cinaywave.app.R
import com.cinaywave.app.data.*
import com.cinaywave.app.feature.AppViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable fun AuthScreen(vm: AppViewModel, back: () -> Unit, done: () -> Unit) {
    val uiText = androidx.compose.ui.platform.LocalContext.current.resources

    var register by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var pending by rememberSaveable { mutableStateOf(false) }
    var resendAt by rememberSaveable { mutableLongStateOf(0L) }
    var expiresAt by rememberSaveable { mutableLongStateOf(0L) }
    var seconds by remember { mutableIntStateOf(0) }
    // Secrets are never stored in saved instance state.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var googleBusy by remember { mutableStateOf(false) }
    val motion = MotionAllowed()
    val busy by vm.authBusy.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    fun received(value: PendingEmail) { email = value.email; pending = true; code = ""; resendAt = System.currentTimeMillis() + value.resendAfter * 1000L; expiresAt = System.currentTimeMillis() + value.expiresIn * 1000L }
    fun task(action: suspend () -> Unit) {
        if (vm.authBusy.value) return
        vm.authBusy.value = true; error = null
        scope.launch {
            try { action() } catch (e: CancellationException) { throw e } catch (e: Exception) { error = errorMessage(e, vm.repo.language.value) }
            finally { vm.authBusy.value = false }
        }
    }
    fun submit() {
        error = when {
            register && name.trim().length !in 2..80 -> uiText.getString(com.cinaywave.app.R.string.ui_name_must_contain_2_to_80_characters_27)
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> uiText.getString(com.cinaywave.app.R.string.ui_enter_a_valid_email_address_28)
            password.length < 8 -> uiText.getString(com.cinaywave.app.R.string.ui_password_must_contain_at_least_8_characters_29)
            password.toByteArray().size > 72 -> uiText.getString(com.cinaywave.app.R.string.ui_password_is_too_long_use_no_more_than_72_bytes_30)
            register && password != confirmation -> uiText.getString(com.cinaywave.app.R.string.ui_passwords_do_not_match_31)
            else -> null
        }
        if (error != null) return
        keyboard?.hide()
        task {
            if (register) received(vm.repo.api.register(Credentials(email.trim(), password, name.trim())))
            else {
                try { vm.repo.acceptAuth(vm.repo.api.login(Credentials(email.trim(), password))); done() }
                catch (e: retrofit2.HttpException) {
                    if (e.code() != 403) throw e
                    try { received(vm.repo.api.resend(Credentials(email.trim(), password))) }
                    catch (delivery: retrofit2.HttpException) {
                        if (delivery.code() != 429) throw delivery
                        pending = true; email = email.trim().lowercase(); resendAt = System.currentTimeMillis() + 60000; expiresAt = 0
                        error = uiText.getString(com.cinaywave.app.R.string.ui_a_code_was_recently_requested_check_your_email_or_wait_to_re_32)
                    }
                }
            }
        }
    }
    LaunchedEffect(pending, resendAt) { while (pending) { seconds = ((resendAt - System.currentTimeMillis() + 999) / 1000).coerceAtLeast(0).toInt(); delay(1000) } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item { PageTitle(if (pending) uiText.getString(com.cinaywave.app.R.string.ui_verify_email_33) else uiText.getString(com.cinaywave.app.R.string.ui_account_34), back = { if (pending && !busy) { pending = false; error = null } else if (!busy) back() }) }
        item {
            Column(Modifier.padding(horizontal = 20.dp).animateContentSize(tween(if (motion) 200 else 0)), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) { Icon(if (pending) Icons.Rounded.MarkEmailRead else Icons.Rounded.MovieFilter, null, Modifier.padding(20.dp).size(36.dp), tint = MaterialTheme.colorScheme.primary) }
                Text(if (pending) uiText.getString(com.cinaywave.app.R.string.ui_check_your_inbox_35) else if (register) uiText.getString(com.cinaywave.app.R.string.ui_your_story_nstarts_here_36) else uiText.getString(com.cinaywave.app.R.string.ui_welcome_back_to_nthe_stories_you_love_37), style = MaterialTheme.typography.headlineMedium)
                Text(if (pending) uiText.getString(com.cinaywave.app.R.string.verification_email_hint, email) else uiText.getString(com.cinaywave.app.R.string.ui_save_favorites_sync_your_history_and_continue_watching_on_ci_38), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (pending) {
                    Text(uiText.getString(com.cinaywave.app.R.string.ui_verification_code_39), style = MaterialTheme.typography.labelLarge)
                    OtpInput(code, { code = it; error = null }, !busy)
                    Text(if (expiresAt > 0 && expiresAt < System.currentTimeMillis()) uiText.getString(com.cinaywave.app.R.string.ui_this_code_has_expired_request_a_new_code_40) else uiText.getString(com.cinaywave.app.R.string.ui_this_code_is_valid_for_5_minutes_keep_it_private_41), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (password.isBlank()) OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text(uiText.getString(com.cinaywave.app.R.string.ui_password_to_resend_the_code_42)) }, shape = MaterialTheme.shapes.medium, singleLine = true, visualTransformation = PasswordVisualTransformation())
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                    Button(onClick = { keyboard?.hide(); task { vm.repo.acceptAuth(vm.repo.api.verify(VerifyEmail(email, code))); password = ""; code = ""; done() } }, enabled = code.length == 6 && !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(uiText.getString(com.cinaywave.app.R.string.ui_verify_continue_43)) }
                    TextButton(onClick = { task { received(vm.repo.api.resend(Credentials(email, password))) } }, enabled = seconds == 0 && password.isNotBlank() && !busy, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(if (seconds > 0) uiText.getString(com.cinaywave.app.R.string.resend_countdown, seconds) else uiText.getString(com.cinaywave.app.R.string.ui_resend_code_44)) }
                    TextButton(onClick = { pending = false; code = ""; error = null }, enabled = !busy) { Text(uiText.getString(com.cinaywave.app.R.string.ui_change_email_go_back_45)) }
                } else {
                    if (register) OutlinedTextField(name, { name = it; error = null }, Modifier.fillMaxWidth(), label = { Text(uiText.getString(com.cinaywave.app.R.string.ui_your_name_46)) }, shape = MaterialTheme.shapes.medium, singleLine = true, enabled = !busy, leadingIcon = { Icon(Icons.Rounded.PersonOutline, null) }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                    OutlinedTextField(email, { email = it; error = null }, Modifier.fillMaxWidth(), label = { Text("Email") }, shape = MaterialTheme.shapes.medium, singleLine = true, enabled = !busy, leadingIcon = { Icon(Icons.Rounded.AlternateEmail, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
                    OutlinedTextField(password, { password = it; error = null }, Modifier.fillMaxWidth(), label = { Text(uiText.getString(com.cinaywave.app.R.string.ui_password_47)) }, shape = MaterialTheme.shapes.medium, singleLine = true, enabled = !busy, visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(), leadingIcon = { Icon(Icons.Rounded.Lock, null) }, trailingIcon = { IconButton(onClick = { showPassword = !showPassword }) { Icon(if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, if (showPassword) uiText.getString(com.cinaywave.app.R.string.ui_hide_password_48) else uiText.getString(com.cinaywave.app.R.string.ui_show_password_49)) } }, supportingText = { if (register) Text(uiText.getString(com.cinaywave.app.R.string.ui_password_must_contain_at_least_8_characters_29)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (register) ImeAction.Next else ImeAction.Done), keyboardActions = KeyboardActions(onDone = { submit() }))
                    if (register) OutlinedTextField(confirmation, { confirmation = it; error = null }, Modifier.fillMaxWidth(), label = { Text(uiText.getString(com.cinaywave.app.R.string.ui_confirm_password_50)) }, shape = MaterialTheme.shapes.medium, singleLine = true, enabled = !busy, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { submit() }))
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                    Button(onClick = ::submit, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { if (busy && !googleBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(if (register) uiText.getString(com.cinaywave.app.R.string.ui_create_account_51) else uiText.getString(com.cinaywave.app.R.string.ui_sign_in_4)) }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { HorizontalDivider(Modifier.weight(1f)); Text(uiText.getString(com.cinaywave.app.R.string.ui_or_52), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); HorizontalDivider(Modifier.weight(1f)) }
                    OutlinedButton(onClick = {
                        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) { error = uiText.getString(com.cinaywave.app.R.string.ui_google_sign_in_is_not_configured_please_use_email_53); return@OutlinedButton }
                        task {
                            googleBusy = true
                            try {
                                if (!BuildConfig.API_URL.startsWith("https://")) throw AuthFlowException("Đăng nhập Google cần máy chủ HTTPS. Vui lòng cấu hình DEBUG_API_URL hoặc dùng email.")
                                val token = GoogleSignIn.token(context)
                                if (token != null) { vm.repo.acceptAuth(vm.repo.api.google(GoogleToken(token))); done() }
                            } finally { googleBusy = false }
                        }
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)) {
                        if (googleBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                        else Icon(painterResource(R.drawable.ic_google), null, Modifier.size(20.dp), tint = Color.Unspecified)
                        Spacer(Modifier.width(12.dp)); Text(uiText.getString(com.cinaywave.app.R.string.ui_continue_with_google_54))
                    }
                    TextButton(onClick = { register = !register; error = null; confirmation = "" }, enabled = !busy, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(if (register) uiText.getString(com.cinaywave.app.R.string.ui_already_have_an_account_sign_in_55) else uiText.getString(com.cinaywave.app.R.string.ui_new_here_create_an_account_56)) }
                    Text(uiText.getString(com.cinaywave.app.R.string.ui_you_can_explore_and_watch_movies_without_signing_in_57), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable private fun OtpInput(value: String, changed: (String) -> Unit, enabled: Boolean) {
    val uiText = androidx.compose.ui.platform.LocalContext.current.resources

    val text = MaterialTheme.colorScheme.onSurface.toArgb()
    val background = MaterialTheme.colorScheme.surfaceVariant.toArgb()
    val callback by rememberUpdatedState(changed)
    AndroidView(modifier = Modifier.fillMaxWidth().height(68.dp), factory = { context ->
        EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(6))
            setAutofillHints("smsOTPCode")
            importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_YES
            hint = "000000"; contentDescription = uiText.getString(com.cinaywave.app.R.string.ui_6_digit_email_verification_code_58)
            textSize = 26f; letterSpacing = .25f; gravity = android.view.Gravity.CENTER
            setSingleLine(true)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { callback(s.toString().filter { it in '0'..'9' }.take(6)) }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }, update = { view ->
        view.isEnabled = enabled; view.setTextColor(text); view.setHintTextColor(android.graphics.Color.GRAY)
        view.background = GradientDrawable().apply { setColor(background); cornerRadius = 16 * view.resources.displayMetrics.density }
        if (view.text.toString() != value) { view.setText(value); view.setSelection(value.length) }
    })
}

