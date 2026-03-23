package net.tomascichero.birthdayremainder.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import net.tomascichero.birthdayremainder.notifications.NotificationsManager

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
    ) {
        // Account section
        SectionHeader("Account")

        ListItem(
            headlineContent = { Text(user?.displayName ?: user?.email ?: "Anonymous") },
            supportingContent = user?.email?.let { { Text(it) } },
            leadingContent = { Icon(Icons.Default.AccountCircle, contentDescription = null) }
        )

        ListItem(
            headlineContent = { Text("Sign out") },
            leadingContent = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
            modifier = Modifier.clickable {
                FirebaseAuth.getInstance().signOut()
            }
        )

        ListItem(
            headlineContent = { Text("Delete all my data") },
            leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
            modifier = Modifier.clickable {
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

        HorizontalDivider()

        // Notifications section
        SectionHeader("Notifications")

        ListItem(
            headlineContent = { Text("Notifications") },
            leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
            trailingContent = {
                Switch(
                    checked = notificationsEnabled == true,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            if (enabled) {
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
                    },
                    enabled = notificationsEnabled != null
                )
            }
        )

        ListItem(
            headlineContent = { Text("Notification time") },
            supportingContent = { Text(selectedTime ?: "Loading...") },
            leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
            modifier = Modifier.clickable(enabled = notificationsEnabled == true) {
                showTimePicker = true
            }
        )

        HorizontalDivider()

        // App section
        SectionHeader("App")

        ListItem(
            headlineContent = { Text("Share") },
            leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
            modifier = Modifier.clickable {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "https://birthday-remainder-app.web.app/")
                }
                context.startActivity(Intent.createChooser(intent, "Share"))
            }
        )

        ListItem(
            headlineContent = { Text("Privacy Policy") },
            leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
            modifier = Modifier.clickable {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://birthday-remainder-app.web.app/privacy-policy"))
                )
            }
        )

        ListItem(
            headlineContent = { Text("Terms of Use") },
            leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
            modifier = Modifier.clickable {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://birthday-remainder-app.web.app/terms-of-use"))
                )
            }
        )

        ListItem(
            headlineContent = { Text("Help & Contact") },
            leadingContent = { Icon(Icons.Default.Email, contentDescription = null) },
            modifier = Modifier.clickable {
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
    }

    if (showTimePicker) {
        NotificationTimePickerDialog(
            options = timeOptions,
            selectedTime = selectedTime,
            onTimeSelected = { time ->
                selectedTime = time
                showTimePicker = false
                scope.launch {
                    NotificationsManager.setTime(time)
                }
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}
