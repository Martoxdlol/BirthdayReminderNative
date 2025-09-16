// data/SampleData.kt
package net.tomascichero.birthdayremainder.data

val sampleBirthdays = List(100) {
    val month = (1..12).random()
    val day = (1..28).random()
    Birthday(it, "Person ${it + 1}", "Month $month, Day $day")
}