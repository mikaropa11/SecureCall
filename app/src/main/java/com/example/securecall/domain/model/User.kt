package com.example.securecall.domain.model

import com.google.firebase.Timestamp

data class User(
    val userId: String = "",
    val username: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val faceEmbedding: List<Float>? = null,
    val status: UserStatus = UserStatus.offline,
    val lastSeen: Timestamp? = null,
    val createdAt: Timestamp? = null
)

enum class UserStatus {
    online,
    offline,
    in_call;

    companion object {
        fun fromString(status: String): UserStatus {
            return when (status.lowercase()) {
                "online" -> online
                "in_call" -> in_call
                else -> offline
            }
        }
    }

    fun toFirebaseString(): String {
        return when (this) {
            online -> "online"
            offline -> "offline"
            in_call -> "in_call"
        }
    }
}
