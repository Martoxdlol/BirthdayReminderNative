package net.tomascichero.birthdayremainder.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import net.tomascichero.birthdayremainder.Analytics
import net.tomascichero.birthdayremainder.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    onBirthdaySaved: (net.tomascichero.birthdayremainder.data.Birthday) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var selectedMonth by remember { mutableIntStateOf(LocalDate.now().monthValue) }
    var selectedDay by remember { mutableIntStateOf(LocalDate.now().dayOfMonth) }
    var yearText by remember { mutableStateOf("") }
    var turnsText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Which field the user last edited
    var lastEditedAgeField by remember { mutableStateOf<String?>(null) }

    val currentYear = LocalDate.now().year
    val focusManager = LocalFocusManager.current

    // Sync year <-> turns
    fun onYearChanged(value: String) {
        yearText = value
        lastEditedAgeField = "year"
        val y = value.toIntOrNull()
        turnsText = if (y != null && y in 1900..currentYear + 1) {
            (currentYear - y).toString()
        } else {
            ""
        }
    }

    fun onTurnsChanged(value: String) {
        turnsText = value
        lastEditedAgeField = "turns"
        val t = value.toIntOrNull()
        yearText = if (t != null && t in 0..130) {
            (currentYear - t).toString()
        } else {
            ""
        }
    }

    fun resetForm() {
        name = ""
        selectedMonth = LocalDate.now().monthValue
        selectedDay = LocalDate.now().dayOfMonth
        yearText = ""
        turnsText = ""
        notes = ""
        lastEditedAgeField = null
    }

    fun save() {
        if (name.isBlank()) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val birthYear = yearText.toIntOrNull()
        val noYear = birthYear == null
        val date = java.util.GregorianCalendar(
            if (noYear) 2000 else birthYear!!,
            selectedMonth - 1,
            selectedDay
        ).time

        val savedBirthday = net.tomascichero.birthdayremainder.data.Birthday(
            id = "",
            personName = name.trim(),
            birth = LocalDate.of(
                if (noYear) 2000 else birthYear!!,
                selectedMonth,
                selectedDay
            ),
            notes = notes,
            noYear = noYear
        )

        FirebaseFirestore.getInstance().collection("birthdays").add(
            hashMapOf(
                "personName" to name.trim(),
                "birth" to com.google.firebase.Timestamp(date),
                "noYear" to noYear,
                "notes" to notes,
                "owner" to uid,
                "app_version" to "3.0.2",
                "created_at" to com.google.firebase.Timestamp.now(),
                "updated_at" to com.google.firebase.Timestamp.now(),
            )
        )
        Analytics.birthdayAdded(hasYear = !noYear)
        resetForm()
        onBirthdaySaved(savedBirthday)
    }

    val daysInMonth = YearMonth.of(
        yearText.toIntOrNull() ?: currentYear,
        selectedMonth
    ).lengthOfMonth()

    // Clamp day if month/year changed
    if (selectedDay > daysInMonth) {
        selectedDay = daysInMonth
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Name
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

        // Month + Day row
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

        // Year + Turns row
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
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Right) }
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
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true,
                supportingText = if (turnsText.isEmpty()) {
                    { Text(stringResource(R.string.optional)) }
                } else null
            )
        }

        // Notes
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(R.string.notes)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Save button
        Button(
            onClick = { save() },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank()
        ) {
            Text(stringResource(R.string.add_birthday))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthDropdown(
    selectedMonth: Int,
    onMonthSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val monthName = java.time.Month.of(selectedMonth)
        .getDisplayName(TextStyle.FULL, Locale.getDefault())

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = monthName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.month)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            for (m in 1..12) {
                val name = java.time.Month.of(m)
                    .getDisplayName(TextStyle.FULL, Locale.getDefault())
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onMonthSelected(m)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDropdown(
    selectedDay: Int,
    daysInMonth: Int,
    month: Int,
    year: Int,
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedDay.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.day)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            for (d in 1..daysInMonth) {
                val dayOfWeek = LocalDate.of(year, month, d).dayOfWeek
                    .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                DropdownMenuItem(
                    text = { Text("$d ($dayOfWeek)") },
                    onClick = {
                        onDaySelected(d)
                        expanded = false
                    }
                )
            }
        }
    }
}
