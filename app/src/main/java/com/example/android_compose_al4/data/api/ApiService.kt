package com.example.android_compose_al4.data.api

import com.example.android_compose_al4.data.model.Transaction
import com.example.android_compose_al4.data.model.TransactionCategory
import com.example.android_compose_al4.data.model.TransactionType
import com.example.android_compose_al4.data.model.User
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @GET("db")
    suspend fun getData(): Response<ApiResponse>
    
    @GET("transactions")
    suspend fun getTransactions(): Response<List<TransactionResponse>>
    
    @GET("profile")
    suspend fun getUserProfile(): Response<UserResponse>
    
    @POST("transactions")
    suspend fun addTransaction(@Body transaction: TransactionRequest): Response<TransactionResponse>
    
    @DELETE("transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: String): Response<Unit>
}

data class ApiResponse(
    @SerializedName("transactions") val transactions: List<TransactionResponse>,
    @SerializedName("profile") val profile: UserResponse
)

data class TransactionResponse(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("date") val date: String,
    @SerializedName("category") val category: CategoryResponse,
    @SerializedName("type") val type: String
) {
    fun toTransaction(): Transaction {
        return Transaction(
            id = id,
            title = title,
            amount = amount,
            date = date,
            category = TransactionCategory(
                id = category.id,
                name = category.name,
                icon = category.icon,
                color = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(category.color))
            ),
            type = enumValueOf<TransactionType>(type)
        )
    }
}

data class TransactionRequest(
    @SerializedName("title") val title: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("categoryId") val categoryId: Int,
    @SerializedName("type") val type: String
)

data class CategoryResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("icon") val icon: String,
    @SerializedName("color") val color: String
)

data class UserResponse(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("balance") val balance: Double,
    @SerializedName("accountNumber") val accountNumber: String
) {
    fun toUser(): User {
        return User(
            id = id,
            name = name,
            email = email,
            phone = phone,
            balance = balance,
            accountNumber = accountNumber
        )
    }
}
