package com.example.securecall.domain.model

data class User(
    val userId: String,
    val username: String,
    val name: String,
    val email: String,
    val photoUrl: String? = null,
    val faceEmbedding: List<Float>? = null,
    val status: UserStatus = UserStatus.OFFLINE,
    val lastSeen: Long? = null,
    val createdAt: Long? = null
)

enum class UserStatus {
    ONLINE,
    OFFLINE,
    IN_CALL;

    companion object {
        fun fromString(status: String): UserStatus {
            return when (status.lowercase()) {
                "online" -> ONLINE
                "in_call" -> IN_CALL
                else -> OFFLINE
            }
        }
    }

    fun toFirebaseString(): String {
        return when (this) {
            ONLINE -> "online"
            OFFLINE -> "offline"
            IN_CALL -> "in_call"
        }
    }
}
