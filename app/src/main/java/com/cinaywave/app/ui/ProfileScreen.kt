package com.cinaywave.app.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cinaywave.app.feature.AppViewModel



@Composable
fun ProfileScreen(vm: AppViewModel, login: () -> Unit, history: () -> Unit, settings: () -> Unit, edit: () -> Unit) {
    val uiText = androidx.compose.ui.platform.LocalContext.current.resources

    val auth by vm.auth.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val watched by vm.history.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf(false) }
    val backdrop = MaterialTheme.colorScheme.background
    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(backdrop),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            uiText.getString(com.cinaywave.app.R.string.ui_my_profile_141),
                            Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium
                        )
                        FilledTonalIconButton(
                            onClick = { if (auth != null) confirm = true else login() },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = backdrop)
                        ) {
                            Icon(
                                if (auth != null) Icons.AutoMirrored.Rounded.Logout else Icons.AutoMirrored.Rounded.Login,
                                if (auth != null) uiText.getString(com.cinaywave.app.R.string.ui_sign_out_142) else uiText.getString(
                                    com.cinaywave.app.R.string.ui_sign_in_4
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.size(88.dp), contentAlignment = Alignment.TopCenter) {
                        Surface(Modifier.size(72.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = .16f)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Person, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                                auth?.user?.avatarUrl?.takeIf { it.isNotBlank() }?.let {
                                    AsyncImage(
                                        it,
                                        uiText.getString(com.cinaywave.app.R.string.ui_profile_photo_143),
                                        Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                        FilledTonalIconButton(
                            onClick = { if (auth != null) edit() else login() },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .size(48.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Icon(
                                Icons.Rounded.Edit,
                                uiText.getString(com.cinaywave.app.R.string.ui_edit_profile_144),
                                Modifier.size(17.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        auth?.user?.name ?: uiText.getString(com.cinaywave.app.R.string.ui_guest_145),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        auth?.user?.email
                            ?: uiText.getString(com.cinaywave.app.R.string.ui_sign_in_to_save_your_viewing_journey_146),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    if (auth == null) Button(
                        onClick = login,
                        modifier = Modifier.padding(top = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) { Text(uiText.getString(com.cinaywave.app.R.string.ui_sign_in_register_147)) }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        item {
            Surface(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Row(
                    Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        Modifier.size(52.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.MovieFilter,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            uiText.getString(com.cinaywave.app.R.string.ui_your_cinema_corner_148),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            uiText.getString(com.cinaywave.app.R.string.ui_favorites_0) + " · " + (favorites.data?.size
                                ?: 0) + "   /   " + uiText.getString(com.cinaywave.app.R.string.ui_watched_149) + " · " + (watched.data?.size
                                ?: 0),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item {
            ProfileMenuRow(
                uiText.getString(com.cinaywave.app.R.string.ui_personal_details_150),
                Icons.Rounded.PersonOutline
            ) { if (auth != null) edit() else login() }
        }
        item {
            ProfileMenuRow(
                uiText.getString(com.cinaywave.app.R.string.ui_watch_history_7),
                Icons.Rounded.History
            ) { if (auth != null) history() else login() }
        }
        item {
            ProfileMenuRow(
                uiText.getString(com.cinaywave.app.R.string.ui_appearance_19),
                Icons.Rounded.Palette,
                settings
            )
        }
        item {
            ProfileMenuRow(
                uiText.getString(com.cinaywave.app.R.string.ui_language_17),
                Icons.Rounded.Language,
                settings
            )
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text(uiText.getString(com.cinaywave.app.R.string.ui_sign_out_151)) },
        text = { Text(uiText.getString(com.cinaywave.app.R.string.ui_your_saved_progress_will_remain_linked_to_this_account_152)) },
        confirmButton = {
            TextButton(onClick = {
                vm.logout(); confirm = false
            }) { Text(uiText.getString(com.cinaywave.app.R.string.ui_sign_out_142)) }
        },
        dismissButton = {
            TextButton(onClick = {
                confirm = false
            }) { Text(uiText.getString(com.cinaywave.app.R.string.ui_cancel_14)) }
        })
}

@Composable
private fun ProfileMenuRow(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, click: () -> Unit) {
    Surface(
        onClick = click,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f))
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                Box(
                    contentAlignment = Alignment.Center
                ) { Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary) }
            }
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ProfileEditScreen(vm: AppViewModel, back: () -> Unit) {
    val resources = LocalContext.current.resources
    val auth by vm.auth.collectAsStateWithLifecycle()
    val draft by vm.profile.collectAsStateWithLifecycle()
    val owner = auth?.user?.id
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    var pickerOwner by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(owner) { if (owner == null) back() else vm.loadProfile() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            vm.editProfileAvatar(uri.toString(), pickerOwner)
        }
    }
    val dark = MaterialTheme.colorScheme.background.luminance() < .5f
    val background = if (dark) MaterialTheme.colorScheme.background else androidx.compose.ui.graphics.Color(0xFFFAFAFB)
    val orange = androidx.compose.ui.graphics.Color(0xFFFF7A24)
    val ink = MaterialTheme.colorScheme.onSurface
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = orange,
        unfocusedBorderColor = if (dark) MaterialTheme.colorScheme.outlineVariant else androidx.compose.ui.graphics.Color(0xFFE1E1E5))
    Column(Modifier.fillMaxSize().background(background)) {
        Box(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            IconButton(back, Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Rounded.ChevronLeft, resources.getString(com.cinaywave.app.R.string.ui_back_84))
            }
            Text(resources.getString(com.cinaywave.app.R.string.ui_edit_profile_144), Modifier.padding(horizontal = 52.dp),
                style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Box(Modifier.fillMaxWidth().padding(bottom = 12.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(96.dp)) {
                        Surface(Modifier.size(88.dp).align(Alignment.Center), shape = CircleShape,
                            border = BorderStroke(1.dp, orange.copy(alpha = .4f)), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Person, null, Modifier.size(42.dp), tint = ink.copy(alpha = .45f))
                                val image = draft.avatar ?: draft.user?.avatarUrl.orEmpty()
                                if (image.isNotBlank()) AsyncImage(image, resources.getString(com.cinaywave.app.R.string.ui_profile_photo_143), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                        }
                        IconButton(onClick = { pickerOwner = owner; picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            enabled = draft.user != null && !draft.saving, modifier = Modifier.align(Alignment.BottomEnd).offset(8.dp, 8.dp).size(48.dp)) {
                            Surface(shape = CircleShape, color = orange, border = BorderStroke(2.dp, background)) {
                                Icon(Icons.Rounded.Edit, resources.getString(com.cinaywave.app.R.string.ui_choose_profile_photo_165), Modifier.padding(6.dp).size(14.dp), tint = androidx.compose.ui.graphics.Color.White)
                            }
                        }
                    }
                }
            }
            if (draft.user == null) item { Feedback(loading = draft.loading, error = draft.error, retry = vm::loadProfile) }
            else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(resources.getString(com.cinaywave.app.R.string.ui_display_name_158), style = MaterialTheme.typography.labelLarge)
                        val invalid = draft.name.trim().length !in 2..80
                        OutlinedTextField((draft.name as Any?)?.toString() ?: "", vm::editProfileName, Modifier.fillMaxWidth(), enabled = !draft.saving, singleLine = true,
                            isError = invalid, shape = RoundedCornerShape(12.dp), colors = fieldColors)
                        if (invalid) Text(resources.getString(com.cinaywave.app.R.string.ui_name_must_contain_2_to_80_characters_159), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Email", style = MaterialTheme.typography.labelLarge)
                        OutlinedTextField((draft.user?.email as Any?)?.toString() ?: "", {}, Modifier.fillMaxWidth(), readOnly = true, singleLine = true,
                            shape = RoundedCornerShape(12.dp), colors = fieldColors,
                            trailingIcon = { Icon(Icons.Rounded.Lock, null, Modifier.size(18.dp)) })
                        Text(resources.getString(com.cinaywave.app.R.string.ui_your_account_email_cannot_be_changed_here_160), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (draft.avatar != null) item {
                    Column {
                        Text(resources.getString(com.cinaywave.app.R.string.ui_previewing_new_photo_jpeg_png_webp_up_to_5_mb_161), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { vm.editProfileAvatar(null, owner) }, enabled = !draft.saving) { Text(resources.getString(com.cinaywave.app.R.string.ui_keep_current_photo_162)) }
                    }
                }
                draft.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
            }
        }
        Button(onClick = { keyboard?.hide(); focus.clearFocus(); vm.saveProfile() }, enabled = draft.canSave,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp).fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF30323B), contentColor = androidx.compose.ui.graphics.Color.White)) {
            if (draft.saving) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)) }
            Text(resources.getString(if (draft.saving) com.cinaywave.app.R.string.ui_saving_163 else com.cinaywave.app.R.string.ui_save_changes_164))
        }
    }
}
