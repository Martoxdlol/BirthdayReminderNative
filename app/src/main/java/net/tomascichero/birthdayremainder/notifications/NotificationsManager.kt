package net.tomascichero.birthdayremainder.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import java.util.Locale
import java.util.TimeZone

object NotificationsManager {

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private suspend fun getFcmToken(): String? {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.e("NotificationsManager", "Cannot get FCM token", e)
            null
        }
    }

    suspend fun isEnabled(context: Context): Boolean {
        if (!hasNotificationPermission(context)) return false
        val token = getFcmToken() ?: return false
        val data = getTokenDoc(token) ?: return false
        return data["enable_notifications"] as? Boolean ?: false
    }

    suspend fun enable(context: Context): Boolean {
        if (!hasNotificationPermission(context)) return false
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val token = getFcmToken() ?: return false
        upsertTokenDoc(token, mapOf("enable_notifications" to true))
        return true
    }

    suspend fun disable(): Boolean {
        val token = getFcmToken() ?: return false
        upsertTokenDoc(token, mapOf("enable_notifications" to false))
        return true
    }

    suspend fun updateUserInfo() {
        val token = getFcmToken() ?: return
        upsertTokenDoc(token)
    }

    suspend fun getTimeOptions(): List<String> {
        return try {
            val config = FirebaseRemoteConfig.getInstance()
            config.fetchAndActivate().await()
            val raw = config.getString("daily_update_time")
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            Log.e("NotificationsManager", "Failed to get time options", e)
            listOf("08:00", "09:00", "10:00", "12:00", "18:00", "20:00")
        }
    }

    suspend fun getCurrentTime(): String? {
        val token = getFcmToken() ?: return null
        val data = getTokenDoc(token) ?: return null
        return data["daily_update_time"] as? String
    }

    suspend fun getDefaultTime(): String {
        return try {
            val config = FirebaseRemoteConfig.getInstance()
            config.fetchAndActivate().await()
            config.getString("default_daily_update_time").ifEmpty { "09:00" }
        } catch (e: Exception) {
            "09:00"
        }
    }

    suspend fun setTime(time: String) {
        val token = getFcmToken() ?: return
        upsertTokenDoc(token, mapOf("daily_update_time" to time))
    }

    private suspend fun getTokenDoc(token: String): Map<String, Any?>? {
        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection("fcm_tokens")
                .document(token)
                .get()
                .await()
            if (doc.exists()) doc.data else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun upsertTokenDoc(token: String, extra: Map<String, Any> = emptyMap()) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val locale = Locale.getDefault()

        val body = hashMapOf<String, Any>(
            "user_id" to user.uid,
            "updated_at" to com.google.firebase.Timestamp.now(),
            "lang" to locale.language,
            "country" to (locale.country ?: ""),
            "timezone" to (TimeZone.getDefault().rawOffset / 3600000),
            "platform" to "android",
        )
        body.putAll(extra)

        try {
            FirebaseFirestore.getInstance()
                .collection("fcm_tokens")
                .document(token)
                .set(body, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e("NotificationsManager", "Failed to upsert token doc", e)
        }
    }
}
