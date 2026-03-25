package net.tomascichero.birthdayremainder.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import net.tomascichero.birthdayremainder.MainActivity
import net.tomascichero.birthdayremainder.R
import java.util.Locale
import java.util.TimeZone

class BirthdayMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // When app is in foreground, FCM delivers here instead of system tray.
        // When app is in background with a notification payload, the system shows it
        // automatically and this method is NOT called — tap handling happens via
        // the extras on the launch intent (handled in MainActivity.handleIncomingIntent).

        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val birthdayId = message.data["birthday_id"]

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (birthdayId != null) {
                putExtra("birthday_id", birthdayId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            birthdayId?.hashCode() ?: System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(birthdayId?.hashCode() ?: System.currentTimeMillis().toInt(), notification)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("BirthdayMessaging", "New FCM token")

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            // User not logged in yet — token will be registered when they enable notifications
            return
        }

        val locale = Locale.getDefault()
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        } catch (_: Exception) { "" }

        val body = hashMapOf<String, Any>(
            "user_id" to user.uid,
            "updated_at" to com.google.firebase.Timestamp.now(),
            "lang" to locale.language,
            "country" to (locale.country ?: ""),
            "timezone" to (TimeZone.getDefault().rawOffset / 3600000),
            "platform" to "android",
            "app_version" to versionName,
        )

        FirebaseFirestore.getInstance()
            .collection("fcm_tokens")
            .document(token)
            .set(body, SetOptions.merge())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Birthdays",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Birthday reminder notifications"
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "birthdays"
    }
}
