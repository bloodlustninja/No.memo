package com.example.nomemo.data.db

import androidx.room.TypeConverter
import com.example.nomemo.data.model.ChecklistItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        if (value == null) return "[]"
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        val listType = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(value, listType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromFloatList(value: List<Float>?): String {
        if (value == null) return "[]"
        return gson.toJson(value)
    }

    @TypeConverter
    fun toFloatList(value: String?): List<Float> {
        if (value.isNullOrBlank()) return emptyList()
        val listType = object : TypeToken<List<Float>>() {}.type
        return try {
            gson.fromJson(value, listType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromChecklistItemsList(value: List<ChecklistItem>?): String {
        if (value == null) return "[]"
        return gson.toJson(value)
    }

    @TypeConverter
    fun toChecklistItemsList(value: String?): List<ChecklistItem> {
        if (value.isNullOrBlank()) return emptyList()
        val listType = object : TypeToken<List<ChecklistItem>>() {}.type
        return try {
            gson.fromJson(value, listType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
