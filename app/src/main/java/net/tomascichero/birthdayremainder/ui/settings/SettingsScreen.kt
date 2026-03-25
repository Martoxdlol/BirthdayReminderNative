package net.tomascichero.birthdayremainder.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import net.tomascichero.birthdayremainder.R
import net.tomascichero.birthdayremainder.notifications.NotificationsManager
import net.tomascichero.birthdayremainder.preferences.AppPreferences
import net.tomascichero.birthdayremainder.preferences.ThemeMode

private val outerCorner = 16.dp
private val innerCorner = 4.dp
private val itemGap = 2.dp

private fun groupShape(position: GroupPosition): Shape = when (position) {
    GroupPosition.Solo -> RoundedCornerShape(outerCorner)
    GroupPosition.Top -> RoundedCornerShape(topStart = outerCorner, topEnd = outerCorner, bottomStart = innerCorner, bottomEnd = innerCorner)
    GroupPosition.Middle -> RoundedCornerShape(innerCorner)
    GroupPosition.Bottom -> RoundedCornerShape(topStart = innerCorner, topEnd = innerCorner, bottomStart = outerCorner, bottomEnd = outerCorner)
}

private enum class GroupPosition { Solo, Top, Middle, Bottom }

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val scope = rememberCoroutineScope()

    var notificationsEnabled by remember { mutableStateOf<Boolean?>(null) }
    var hasPermission by remember { mutableStateOf(NotificationsManager.hasNotificationPermission(context)) }
    var selectedTime by remember { mutableStateOf<String?>(null) }
    var timeOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }

    val currentTheme by AppPreferences.themeMode.collectAsState()
    val currentLanguage by AppPreferences.language.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            scope.launch {
                NotificationsManager.enable(context)
                notificationsEnabled = true
                NotificationsManager.updateUserInfo(context)
            }
        }
    }

    LaunchedEffect(Unit) {
        notificationsEnabled = NotificationsManager.isEnabled(context)
        selectedTime = NotificationsManager.getCurrentTime()
            ?: NotificationsManager.getDefaultTime()
        timeOptions = NotificationsManager.getTimeOptions()
    }

    // Pre-compute email strings at composable level (stringResource can't be called inside lambdas)
    val deleteDataSubject = stringResource(R.string.delete_data_subject)
    val helpSubject = stringResource(R.string.help_subject)
    val helpBody = stringResource(R.string.help_body)
    val anonymousLabel = stringResource(R.string.anonymous)
    val loadingLabel = stringResource(R.string.loading)
    val shareLabel = stringResource(R.string.share)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Account
        SectionHeader(stringResource(R.string.account))

        SettingsItem(
            icon = Icons.Default.AccountCircle,
            title = user?.displayName ?: user?.email ?: anonymousLabel,
            subtitle = user?.email,
            position = GroupPosition.Top,
            onClick = {}
        )
        SettingsItem(
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            title = stringResource(R.string.sign_out),
            position = GroupPosition.Middle,
            onClick = { FirebaseAuth.getInstance().signOut() }
        )

        val email = user?.email ?: "<please put here your email>"
        val uidSuffix = user?.uid?.takeLast(6) ?: ""
        val deleteDataBody = stringResource(R.string.delete_data_body, email, uidSuffix)

        SettingsItem(
            icon = Icons.Default.Delete,
            title = stringResource(R.string.delete_all_my_data),
            position = GroupPosition.Bottom,
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse(
                        "mailto:martoxdlol@gmail.com" +
                                "?subject=${Uri.encode(deleteDataSubject)}" +
                                "&body=${Uri.encode(deleteDataBody)}"
                    )
                }
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Notifications
        SectionHeader(stringResource(R.string.notifications))

        NotificationToggleItem(
            enabled = notificationsEnabled,
            position = GroupPosition.Top,
            onToggle = { wantEnabled ->
                scope.launch {
                    if (wantEnabled) {
                        if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            NotificationsManager.enable(context)
                            notificationsEnabled = true
                        }
                    } else {
                        NotificationsManager.disable(context)
                        notificationsEnabled = false
                    }
                }
            }
        )

        val displayTime = selectedTime?.split(":")?.take(2)?.joinToString(":") ?: loadingLabel
        SettingsItem(
            icon = Icons.Default.Notifications,
            title = stringResource(R.string.notification_time),
            subtitle = displayTime,
            position = GroupPosition.Bottom,
            enabled = notificationsEnabled == true,
            onClick = { showTimePicker = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Preferences
        SectionHeader(stringResource(R.string.preferences))

        SettingsItem(
            icon = Icons.Default.Palette,
            title = stringResource(R.string.theme),
            subtitle = when (currentTheme) {
                ThemeMode.System -> stringResource(R.string.theme_system)
                ThemeMode.Light -> stringResource(R.string.theme_light)
                ThemeMode.Dark -> stringResource(R.string.theme_dark)
            },
            position = GroupPosition.Top,
            onClick = { showThemePicker = true }
        )
        SettingsItem(
            icon = Icons.Default.Translate,
            title = stringResource(R.string.language),
            subtitle = AppPreferences.getLanguageLabel(currentLanguage),
            position = GroupPosition.Bottom,
            onClick = { showLanguagePicker = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // App
        SectionHeader(stringResource(R.string.app_section))

        SettingsItem(
            icon = Icons.Default.Share,
            title = stringResource(R.string.share),
            position = GroupPosition.Top,
            onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "https://birthday-remainder-app.web.app/")
                }
                context.startActivity(Intent.createChooser(intent, shareLabel))
            }
        )
        SettingsItem(
            icon = Icons.Default.Lock,
            title = stringResource(R.string.privacy_policy),
            position = GroupPosition.Middle,
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://birthday-remainder-app.web.app/privacy-policy"))
                )
            }
        )
        SettingsItem(
            icon = Icons.Default.Info,
            title = stringResource(R.string.terms_of_use),
            position = GroupPosition.Middle,
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://birthday-remainder-app.web.app/terms-of-use"))
                )
            }
        )
        SettingsItem(
            icon = Icons.Default.Email,
            title = stringResource(R.string.help_and_contact),
            position = GroupPosition.Bottom,
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse(
                        "mailto:martoxdlol@gmail.com" +
                                "?subject=${Uri.encode(helpSubject)}" +
                                "&body=${Uri.encode(helpBody)}"
                    )
                }
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showTimePicker) {
        NotificationTimePickerDialog(
            options = timeOptions,
            selectedTime = selectedTime,
            onTimeSelected = { time ->
                selectedTime = time
                showTimePicker = false
                scope.launch { NotificationsManager.setTime(context, time) }
            },
            onDismiss = { showTimePicker = false }
        )
    }

    if (showThemePicker) {
        OptionPickerDialog(
            title = stringResource(R.string.theme),
            options = listOf(
                stringResource(R.string.theme_system),
                stringResource(R.string.theme_light),
                stringResource(R.string.theme_dark)
            ),
            selectedIndex = currentTheme.ordinal,
            onSelected = { index ->
                val mode = ThemeMode.entries[index]
                AppPreferences.setTheme(context, mode)
                showThemePicker = false
            },
            onDismiss = { showThemePicker = false }
        )
    }

    if (showLanguagePicker) {
        OptionPickerDialog(
            title = stringResource(R.string.language),
            options = AppPreferences.availableLanguages.map { it.label },
            selectedIndex = AppPreferences.availableLanguages.indexOfFirst { it.code == currentLanguage }.coerceAtLeast(0),
            onSelected = { index ->
                val lang = AppPreferences.availableLanguages[index]
                AppPreferences.setLanguage(context, lang.code)
                showLanguagePicker = false
            },
            onDismiss = { showLanguagePicker = false }
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    position: GroupPosition,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val topPadding: Dp = if (position == GroupPosition.Top || position == GroupPosition.Solo) 0.dp else itemGap

    Column(modifier = Modifier.padding(top = topPadding)) {
        Card(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = groupShape(position),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (enabled) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationToggleItem(
    enabled: Boolean?,
    position: GroupPosition,
    onToggle: (Boolean) -> Unit
) {
    Card(
        onClick = { if (enabled != null) onToggle(enabled) },
        enabled = enabled != null,
        modifier = Modifier.fillMaxWidth(),
        shape = groupShape(position),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (enabled == true) Icons.Default.Notifications else Icons.Outlined.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.notifications), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (enabled == true) stringResource(R.string.enabled) else stringResource(R.string.disabled),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled == true,
                onCheckedChange = { onToggle(it) },
                enabled = enabled != null
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
    )
}
