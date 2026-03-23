package net.tomascichero.birthdayremainder.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object BirthdayRepository {

    fun getBirthdaysFlow(): Flow<List<Birthday>> = callbackFlow {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = FirebaseFirestore.getInstance()
            .collection("birthdays")
            .whereEqualTo("owner", uid)
            .addSnapshotListener { snapshot: QuerySnapshot?, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val birthdays = snapshot?.documents
                    ?.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        Birthday.fromFirestore(doc.id, data)
                    }
                    ?.sortedBy { it.daysUntilNextBirthday() }
                    ?: emptyList()

                trySend(birthdays)
            }

        awaitClose { listener.remove() }
    }
}
