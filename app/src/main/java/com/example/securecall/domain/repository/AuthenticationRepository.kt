package com.example.securecall.domain.repository

import com.example.securecall.domain.model.User
import com.example.securecall.domain.model.UserStatus
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential

/**
 * The Authentication Repository
 */
interface AuthenticationRepository {

    /**
     * Current User
     * Type FirebaseUser
     */
    val currentUser: FirebaseUser?

    /**
     * isUserAuthenticated
     *
     * @return  a boolean indicating if it's authenticated
     */
    fun isUserAuthenticated(): Boolean

    /**
     * signUpWithEmail
     *
     * @return  the FirebaseUser encapsulated
     */
    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser>

    /**
     * loginWithEmail
     *
     * @return  the FirebaseUser encapsulated
     */
    suspend fun loginWithEmail(email: String, password: String): Result<FirebaseUser>

    /**
     * verifyPhoneCredential
     *
     * @return  the FirebaseUser encapsulated
     */
    suspend fun verifyPhoneCredential(credential: PhoneAuthCredential): Result<FirebaseUser>

    /**
     * logOut
     */
    fun logOut()

    /**
     * recoverPassword
     */
    suspend fun recoverPassword(email: String): Result<Unit>

    //Firestore

    suspend fun isNewUser(userId: String): Result<Boolean>
    suspend fun checkUsernameAvailable(username: String): Result<Boolean>
    suspend fun saveUserProfile(user: User): Result<Unit>
    suspend fun getUserProfile(userId: String): Result<User>
    suspend fun updateUserStatus(userId: String, status: UserStatus): Result<Unit>
    suspend fun updateFaceEmbedding(userId: String, embedding: List<Float>): Result<Unit>




}