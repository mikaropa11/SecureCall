package com.example.securecall.data.repository

import com.example.securecall.domain.repository.AuthenticationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential

import kotlinx.coroutines.tasks.await
import org.webrtc.PeerConnection
import javax.inject.Inject

class AuthenticationRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
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

}