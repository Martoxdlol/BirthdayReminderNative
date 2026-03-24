package net.tomascichero.birthdayremainder

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.tomascichero.birthdayremainder.data.Birthday
import net.tomascichero.birthdayremainder.preferences.AppPreferences
import net.tomascichero.birthdayremainder.preferences.ThemeMode
import net.tomascichero.birthdayremainder.ui.add.AddScreen
import net.tomascichero.birthdayremainder.ui.home.BirthdayDetailSheet
import net.tomascichero.birthdayremainder.ui.home.BirthdayListScreen
import net.tomascichero.birthdayremainder.ui.login.LoginScreen
import net.tomascichero.birthdayremainder.ui.settings.SettingsScreen
import net.tomascichero.birthdayremainder.ui.share.ReceiveScreen
import net.tomascichero.birthdayremainder.ui.theme.BirthdayReminderTheme


class MainActivity : AppCompatActivity() {

    val pendingShareUrl = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPreferences.init(this)
        if (savedInstanceState == null) {
            handleIncomingIntent(intent)
        }
        enableEdgeToEdge()
        setContent {
            AppRoot(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val data = intent?.data
        if (data != null && data.host == "birthday-remainder-app.web.app" && data.path?.startsWith("/share") == true) {
            pendingShareUrl.value = data.toString()
        }
    }
}

@Composable
fun AppRoot(activity: MainActivity? = null) {
    val themeMode by AppPreferences.themeMode.collectAsState()

    val darkTheme = when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }

    BirthdayReminderTheme(darkTheme = darkTheme) {
        AuthWrapper(activity)
    }
}

@Composable
fun AuthWrapper(activity: MainActivity? = null) {
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
        AppScreen(activity)
    } else {
        LoginScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppScreen(activity: MainActivity? = null) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedItem by rememberSaveable { mutableIntStateOf(0) }
    val navItems = listOf(
        stringResource(R.string.nav_home),
        stringResource(R.string.nav_add),
        stringResource(R.string.nav_settings)
    )
    val icons =
        listOf(Icons.Default.Home, Icons.Default.Add, Icons.Default.Settings)

    val scrollBehavior =
        TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val pendingUrl by activity?.pendingShareUrl?.collectAsState() ?: remember { mutableStateOf(null) }

    if (pendingUrl != null) {
        ReceiveScreen(
            url = pendingUrl!!,
            onDone = { activity?.pendingShareUrl?.value = null }
        )
        return
    }

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
                            placeholder = { Text(stringResource(R.string.search_birthdays)) },
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

                        1 -> Text(stringResource(R.string.add_birthday))
                        2 -> Text(stringResource(R.string.nav_settings))
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

    var sheetBirthday by remember { mutableStateOf<Birthday?>(null) }

    var paddingBottom = imePaddingBottom
    if(innerPaddingValues.calculateBottomPadding() > paddingBottom) {
        paddingBottom = innerPaddingValues.calculateBottomPadding()
    }

    Box(modifier = Modifier.padding(
        top = innerPaddingValues.calculateTopPadding(),
        bottom = paddingBottom
    )) {
        Box(modifier = if (selectedItem == 0) Modifier else Modifier.size(0.dp)) {
            BirthdayListScreen(filter = filter)
        }
        if (selectedItem == 1) {
            AddScreen(onBirthdaySaved = { sheetBirthday = it })
        }
        if (selectedItem == 2) {
            SettingsScreen()
        }
    }

    sheetBirthday?.let { birthday ->
        BirthdayDetailSheet(
            birthday = birthday,
            onDismiss = { sheetBirthday = null }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AppScreenPreview() {
    BirthdayReminderTheme {
        AppScreen()
    }
}
