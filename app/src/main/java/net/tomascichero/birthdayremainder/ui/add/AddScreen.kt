package net.tomascichero.birthdayremainder.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(modifier: Modifier = Modifier) {
    var fullName by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var isMonthMenuExpanded by remember { mutableStateOf(false) }
    var isDayMenuExpanded by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
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

        ExposedDropdownMenuBox(
            expanded = isMonthMenuExpanded,
            onExpandedChange = { isMonthMenuExpanded = it }
        ) {
            OutlinedTextField(
                value = month,
                onValueChange = {},
                readOnly = true,
                label = { Text("Month") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMonthMenuExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = isMonthMenuExpanded,
                onDismissRequest = { isMonthMenuExpanded = false }
            ) {
                months.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            month = selectionOption
                            isMonthMenuExpanded = false
                            day = "" // Reset day when month changes
                        }
                    )
                }
            }
        }

        val monthNumber = months.indexOf(month) + 1
        val yearNumber = year.toIntOrNull() ?: LocalDate.now().year
        val daysInMonth = if (month.isNotEmpty()) {
            YearMonth.of(yearNumber, monthNumber).lengthOfMonth()
        } else {
            31
        }

        ExposedDropdownMenuBox(
            expanded = isDayMenuExpanded,
            onExpandedChange = { isDayMenuExpanded = it }
        ) {
            OutlinedTextField(
                value = day,
                onValueChange = {},
                readOnly = true,
                label = { Text("Day") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDayMenuExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = isDayMenuExpanded,
                onDismissRequest = { isDayMenuExpanded = false }
            ) {
                for (d in 1..daysInMonth) {
                    val dayOfWeek = if (month.isNotEmpty()) {
                        LocalDate.of(yearNumber, monthNumber, d).dayOfWeek
                            .getDisplayName(TextStyle.FULL, Locale.getDefault())
                    } else ""
                    DropdownMenuItem(
                        text = { Text("$d ($dayOfWeek)") },
                        onClick = {
                            day = d.toString()
                            isDayMenuExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = year,
            onValueChange = {
                year = it
                day = "" // Reset day when year changes
            },
            label = { Text("Year (Optional)") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Button(
            onClick = {
                // Handle form submission here
                println("Full Name: $fullName")
                println("Month: $month")
                println("Day: $day")
                println("Year: $year")
                println("Notes: $notes")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Add Birthday")
        }
    }
}