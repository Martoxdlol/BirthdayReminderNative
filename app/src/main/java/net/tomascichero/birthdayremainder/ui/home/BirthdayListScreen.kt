package net.tomascichero.birthdayremainder.ui.home

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.tomascichero.birthdayremainder.Analytics
import net.tomascichero.birthdayremainder.R
import net.tomascichero.birthdayremainder.data.Birthday
import net.tomascichero.birthdayremainder.data.BirthdayRepository
import net.tomascichero.birthdayremainder.data.ShareUtils
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
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    val context = LocalContext.current
    val shareLabel = stringResource(R.string.share_birthdays)

    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedIds = emptySet()
    }

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

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (selectionMode) 80.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filter.isEmpty() && !selectionMode) {
                item(key = "notification_prompt") {
                    NotificationPromptCard(onDismiss = {})
                }
            }
            if (selectionMode) {
                item(key = "selection_header") {
                    Text(
                        text = stringResource(R.string.selected_count, selectedIds.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
            items(items = filtered, key = { it.id }) { birthday ->
                BirthdayListItem(
                    birthday = birthday,
                    isSelectionMode = selectionMode,
                    isSelected = birthday.id in selectedIds,
                    onClick = {
                        if (selectionMode) {
                            selectedIds = if (birthday.id in selectedIds) {
                                val new = selectedIds - birthday.id
                                if (new.isEmpty()) selectionMode = false
                                new
                            } else {
                                selectedIds + birthday.id
                            }
                        } else {
                            selectedBirthday = birthday
                        }
                    },
                    onLongClick = {
                        if (!selectionMode) {
                            selectionMode = true
                            selectedIds = setOf(birthday.id)
                        }
                    }
                )
            }
        }

        if (selectionMode && selectedIds.isNotEmpty()) {
            FloatingActionButton(
                onClick = {
                    val selected = birthdays.filter { it.id in selectedIds }
                    val url = ShareUtils.encodeShareUrl(selected)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, url)
                    }
                    context.startActivity(Intent.createChooser(intent, shareLabel))
                    Analytics.birthdayShared(selected.size)
                    selectionMode = false
                    selectedIds = emptySet()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = shareLabel)
            }
        }
    }

    selectedBirthday?.let { birthday ->
        BirthdayDetailSheet(
            birthday = birthday,
            onDismiss = { selectedBirthday = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BirthdayListItem(
    birthday: Birthday,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
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
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = if (isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
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
            if (!isSelectionMode) {
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
}
