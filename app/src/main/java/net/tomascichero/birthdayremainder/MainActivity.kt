package net.tomascichero.birthdayremainder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import net.tomascichero.birthdayremainder.data.sampleBirthdays
import net.tomascichero.birthdayremainder.ui.add.AddScreen
import net.tomascichero.birthdayremainder.ui.home.BirthdayListScreen
import net.tomascichero.birthdayremainder.ui.login.LoginScreen
import net.tomascichero.birthdayremainder.ui.settings.SettingsScreen
import net.tomascichero.birthdayremainder.ui.theme.BirthdayReminderTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BirthdayReminderTheme {
                AuthWrapper()
            }
        }
    }
}

@Composable
fun AuthWrapper() {
    val auth = FirebaseAuth.getInstance()
    var user by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            user = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    if (user != null) {
        AppScreen()
    } else {
        LoginScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var selectedItem by remember { mutableIntStateOf(0) }
    val navItems = listOf("Home", "Add", "Settings")
    val icons =
        listOf(Icons.Default.Home, Icons.Default.Add, Icons.Default.Settings)

    val scrollBehavior =
        TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())



    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    when (selectedItem) {
                        0 -> TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search birthdays...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            )
                        )

                        1 -> Text("Add Birthday")
                        2 -> Text("Settings")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
             NavigationBar {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        AppMainContainer(
            innerPaddingValues = innerPadding,
            selectedItem = selectedItem,
            filter = searchQuery
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppMainContainer(innerPaddingValues: PaddingValues, selectedItem: Int, filter: String) {

    val imePaddingBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    val filteredBirthdays = if (filter.isEmpty()) {
        sampleBirthdays
    } else {
        sampleBirthdays.filter {
            it.name.contains(filter, ignoreCase = true)
        }
    }

    var paddingBottom = imePaddingBottom
    if(innerPaddingValues.calculateBottomPadding() > paddingBottom) {
        paddingBottom = innerPaddingValues.calculateBottomPadding()
    }

    Box(modifier = Modifier.padding(
        top = innerPaddingValues.calculateTopPadding(),
        bottom = paddingBottom
    )) {
        when (selectedItem) {
            0 -> BirthdayListScreen(
                birthdays = filteredBirthdays,
            )

            1 -> AddScreen()
            2 -> SettingsScreen()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppScreenPreview() {
    BirthdayReminderTheme {
        AppScreen()
    }
}
