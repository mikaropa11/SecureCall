package com.example.securecall.domain.repository

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


}