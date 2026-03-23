package net.tomascichero.birthdayremainder.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

@Composable
fun NotificationTimePickerDialog(
    options: List<String>,
    selectedTime: String?,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notification time") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { time ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = time,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        leadingContent = {
                            RadioButton(
                                selected = time == selectedTime,
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = time == selectedTime,
                                role = Role.RadioButton,
                                onClick = { onTimeSelected(time) }
                            )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
