package net.tomascichero.birthdayremainder.data

import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Birthday(
    val id: String,
    val personName: String,
    val birth: LocalDate,
    val notes: String,
    val noYear: Boolean
) {
    fun nextBirthday(from: LocalDate = LocalDate.now()): LocalDate {
        val thisYear = from.year
        val next = LocalDate.of(thisYear, birth.month, birth.dayOfMonth)
        return if (next.isBefore(from)) {
            LocalDate.of(thisYear + 1, birth.month, birth.dayOfMonth)
        } else {
            next
        }
    }

    fun daysUntilNextBirthday(from: LocalDate = LocalDate.now()): Long {
        return ChronoUnit.DAYS.between(from, nextBirthday(from))
    }

    fun nextAge(from: LocalDate = LocalDate.now()): Int? {
        if (noYear) return null
        return nextBirthday(from).year - birth.year
    }

    companion object {
        fun fromFirestore(id: String, data: Map<String, Any?>): Birthday {
            val rawBirth = data["birth"]
            val birth = when (rawBirth) {
                is Timestamp -> {
                    val date = rawBirth.toDate()
                    @Suppress("DEPRECATION")
                    LocalDate.of(date.year + 1900, date.month + 1, date.date)
                }
                else -> LocalDate.of(2000, 1, 1)
            }

            return Birthday(
                id = id,
                personName = data["personName"] as? String ?: "",
                birth = birth,
                notes = data["notes"] as? String ?: "",
                noYear = data["noYear"] as? Boolean ?: false
            )
        }
    }
}
