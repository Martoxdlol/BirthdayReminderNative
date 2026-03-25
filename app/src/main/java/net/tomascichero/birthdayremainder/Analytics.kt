package net.tomascichero.birthdayremainder

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object Analytics {
    private lateinit var fa: FirebaseAnalytics

    fun init(firebaseAnalytics: FirebaseAnalytics) {
        fa = firebaseAnalytics
    }

    fun screen(name: String) {
        fa.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, name)
        })
    }

    fun logEvent(name: String, params: Map<String, String> = emptyMap()) {
        fa.logEvent(name, if (params.isEmpty()) null else Bundle().apply {
            params.forEach { (k, v) -> putString(k, v) }
        })
    }

    // Auth
    fun signIn() = logEvent("sign_in")
    fun signOut() = logEvent("sign_out")

    // Birthday CRUD
    fun birthdayAdded(hasYear: Boolean) = logEvent("birthday_added", mapOf("has_year" to hasYear.toString()))
    fun birthdayEdited() = logEvent("birthday_edited")
    fun birthdayDeleted() = logEvent("birthday_deleted")
    fun birthdayViewed() = logEvent("birthday_viewed")

    // Sharing
    fun birthdayShared(count: Int = 1) = logEvent("birthday_shared", mapOf("count" to count.toString()))
    fun sharedBirthdaysReceived(count: Int) = logEvent("shared_birthdays_received", mapOf("count" to count.toString()))

    // Notifications
    fun notificationsEnabled() = logEvent("notifications_enabled")
    fun notificationsDisabled() = logEvent("notifications_disabled")
    fun notificationTimeChanged(time: String) = logEvent("notification_time_changed", mapOf("time" to time))

    // Settings
    fun themeChanged(theme: String) = logEvent("theme_changed", mapOf("theme" to theme))
    fun languageChanged(language: String) = logEvent("language_changed", mapOf("language" to language))
    fun shareAppClicked() = logEvent("share_app_clicked")
}
