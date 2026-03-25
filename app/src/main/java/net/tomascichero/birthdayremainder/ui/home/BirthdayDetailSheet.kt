package net.tomascichero.birthdayremainder.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import net.tomascichero.birthdayremainder.data.ShareUtils
import net.tomascichero.birthdayremainder.Analytics
import net.tomascichero.birthdayremainder.R
import net.tomascichero.birthdayremainder.data.Birthday
import net.tomascichero.birthdayremainder.ui.add.DayDropdown
import net.tomascichero.birthdayremainder.ui.add.MonthDropdown
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayDetailSheet(
    birthday: Birthday,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        if (isEditing) {
            EditContent(
                birthday = birthday,
                onCancel = { isEditing = false },
                onSaved = { isEditing = false }
            )
        } else {
            DetailContent(
                birthday = birthday,
                onEditClick = { isEditing = true },
                onDeleteClick = { showDeleteConfirm = true }
            )
        }
    }

    if (showDeleteConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.delete_birthday_confirm)) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        if (birthday.id.isNotEmpty()) {
                            FirebaseFirestore.getInstance()
                                .collection("birthdays")
                                .document(birthday.id)
                                .delete()
                            Analytics.birthdayDeleted()
                        }
                        showDeleteConfirm = false
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showDeleteConfirm = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun DetailContent(
    birthday: Birthday,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault())
    }
    val fullDateFormatter = remember {
        DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())
    }

    val daysUntil = birthday.daysUntilNextBirthday()
    val nextAge = birthday.nextAge()
    val context = LocalContext.current
    val shareLabel = stringResource(R.string.share)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = birthday.personName,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                val url = ShareUtils.encodeShareUrl(listOf(birthday))
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, url)
                }
                context.startActivity(Intent.createChooser(intent, shareLabel))
                Analytics.birthdayShared()
            }) {
                Icon(Icons.Default.Share, contentDescription = shareLabel)
            }
            if (birthday.id.isNotEmpty()) {
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        DetailRow(
            label = stringResource(R.string.birthday),
            value = if (birthday.noYear) {
                birthday.birth.format(dateFormatter)
            } else {
                birthday.birth.format(fullDateFormatter)
            }
        )

        if (nextAge != null) {
            DetailRow(label = stringResource(R.string.turns), value = nextAge.toString())
        }

        DetailRow(
            label = stringResource(R.string.next_birthday),
            value = when (daysUntil) {
                0L -> stringResource(R.string.today)
                1L -> stringResource(R.string.tomorrow)
                else -> stringResource(R.string.in_days_format, daysUntil.toInt())
            }
        )

        val nextBirthday = birthday.nextBirthday()
        val nextBirthdayDayOfWeek = nextBirthday.dayOfWeek
            .getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
        DetailRow(label = stringResource(R.string.falls_on), value = nextBirthdayDayOfWeek)

        if (birthday.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.notes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = birthday.notes,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditContent(
    birthday: Birthday,
    onCancel: () -> Unit,
    onSaved: () -> Unit
) {
    val currentYear = LocalDate.now().year
    val focusManager = LocalFocusManager.current

    var name by remember { mutableStateOf(birthday.personName) }
    var selectedMonth by remember { mutableIntStateOf(birthday.birth.monthValue) }
    var selectedDay by remember { mutableIntStateOf(birthday.birth.dayOfMonth) }
    var yearText by remember {
        mutableStateOf(if (birthday.noYear) "" else birthday.birth.year.toString())
    }
    var turnsText by remember {
        mutableStateOf(if (birthday.noYear) "" else (currentYear - birthday.birth.year).toString())
    }
    var notes by remember { mutableStateOf(birthday.notes) }

    fun onYearChanged(value: String) {
        yearText = value
        val y = value.toIntOrNull()
        turnsText = if (y != null && y in 1900..currentYear + 1) {
            (currentYear - y).toString()
        } else {
            ""
        }
    }

    fun onTurnsChanged(value: String) {
        turnsText = value
        val t = value.toIntOrNull()
        yearText = if (t != null && t in 0..130) {
            (currentYear - t).toString()
        } else {
            ""
        }
    }

    fun save() {
        if (name.isBlank() || birthday.id.isEmpty()) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val birthYear = yearText.toIntOrNull()
        val noYear = birthYear == null
        val date = java.util.GregorianCalendar(
            if (noYear) 2000 else birthYear!!,
            selectedMonth - 1,
            selectedDay
        ).time

        FirebaseFirestore.getInstance().collection("birthdays").document(birthday.id).update(
            mapOf(
                "personName" to name.trim(),
                "birth" to com.google.firebase.Timestamp(date),
                "noYear" to noYear,
                "notes" to notes,
                "owner" to uid,
                "app_version" to "3.0.2",
                "updated_at" to com.google.firebase.Timestamp.now(),
            )
        )
        Analytics.birthdayEdited()
        onSaved()
    }

    val daysInMonth = YearMonth.of(
        yearText.toIntOrNull() ?: currentYear,
        selectedMonth
    ).lengthOfMonth()

    if (selectedDay > daysInMonth) {
        selectedDay = daysInMonth
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.edit_birthday),
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.name)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MonthDropdown(
                selectedMonth = selectedMonth,
                onMonthSelected = { selectedMonth = it },
                modifier = Modifier.weight(1f)
            )
            DayDropdown(
                selectedDay = selectedDay,
                daysInMonth = daysInMonth,
                month = selectedMonth,
                year = yearText.toIntOrNull() ?: currentYear,
                onDaySelected = { selectedDay = it },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = yearText,
                onValueChange = { onYearChanged(it.filter { c -> c.isDigit() }.take(4)) },
                label = { Text(stringResource(R.string.birth_year)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                supportingText = if (yearText.isEmpty()) {
                    { Text(stringResource(R.string.optional)) }
                } else null
            )
            OutlinedTextField(
                value = turnsText,
                onValueChange = { onTurnsChanged(it.filter { c -> c.isDigit() }.take(3)) },
                label = { Text(stringResource(R.string.turns)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                supportingText = if (turnsText.isEmpty()) {
                    { Text(stringResource(R.string.optional)) }
                } else null
            )
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(R.string.notes)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.cancel))
            }
            Button(
                onClick = { save() },
                modifier = Modifier.weight(1f),
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
