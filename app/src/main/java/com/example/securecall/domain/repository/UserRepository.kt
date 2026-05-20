package com.example.securecall.domain.repository

import com.example.securecall.domain.model.User
import com.example.securecall.domain.model.UserStatus

interface UserRepository {

    suspend fun isNewUser(userId: String): Result<Boolean>
    suspend fun checkUsernameAvailable(username: String): Result<Boolean>
    suspend fun saveUserProfile(user: User): Result<Unit>
    suspend fun saveEmbedding(embedding: FloatArray?): Result<Unit>
    suspend fun getEmbedding(): Result<FloatArray>
    suspend fun getUserProfile(userId: String): Result<User>
    suspend fun searchUsers(query: String): Result<List<User>>
    suspend fun updateUserStatus(userId: String, status: UserStatus): Result<Unit>
    suspend fun updateFaceEmbedding(userId: String, embedding: List<Float>): Result<Unit>
}