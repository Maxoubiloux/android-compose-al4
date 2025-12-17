package com.example.android_compose_al4.data.repository

import android.content.Context
import androidx.core.graphics.toColorInt
import com.example.android_compose_al4.data.api.ApiService
import com.example.android_compose_al4.data.api.AccountUpdateRequest
import com.example.android_compose_al4.data.api.AccountCreateRequest
import com.example.android_compose_al4.data.api.CategoryRequest
import com.example.android_compose_al4.data.api.TransactionRequest
import com.example.android_compose_al4.data.api.UserResponse
import com.example.android_compose_al4.data.model.Transaction
import com.example.android_compose_al4.data.model.User
import com.example.android_compose_al4.data.auth.SessionManager
import com.example.android_compose_al4.data.model.BankAccount
import com.example.android_compose_al4.data.model.BankAccount.AccountType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

interface BankRepository {
    suspend fun getTransactions(): Flow<List<Transaction>>
    suspend fun getUserProfile(): Flow<User?>
    suspend fun addTransaction(transaction: Transaction): Flow<Result<Transaction>>
    suspend fun deleteTransaction(transactionId: String): Flow<Result<Boolean>>
    suspend fun updateAccounts(accounts: List<BankAccount>): Flow<Result<Unit>>
    suspend fun getAccounts(): Flow<List<BankAccount>>
    suspend fun getCurrentAccount(): Flow<BankAccount?>

    suspend fun createAccount(account: BankAccount): Flow<Result<BankAccount>>
    suspend fun deleteAccount(accountId: String): Flow<Result<Boolean>>
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
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emit(emptyList())
        }
    }
    
    override suspend fun getUserProfile(): Flow<User?> = flow {
        try {
            val email = SessionManager.currentEmail
            val response = apiService.getProfiles(email = email)
            if (response.isSuccessful) {
                val profile = response.body()?.firstOrNull()
                if (profile != null) {
                    emit(profile.toUser())
                    return@flow
                }
            }
        } catch (e: CancellationException) {
            throw e
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
                date = transaction.date,
                category = CategoryRequest(
                    id = transaction.category.id,
                    name = transaction.category.name,
                    icon = transaction.category.icon,
                    color = toHexColor(transaction.category.color)
                ),
                type = transaction.type.name,
                accountId = transaction.accountId
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
        } catch (e: CancellationException) {
            throw e
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    override suspend fun updateAccounts(accounts: List<BankAccount>): Flow<Result<Unit>> = flow {
        try {
            for (account in accounts) {
                val accountId = account.id
                if (accountId.isNullOrBlank()) {
                    emit(Result.failure(IllegalStateException("Account id is required to update account")))
                    return@flow
                }
                val response = apiService.updateAccount(
                    id = accountId,
                    request = AccountUpdateRequest(balance = account.balance)
                )
                if (!response.isSuccessful) {
                    emit(Result.failure(Exception("Failed to update account")))
                    return@flow
                }
            }
            emit(Result.success(Unit))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override suspend fun getAccounts(): Flow<List<BankAccount>> = flow {
        try {
            val response = apiService.getAccounts()
            if (response.isSuccessful) {
                val accounts = response.body()?.mapNotNull { it.toBankAccountOrNull() } ?: emptyList()
                emit(accounts)
            } else {
                emit(emptyList())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun getCurrentAccount(): Flow<BankAccount?> = flow {
        try {
            val response = apiService.getAccounts()
            if (response.isSuccessful) {
                val accounts = response.body()?.mapNotNull { it.toBankAccountOrNull() } ?: emptyList()
                emit(accounts.find { it.type == AccountType.CURRENT })
            } else {
                emit(null)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emit(null)
        }
    }

    override suspend fun createAccount(account: BankAccount): Flow<Result<BankAccount>> = flow {
        try {
            val request = AccountCreateRequest(
                accountNumber = account.accountNumber,
                accountName = account.accountName,
                balance = account.balance,
                type = accountTypeToApiType(account.type),
                currency = account.currency,
                color = toHexColor(account.color)
            )

            val response = apiService.createAccount(request)
            if (response.isSuccessful) {
                val created = response.body()?.toBankAccountOrNull()
                if (created != null) {
                    emit(Result.success(created))
                } else {
                    emit(Result.failure(Exception("Empty body")))
                }
            } else {
                emit(Result.failure(Exception("Failed to create account")))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override suspend fun deleteAccount(accountId: String): Flow<Result<Boolean>> = flow {
        try {
            val response = apiService.deleteAccount(accountId)
            if (response.isSuccessful) {
                emit(Result.success(true))
            } else {
                emit(Result.failure(Exception("Failed to delete account")))
            }
        } catch (e: CancellationException) {
            throw e
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
            accounts = listOf(
                BankAccount(
                    accountNumber = "FR76 3000 1007 1600 0000 0000 123",
                    accountName = "Compte Courant",
                    balance = 1250.50,
                    type = AccountType.CURRENT
                )
            )
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
        } catch (_: Exception) {
            null
        }
    }

    private fun com.example.android_compose_al4.data.api.AccountResponse.toBankAccountOrNull(): BankAccount? {
        val type = typeToAccountTypeOrNull(type) ?: return null
        val parsedColor = try {
            val hex = color ?: "#6200EE"
            Color(hex.toColorInt())
        } catch (_: Exception) {
            Color(0xFF6200EE)
        }

        return BankAccount(
            id = id.toString(),
            accountNumber = accountNumber,
            accountName = accountName,
            balance = balance,
            type = type,
            currency = currency ?: "EUR",
            color = parsedColor
        )
    }

    private fun typeToAccountTypeOrNull(raw: String): AccountType? {
        return when (raw.uppercase()) {
            "CURRENT", "CURRENT_ACCOUNT" -> AccountType.CURRENT
            "LIVRET_A" -> AccountType.LIVRET_A
            "LIVRET_JEUNE" -> AccountType.LIVRET_JEUNE
            "PEL" -> AccountType.PEL
            else -> null
        }
    }

    private fun accountTypeToApiType(type: AccountType): String {
        return when (type) {
            AccountType.CURRENT -> "CURRENT_ACCOUNT"
            else -> type.name
        }
    }

    private fun toHexColor(color: Color): String {
        val argb = color.toArgb()
        val rgb = argb and 0x00FFFFFF
        return String.format("#%06X", rgb)
    }

    private data class MockDataProfiles(
        @SerializedName("profiles") val profiles: List<UserResponse>? = null
    )
}
