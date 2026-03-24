package net.tomascichero.birthdayremainder.ui.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import net.tomascichero.birthdayremainder.R
import net.tomascichero.birthdayremainder.data.ShareUtils
import net.tomascichero.birthdayremainder.data.ShareableBirthday
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveScreen(
    url: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val birthdays = ShareUtils.decodeShareUrl(url)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shared_birthdays_title)) }
            )
        }
    ) { innerPadding ->
        if (birthdays.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.invalid_share_link),
                    style = MaterialTheme.typography.bodyLarge
                )
                Button(onClick = onDone, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(R.string.cancel))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.someone_shared, birthdays.size),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(birthdays) { birthday ->
                        ShareableBirthdayCard(birthday)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDone,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = {
                            addAllToFirestore(birthdays)
                            onDone()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.add_all))
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareableBirthdayCard(birthday: ShareableBirthday) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = birthday.name,
                style = MaterialTheme.typography.titleMedium
            )
            val dateText = buildString {
                val monthName = java.time.Month.of(birthday.month)
                    .getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
                append("$monthName ${birthday.day}")
                if (birthday.year != null) append(", ${birthday.year}")
            }
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (birthday.notes.isNotBlank()) {
                Text(
                    text = birthday.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private fun addAllToFirestore(birthdays: List<ShareableBirthday>) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()
    val batch = db.batch()

    for (b in birthdays) {
        val noYear = b.year == null
        val date = java.util.GregorianCalendar(
            b.year ?: 2000,
            b.month - 1,
            b.day
        ).time

        val ref = db.collection("birthdays").document()
        batch.set(ref, hashMapOf(
            "personName" to b.name,
            "birth" to Timestamp(date),
            "noYear" to noYear,
            "notes" to (b.notes),
            "owner" to uid,
            "app_version" to "3.0.0",
            "created_at" to Timestamp.now(),
            "updated_at" to Timestamp.now(),
        ))
    }

    batch.commit()
}
