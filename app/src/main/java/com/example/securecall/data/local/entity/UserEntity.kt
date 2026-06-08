package com.example.securecall.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.securecall.domain.model.SyncStatus

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val username: String,
    val name: String,
    val email: String,
    val photoUrl: String?,
    val faceEmbedding: List<Float>?,
    val status: String,
    val lastSeen: Long?,
    val createdAt: Long?,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val updatedAt: Long = System.currentTimeMillis()
)
