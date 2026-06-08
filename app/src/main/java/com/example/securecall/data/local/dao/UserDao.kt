package com.example.securecall.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.securecall.data.local.entity.UserEntity
import com.example.securecall.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUser(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId")
    fun observeUser(userId: String): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET status = :status, lastSeen = :lastSeen, syncStatus = :syncStatus, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateStatus(
        userId: String,
        status: String,
        lastSeen: Long?,
        syncStatus: SyncStatus,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE users SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateSyncStatus(
        userId: String,
        syncStatus: SyncStatus,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM users WHERE syncStatus != :synced")
    suspend fun getPendingUsers(synced: SyncStatus = SyncStatus.SYNCED): List<UserEntity>
}
