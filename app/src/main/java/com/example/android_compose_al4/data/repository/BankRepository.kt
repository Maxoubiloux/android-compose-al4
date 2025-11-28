package com.example.android_compose_al4.data.repository

import android.content.Context
import com.example.android_compose_al4.data.api.ApiService
import com.example.android_compose_al4.data.api.TransactionRequest
import com.example.android_compose_al4.data.api.UserResponse
import com.example.android_compose_al4.data.model.Transaction
import com.example.android_compose_al4.data.model.User
import com.example.android_compose_al4.data.auth.SessionManager
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

interface BankRepository {
    suspend fun getTransactions(): Flow<List<Transaction>>
    suspend fun getUserProfile(): Flow<User?>
    suspend fun addTransaction(transaction: Transaction): Flow<Result<Transaction>>
    suspend fun deleteTransaction(transactionId: String): Flow<Result<Boolean>>
}

@Singleton
class BankRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val appContext: Context
) : BankRepository {
    
    override suspend fun getTransactions(): Flow<List<Transaction>> = flow {
        try {
            val response = apiService.getTransactions()
            if (response.isSuccessful) {
                val items = response.body()?.map { it.toTransaction() } ?: emptyList()
                emit(items)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    override suspend fun getUserProfile(): Flow<User?> = flow {
        try {
            val response = apiService.getUserProfile()
            if (response.isSuccessful) {
                emit(response.body()?.toUser())
                return@flow
            }
        } catch (_: Exception) {
        }
        val user = loadUserFromAssets()
        emit(user ?: createDummyUser())
    }
    
    override suspend fun addTransaction(transaction: Transaction): Flow<Result<Transaction>> = flow {
        try {
            val request = TransactionRequest(
                title = transaction.title,
                amount = transaction.amount,
                categoryId = transaction.category.id,
                type = transaction.type.name
            )
            val response = apiService.addTransaction(request)
            if (response.isSuccessful) {
                val body = response.body()?.toTransaction()
                if (body != null) {
                    emit(Result.success(body))
                } else {
                    emit(Result.failure(Exception("Empty body")))
                }
            } else {
                emit(Result.failure(Exception("Failed to add transaction")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    override suspend fun deleteTransaction(transactionId: String): Flow<Result<Boolean>> = flow {
        try {
            val response = apiService.deleteTransaction(transactionId)
            if (response.isSuccessful) {
                emit(Result.success(true))
            } else {
                emit(Result.failure(Exception("Failed to delete transaction")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    private fun createDummyUser(): User {
        return User(
            id = "1",
            name = "John Doe",
            email = "john.doe@example.com",
            phone = "+33612345678",
            balance = 1250.50,
            accountNumber = "FR76 3000 1007 1600 0000 0000 123"
        )
    }

    private fun loadUserFromAssets(): User? {
        return try {
            val json = appContext.assets.open("mock_data.json").bufferedReader().use { it.readText() }
            val data = Gson().fromJson(json, MockDataProfiles::class.java)
            val email = SessionManager.currentEmail
            val profile = when {
                email != null -> data.profiles?.firstOrNull { it.email.equals(email, ignoreCase = true) }
                else -> data.profiles?.firstOrNull()
            }
            profile?.toUser()
        } catch (e: Exception) {
            null
        }
    }

    private data class MockDataProfiles(
        @SerializedName("profiles") val profiles: List<UserResponse>? = null
    )
}
