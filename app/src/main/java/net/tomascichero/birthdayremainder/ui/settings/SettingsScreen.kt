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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import net.tomascichero.birthdayremainder.notifications.NotificationsManager

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

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            scope.launch {
                NotificationsManager.enable(context)
                notificationsEnabled = true
                NotificationsManager.updateUserInfo()
            }
        }
    }

    LaunchedEffect(Unit) {
        notificationsEnabled = NotificationsManager.isEnabled(context)
        selectedTime = NotificationsManager.getCurrentTime()
            ?: NotificationsManager.getDefaultTime()
        timeOptions = NotificationsManager.getTimeOptions()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Account
        SectionHeader("Account")

        SettingsItem(
            icon = Icons.Default.AccountCircle,
            title = user?.displayName ?: user?.email ?: "Anonymous",
            subtitle = user?.email,
            position = GroupPosition.Top,
            onClick = {}
        )
        SettingsItem(
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            title = "Sign out",
            position = GroupPosition.Middle,
            onClick = { FirebaseAuth.getInstance().signOut() }
        )
        SettingsItem(
            icon = Icons.Default.Delete,
            title = "Delete all my data",
            position = GroupPosition.Bottom,
            onClick = {
                val email = user?.email ?: "<please put here your email>"
                val uidSuffix = user?.uid?.takeLast(6) ?: ""
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse(
                        "mailto:martoxdlol@gmail.com" +
                                "?subject=${Uri.encode("Delete all my data")}" +
                                "&body=${Uri.encode("Hi, I want you to delete my account and all my data from the Birthday Reminder app. My email registered in the app is: $email.\n\n$uidSuffix.\n")}"
                    )
                }
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Notifications
        SectionHeader("Notifications")

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
                        NotificationsManager.disable()
                        notificationsEnabled = false
                    }
                }
            }
        )

        val displayTime = selectedTime?.split(":")?.take(2)?.joinToString(":") ?: "Loading..."
        SettingsItem(
            icon = Icons.Default.Notifications,
            title = "Notification time",
            subtitle = displayTime,
            position = GroupPosition.Bottom,
            enabled = notificationsEnabled == true,
            onClick = { showTimePicker = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // App
        SectionHeader("App")

        SettingsItem(
            icon = Icons.Default.Share,
            title = "Share",
            position = GroupPosition.Top,
            onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "https://birthday-remainder-app.web.app/")
                }
                context.startActivity(Intent.createChooser(intent, "Share"))
            }
        )
        SettingsItem(
            icon = Icons.Default.Lock,
            title = "Privacy Policy",
            position = GroupPosition.Middle,
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://birthday-remainder-app.web.app/privacy-policy"))
                )
            }
        )
        SettingsItem(
            icon = Icons.Default.Info,
            title = "Terms of Use",
            position = GroupPosition.Middle,
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://birthday-remainder-app.web.app/terms-of-use"))
                )
            }
        )
        SettingsItem(
            icon = Icons.Default.Email,
            title = "Help & Contact",
            position = GroupPosition.Bottom,
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse(
                        "mailto:martoxdlol@gmail.com" +
                                "?subject=${Uri.encode("Birthday Reminder Help")}" +
                                "&body=${Uri.encode("Hi, I need help with the Birthday Reminder app. \n")}"
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
                scope.launch { NotificationsManager.setTime(time) }
            },
            onDismiss = { showTimePicker = false }
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
        onClick = { if (enabled != null) onToggle(enabled != true) },
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
                Text(text = "Notifications", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (enabled == true) "Enabled" else "Disabled",
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
