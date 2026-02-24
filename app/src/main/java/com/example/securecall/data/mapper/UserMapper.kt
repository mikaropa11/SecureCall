package com.example.securecall.data.mapper

import com.example.securecall.data.remote.dto.UserDto
import com.example.securecall.domain.model.User
import com.example.securecall.domain.model.UserStatus
import com.google.firebase.Timestamp

fun UserDto.toDomain(userId: String): User {
    return User(
        userId = userId,
        username = username,
        name = name,
        email = email,
        photoUrl = photoUrl,
        faceEmbedding = faceEmbedding?.map { it.toFloat() },
        status = UserStatus.fromString(status),
        lastSeen = lastSeen?.seconds?.times(1000),
        createdAt = createdAt?.seconds?.times(1000)
    )
}

fun User.toDto(): UserDto {
    return UserDto(
        username = username,
        name = name,
        email = email,
        photoUrl = photoUrl,
        faceEmbedding = faceEmbedding?.map { it.toDouble() },
        status = status.toFirebaseString(),
        lastSeen = lastSeen?.let { Timestamp(it / 1000, 0) },
        createdAt = createdAt?.let { Timestamp(it / 1000, 0) }
    )
}

/**
 * Maps a firestore doc to UserDto
 */
fun Map<String, Any>.toUserDto(): UserDto {
    return UserDto(
        username = this["username"] as? String ?: "",
        name = this["name"] as? String ?: "",
        email = this["email"] as? String ?: "",
        photoUrl = this["photoUrl"] as? String,
        faceEmbedding = (this["faceEmbedding"] as? List<*>)?.mapNotNull {
            when (it) {
                is Double -> it
                is Number -> it.toDouble()
                else -> null
            }
        },  // ← Conversión segura
        status = this["status"] as? String ?: "offline",
        lastSeen = this["lastSeen"] as? Timestamp,
        createdAt = this["createdAt"] as? Timestamp,
        updatedAt = this["updatedAt"] as? Timestamp
    )
}
