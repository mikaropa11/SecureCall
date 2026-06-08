package com.example.securecall.data.repository

import android.util.Log
import com.example.securecall.data.local.dao.UserDao
import com.example.securecall.data.mapper.toDomain
import com.example.securecall.data.mapper.toDto
import com.example.securecall.data.mapper.toEntity
import com.example.securecall.data.mapper.toUserDto
import com.example.securecall.domain.model.SyncStatus
import com.example.securecall.domain.model.User
import com.example.securecall.domain.model.UserStatus
import com.example.securecall.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firebaseFirestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val userDao: UserDao
): UserRepository {
    private val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    override suspend fun isNewUser(userId: String): Result<Boolean> {
        return try {
            val document = firebaseFirestore.collection("users")
                .document(userId)
                .get()
                .await()

            Result.success(!document.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkUsernameAvailable(username: String): Result<Boolean> {
        return try {
            val lowerCaseUsername = username.lowercase().trim()

            val document = firebaseFirestore.collection("usernames")
                .document(lowerCaseUsername)
                .get()
                .await()

            Result.success(!document.exists())
        } catch (e: Exception){
            Result.failure(e)
        }
    }

    override suspend fun saveUserProfile(user: User): Result<Unit> {
        userDao.insertUser(
            user.toEntity().copy(
                syncStatus = SyncStatus.PENDING,
                updatedAt = System.currentTimeMillis()
            )
        )

        return try {
            Log.d("SaveUser", "User received: ${user.toString()}")
            val normalizedUsername = user.username.lowercase().trim()
            val userDto = user.toDto()

            val userData = userDto.toMap().toMutableMap()
            userData["updatedAt"] = FieldValue.serverTimestamp()
            if (userData["createdAt"] == null) {
                userData["createdAt"] = FieldValue.serverTimestamp()
            }
            if (userData["lastSeen"] == null) {
                userData["lastSeen"] = FieldValue.serverTimestamp()
            }

            firebaseFirestore.collection("users")
                .document(user.userId)
                .set(userData)
                .await()


            Log.d("SaveUser", "User saved in users collection")


            val usernameData = hashMapOf(
                "userId" to user.userId,
                "createdAt" to FieldValue.serverTimestamp()
            )

            Log.d("SaveUser", "Username data prepared: $usernameData")

            firebaseFirestore.collection("usernames")
                .document(normalizedUsername)
                .set(usernameData)
                .await()


            Log.d("SaveUser", "Username saved in usernames collection")
            userDao.updateSyncStatus(user.userId, SyncStatus.SYNCED)
            Result.success(Unit)
        } catch (e: Exception) {
            userDao.updateSyncStatus(user.userId, SyncStatus.FAILED)
            Result.failure(e)
        }
    }

    override suspend fun saveEmbedding(embedding: FloatArray?): Result<Unit> {
        return try {

            val userId: FirebaseUser? = currentUser

            if (userId == null) {
                return Result.failure(Exception("User not logged in"))
            }

            val userRef = firebaseFirestore
                .collection("users")
                .document(userId.uid)

            userRef.update(
                "faceEmbedding",
                embedding?.toList()
            ).await()

            Result.success(Unit)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getEmbedding(): Result<FloatArray> {
        return try {
            val user = currentUser
                ?: return Result.failure(Exception("User not logged in"))

            val userRef = firebaseFirestore
                .collection("users")
                .document(user.uid)

            val snapshot = userRef.get().await()

            val raw = snapshot.get("faceEmbedding") as? List<*>
                ?: return Result.failure(Exception("No embedding found"))

            val embedding = raw.map {
                (it as Number).toFloat()
            }.toFloatArray()

            Result.success(embedding)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }


    override suspend fun getUserProfile(userId: String): Result<User> {
        syncPendingUsers()
        val cachedUser = userDao.getUser(userId)?.toDomain()
        return try {
            val document = firebaseFirestore.collection("users")
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val data = document.data ?: return Result.failure(Exception("Empty document"))
                val userDto = data.toUserDto()
                val user = userDto.toDomain(userId)
                userDao.insertUser(user.toEntity().copy(syncStatus = SyncStatus.SYNCED))
                Result.success(user)
            } else {
                cachedUser?.let { Result.success(it) }
                    ?: Result.failure(Exception("User profile not found"))
            }
        } catch (e: Exception) {
            cachedUser?.let { Result.success(it) } ?: Result.failure(e)
        }
    }

    override suspend fun searchUsers(query: String): Result<List<User>> {
        syncPendingUsers()

        return try {

            val snapshot = firebaseFirestore.collection("users")
                .orderBy("usernameLowercase")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .await()

            val users = snapshot.documents.mapNotNull {
                it.toObject(User::class.java)?.copy(userId = it.id)
            }

            users.forEach { user ->
                userDao.insertUser(user.toEntity().copy(syncStatus = SyncStatus.SYNCED))
            }

            Result.success(users)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserStatus(userId: String, status: UserStatus): Result<Unit> {
        syncPendingUsers()
        val now = System.currentTimeMillis()
        val cachedUser = userDao.getUser(userId)
        if (status == UserStatus.offline && cachedUser?.status == UserStatus.in_call.toFirebaseString()) {
            return Result.success(Unit)
        }
        if (cachedUser == null) {
            userDao.insertUser(
                com.example.securecall.data.local.entity.UserEntity(
                    userId = userId,
                    username = currentUser?.displayName.orEmpty(),
                    name = currentUser?.displayName.orEmpty(),
                    email = currentUser?.email.orEmpty(),
                    photoUrl = currentUser?.photoUrl?.toString(),
                    faceEmbedding = null,
                    status = status.toFirebaseString(),
                    lastSeen = now,
                    createdAt = null,
                    syncStatus = SyncStatus.PENDING,
                    updatedAt = now
                )
            )
        } else {
            userDao.updateStatus(
                userId = userId,
                status = status.toFirebaseString(),
                lastSeen = now,
                syncStatus = SyncStatus.PENDING,
                updatedAt = now
            )
        }

        return try {
            firebaseFirestore.collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "status" to status.toFirebaseString(),
                        "lastSeen" to FieldValue.serverTimestamp()
                    )
                )
                .await()

            userDao.updateSyncStatus(userId, SyncStatus.SYNCED)
            Result.success(Unit)
        } catch (e: Exception) {
            userDao.updateSyncStatus(userId, SyncStatus.FAILED)
            Result.failure(e)
        }
    }

    override suspend fun updateFaceEmbedding(userId: String, embedding: List<Float>): Result<Unit> {
        return try {
            firebaseFirestore.collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "faceEmbedding" to embedding.map { it.toDouble() },
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun syncPendingUsers() {
        userDao.getPendingUsers().forEach { entity ->
            runCatching {
                val user = entity.toDomain()
                firebaseFirestore.collection("users")
                    .document(user.userId)
                    .set(user.toDto().toMap())
                    .await()
                userDao.updateSyncStatus(user.userId, SyncStatus.SYNCED)
            }.onFailure {
                Log.e("UserRepository", "Pending user sync failed: ${entity.userId}", it)
            }
        }
    }

}
