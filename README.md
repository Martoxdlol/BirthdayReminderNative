# Birthday Reminder — Android (Jetpack Compose)

Native Android app for tracking birthdays with reminders, built with Jetpack Compose and Firebase.

## Prerequisites

- Android Studio Hedgehog or later
- JDK 11+
- A `google-services.json` file in `app/` (from Firebase Console, project `birthday-remainder-app`)
- For release builds: `android_apps_signature_store.keystore` at the project root and a `.env` file

## Setup

1. Clone the repository
2. Place `google-services.json` in `app/`
3. Open the project in Android Studio or build from the command line

## Build

### Debug

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release (signed)

Create a `.env` file at the project root:

```
KEYSTORE_PASSWORD=your_password
```

Then build:

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### Play Store bundle

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

## Install on device

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Project structure

```
app/src/main/java/net/tomascichero/birthdayremainder/
├── MainActivity.kt          # Entry point, navigation, auth wrapper
├── data/
│   ├── Birthday.kt          # Data model
│   ├── BirthdayRepository.kt # Firestore real-time data
│   └── SharePayload.kt      # URL encode/decode for sharing
├── notifications/
│   └── NotificationsManager.kt # FCM token and notification preferences
├── preferences/
│   └── AppPreferences.kt    # Theme and language settings
└── ui/
    ├── add/AddScreen.kt     # Add birthday form
    ├── home/
    │   ├── BirthdayListScreen.kt    # Home list with multi-select
    │   ├── BirthdayDetailSheet.kt   # Detail/edit bottom sheet
    │   └── NotificationPromptCard.kt
    ├── login/LoginScreen.kt # Google Sign-In
    ├── settings/
    │   ├── SettingsScreen.kt
    │   ├── NotificationTimePickerDialog.kt
    │   └── OptionPickerDialog.kt
    ├── share/ReceiveScreen.kt # Incoming share link handler
    └── theme/
```

## Localization

Supported languages: English, Spanish, French, Portuguese, German, Italian.

String resources are in `app/src/main/res/values-{locale}/strings.xml`. To add a new language, create a new `values-{code}` directory with a translated `strings.xml` and add the locale to `res/xml/locales_config.xml`.

## Firebase

The app uses:
- **Firebase Auth** — Google Sign-In
- **Cloud Firestore** — Birthday data storage
- **Firebase Cloud Messaging** — Push notifications
- **Firebase Remote Config** — Notification time options

## Deep links

The app handles `https://birthday-remainder-app.web.app/share#...` URLs for receiving shared birthdays.
