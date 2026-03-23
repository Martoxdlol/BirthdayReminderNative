package net.tomascichero.birthdayremainder.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.tomascichero.birthdayremainder.R
import net.tomascichero.birthdayremainder.data.Birthday
import net.tomascichero.birthdayremainder.data.BirthdayRepository
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun BirthdayListScreen(
    filter: String = "",
    modifier: Modifier = Modifier,
) {
    val allBirthdays by remember { BirthdayRepository.getBirthdaysFlow() }
        .collectAsState(initial = null)

    var selectedBirthday by remember { mutableStateOf<Birthday?>(null) }

    val birthdays = allBirthdays

    if (birthdays == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val filtered = if (filter.isEmpty()) {
        birthdays
    } else {
        birthdays.filter { it.personName.contains(filter, ignoreCase = true) }
    }

    if (filtered.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (filter.isEmpty()) stringResource(R.string.no_birthdays_yet) else stringResource(R.string.no_results),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (filter.isEmpty()) {
            item(key = "notification_prompt") {
                NotificationPromptCard(onDismiss = {})
            }
        }
        items(items = filtered, key = { it.id }) { birthday ->
            BirthdayListItem(
                birthday = birthday,
                onClick = { selectedBirthday = birthday }
            )
        }
    }

    selectedBirthday?.let { birthday ->
        BirthdayDetailSheet(
            birthday = birthday,
            onDismiss = { selectedBirthday = null }
        )
    }
}

@Composable
fun BirthdayListItem(
    birthday: Birthday,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val daysUntil = birthday.daysUntilNextBirthday()
    val nextAge = birthday.nextAge()

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault())
    }

    val daysText = when (daysUntil) {
        0L -> stringResource(R.string.today)
        1L -> stringResource(R.string.tomorrow)
        else -> stringResource(R.string.days_format, daysUntil.toInt())
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = birthday.personName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(birthday.birth.format(dateFormatter))
                        if (nextAge != null) {
                            append(" · ")
                            append(stringResource(R.string.turns_format, nextAge))
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = daysText,
                style = if (daysUntil == 0L) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                color = if (daysUntil == 0L) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
