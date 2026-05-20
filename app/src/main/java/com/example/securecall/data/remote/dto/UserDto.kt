package com.example.securecall.data.remote.dto

import com.google.firebase.Timestamp

data class UserDto(
    val userId: String = "",
    val username: String = "",
    val usernameLowercase: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val faceEmbedding: List<Double>? = null,
    val status: String = "offline",
    val lastSeen: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "username" to username,
            "usernameLowercase" to usernameLowercase,
            "name" to name,
            "email" to email,
            "photoUrl" to photoUrl,
            "faceEmbedding" to faceEmbedding,
            "status" to status,
            "lastSeen" to lastSeen,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }
}
