package com.example.securecall.domain.repository

import com.example.securecall.domain.model.Call
import kotlinx.coroutines.flow.Flow

interface CallRepository {

    fun getCalls(): Flow<List<Call>>

    suspend fun insertCall(call: Call)
}