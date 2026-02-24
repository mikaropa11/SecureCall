package com.example.securecall.data.repository

import android.util.Log
import com.example.securecall.data.mapper.toDomain
import com.example.securecall.data.mapper.toDto
import com.example.securecall.data.mapper.toUserDto
import com.example.securecall.domain.model.User
import com.example.securecall.domain.model.UserStatus
import com.example.securecall.domain.repository.AuthenticationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthenticationRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFirestore: FirebaseFirestore
): AuthenticationRepository {


    override val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    override fun isUserAuthenticated(): Boolean {
        return currentUser != null
    }

    override suspend fun signUpWithEmail(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            return Result.success(result.user!!)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun loginWithEmail(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            return Result.success(result.user!!)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun verifyPhoneCredential(credential: PhoneAuthCredential): Result<FirebaseUser> {
        try {
            val result = firebaseAuth.signInWithCredential(credential).await()
            return Result.success(result.user!!)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override fun logOut() {
        firebaseAuth.signOut()
    }

    override suspend fun recoverPassword(email: String): Result<Unit> {
        try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    //Firestore
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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserProfile(userId: String): Result<User> {
        return try {
            val document = firebaseFirestore.collection("users")
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val data = document.data ?: return Result.failure(Exception("Empty document"))
                val userDto = data.toUserDto()
                Result.success(userDto.toDomain(userId))
            } else {
                Result.failure(Exception("User profile not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserStatus(userId: String, status: UserStatus): Result<Unit> {
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

            Result.success(Unit)
        } catch (e: Exception) {
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
}