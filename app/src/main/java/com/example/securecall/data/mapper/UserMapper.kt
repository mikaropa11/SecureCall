package com.example.securecall.data.mapper

import com.example.securecall.data.local.entity.UserEntity
import com.example.securecall.data.remote.dto.UserDto
import com.example.securecall.domain.model.User
import com.example.securecall.domain.model.UserStatus
import com.google.firebase.Timestamp
import java.util.Date

fun UserDto.toDomain(userId: String): User {
    return User(
        userId = userId,
        username = username,
        name = name,
        email = email,
        photoUrl = photoUrl,
        faceEmbedding = faceEmbedding?.map { it.toFloat() },
        status = UserStatus.fromString(status),
        lastSeen = lastSeen,
        createdAt = createdAt
    )
}

fun UserEntity.toDomain(): User {
    return User(
        userId = userId,
        username = username,
        name = name,
        email = email,
        photoUrl = photoUrl,
        faceEmbedding = faceEmbedding,
        status = UserStatus.fromString(status),
        lastSeen = lastSeen?.let { Timestamp(Date(it)) },
        createdAt = createdAt?.let { Timestamp(Date(it)) }
    )
}

fun User.toEntity(): UserEntity {
    return UserEntity(
        userId = userId,
        username = username,
        name = name,
        email = email,
        photoUrl = photoUrl,
        faceEmbedding = faceEmbedding,
        status = status.toFirebaseString(),
        lastSeen = lastSeen?.toDate()?.time,
        createdAt = createdAt?.toDate()?.time
    )
}

fun User.toDto(): UserDto {
    return UserDto(
        userId = userId,
        username = username,
        usernameLowercase = username.lowercase(),
        name = name,
        email = email,
        photoUrl = photoUrl,
        faceEmbedding = faceEmbedding?.map { it.toDouble() },
        status = status.toFirebaseString(),
        lastSeen = lastSeen,
        createdAt = createdAt
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
