package com.example.securecall.data.repository

import com.example.securecall.data.local.dao.CallDao
import com.example.securecall.data.mapper.toDomain
import com.example.securecall.data.mapper.toEntity
import com.example.securecall.domain.model.Call
import com.example.securecall.domain.repository.CallRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CallRepositoryImpl @Inject constructor(
    private val callDao: CallDao
) : CallRepository {

    override fun getCalls(): Flow<List<Call>> =
        callDao.getCalls().map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun insertCall(call: Call) {
        callDao.insertCall(call.toEntity())
    }
}