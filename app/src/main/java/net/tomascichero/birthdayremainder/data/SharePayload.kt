package net.tomascichero.birthdayremainder.data

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

data class ShareableBirthday(
    val name: String,
    val month: Int,
    val day: Int,
    val year: Int? = null,
    val notes: String = ""
)

object ShareUtils {
    private const val BASE_URL = "https://birthday-remainder-app.web.app/share"

    fun encodeShareUrl(birthdays: List<Birthday>): String {
        val arr = JSONArray()
        for (b in birthdays) {
            val obj = JSONObject()
            obj.put("name", b.personName)
            obj.put("month", b.birth.monthValue)
            obj.put("day", b.birth.dayOfMonth)
            if (!b.noYear) obj.put("year", b.birth.year)
            if (b.notes.isNotBlank()) obj.put("notes", b.notes)
            arr.put(obj)
        }
        val json = arr.toString()
        val encoded = Base64.encodeToString(json.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
        return "$BASE_URL#$encoded"
    }

    fun decodeShareUrl(url: String): List<ShareableBirthday> {
        val uri = android.net.Uri.parse(url)
        val data = uri.fragment ?: uri.getQueryParameter("data") ?: return emptyList()
        return try {
            val json = String(Base64.decode(data, Base64.URL_SAFE or Base64.NO_WRAP))
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                ShareableBirthday(
                    name = obj.optString("name", ""),
                    month = obj.optInt("month", 1),
                    day = obj.optInt("day", 1),
                    year = if (obj.has("year")) obj.getInt("year") else null,
                    notes = obj.optString("notes", "")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
