package com.example.securecall.data.local.converter

import androidx.room.TypeConverter
import com.example.securecall.domain.model.UserStatus

class Converters {

    @TypeConverter
    fun fromFloatList(list: List<Float>?): String? {
        return list?.joinToString(",")
    }

    @TypeConverter
    fun toFloatList(data: String?): List<Float>? {
        return data?.split(",")?.mapNotNull { it.toFloatOrNull() }
    }

    @TypeConverter
    fun fromStatus(status: UserStatus): String {
        return status.toFirebaseString()
    }

    @TypeConverter
    fun toStatus(status: String): UserStatus {
        return UserStatus.fromString(status)
    }
}