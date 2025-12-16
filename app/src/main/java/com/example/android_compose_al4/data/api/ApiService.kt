package com.example.android_compose_al4.data.api

import com.example.android_compose_al4.data.model.BankAccount
import com.example.android_compose_al4.data.model.Transaction
import com.example.android_compose_al4.data.model.TransactionCategory
import com.example.android_compose_al4.data.model.TransactionType
import com.example.android_compose_al4.data.model.User
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*
import androidx.core.graphics.toColorInt

interface ApiService {
    @GET("transactions")
    suspend fun getTransactions(): Response<List<TransactionResponse>>

    @POST("transactions")
    suspend fun addTransaction(@Body transaction: TransactionRequest): Response<TransactionResponse>
    
    @DELETE("transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: String): Response<Unit>
    
    @GET("accounts")
    suspend fun getAccounts(): Response<List<AccountResponse>>

    @POST("accounts")
    suspend fun createAccount(@Body request: AccountCreateRequest): Response<AccountResponse>
    
    @PATCH("accounts/{id}")
    suspend fun updateAccount(
        @Path("id") id: String,
        @Body request: AccountUpdateRequest
    ): Response<AccountResponse>

    @DELETE("accounts/{id}")
    suspend fun deleteAccount(@Path("id") id: String): Response<Unit>
    
    @GET("profiles")
    suspend fun getProfiles(@Query("email") email: String? = null): Response<List<UserResponse>>
}

data class TransactionResponse(
    @SerializedName("id") val id: Any,
    @SerializedName("title") val title: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("date") val date: String,
    @SerializedName("category") val category: CategoryResponse,
    @SerializedName("type") val type: String,
    @SerializedName("accountId") val accountId: String? = null
) {
    fun toTransaction(): Transaction {
        return Transaction(
            id = id.toString(),
            title = title,
            amount = amount,
            date = date,
            category = TransactionCategory(
                id = category.id,
                name = category.name,
                icon = category.icon,
                color = androidx.compose.ui.graphics.Color(category.color.toColorInt())
            ),
            type = enumValueOf<TransactionType>(type),
            accountId = accountId
        )
    }
}

data class TransactionRequest(
    @SerializedName("title") val title: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("date") val date: String,
    @SerializedName("category") val category: CategoryRequest,
    @SerializedName("type") val type: String,
    @SerializedName("accountId") val accountId: String? = null
)

data class AccountResponse(
    @SerializedName("id") val id: Any,
    @SerializedName("accountNumber") val accountNumber: String,
    @SerializedName("accountName") val accountName: String,
    @SerializedName("balance") val balance: Double,
    @SerializedName("type") val type: String,
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("color") val color: String? = null
)

data class AccountCreateRequest(
    @SerializedName("accountNumber") val accountNumber: String,
    @SerializedName("accountName") val accountName: String,
    @SerializedName("balance") val balance: Double,
    @SerializedName("type") val type: String,
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("color") val color: String? = null
)

data class AccountUpdateRequest(
    @SerializedName("balance") val balance: Double
)

data class CategoryResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("icon") val icon: String,
    @SerializedName("color") val color: String
)

data class CategoryRequest(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("icon") val icon: String,
    @SerializedName("color") val color: String
)

data class UserResponse(
    @SerializedName("id") val id: Any,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("balance") val balance: Double,
    @SerializedName("accountNumber") val accountNumber: String
) {
    fun toUser(): User {
        val defaultAccount = BankAccount(
            accountNumber = accountNumber,
            accountName = "Compte Courant",
            balance = balance,
            type = BankAccount.AccountType.CURRENT
        )
        
        return User(
            id = id.toString(),
            name = name,
            email = email,
            phone = phone,
            accounts = listOf(defaultAccount)
        )
    }
}
