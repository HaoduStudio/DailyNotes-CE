package com.haodustudio.DailyNotes.model.models

import android.util.Log
import androidx.room.TypeConverter
import com.google.gson.Gson
import org.json.JSONObject

class TypeConverter {

    private val gson = Gson()

    private data class MoodPayload(val first: Int? = null, val second: String? = null)

    @TypeConverter
    fun jsonToModel(json: String?): Pair<Int, String> {
        if (json.isNullOrBlank()) {
            return Pair(0, "")
        }
        return try {
            val payload = gson.fromJson(json, MoodPayload::class.java)
            Pair(payload?.first ?: 0, payload?.second.orEmpty())
        } catch (err: Exception) {
            Log.w("TypeConverter", "Unable to parse mood payload", err)
            Pair(0, "")
        }
    }

    @TypeConverter
    fun modelToJson(data: Pair<Int, String>?): String {
        val payload = MoodPayload(data?.first, data?.second)
        return gson.toJson(payload)
    }

    @TypeConverter
    fun jsonToModel2(json: String?): HashMap<String, String> {
        if (json.isNullOrBlank() || json == "null") {
            return hashMapOf()
        }
        return try {
            val jsonObject = JSONObject(json)
            val result = HashMap<String, String>(jsonObject.length())
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                result[key] = jsonObject.optString(key)
            }
            result
        } catch (err: Exception) {
            Log.w("TypeConverter", "Unable to parse data payload", err)
            hashMapOf()
        }
    }

    @TypeConverter
    fun modelToJson2(data: HashMap<String, String>?): String {
        if (data.isNullOrEmpty()) {
            return "{}"
        }
        return try {
            val jsonObject = JSONObject()
            data.forEach { (key, value) ->
                jsonObject.put(key, value)
            }
            jsonObject.toString()
        } catch (err: Exception) {
            Log.w("TypeConverter", "Unable to serialize data payload", err)
            "{}"
        }
    }
}