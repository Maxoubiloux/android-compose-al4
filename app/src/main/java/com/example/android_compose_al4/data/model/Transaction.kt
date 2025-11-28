package com.example.android_compose_al4.data.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val date: String,
    val category: TransactionCategory,
    val type: TransactionType = if (amount >= 0) TransactionType.INCOME else TransactionType.EXPENSE
)

enum class TransactionType {
    INCOME,
    EXPENSE
}

data class TransactionCategory(
    val id: Int,
    val name: String,
    val icon: String,
    val color: Color
)

val transactionCategories = listOf(
    TransactionCategory(1, "Nourriture", "restaurant", Color(0xFFFF6B6B)),
    TransactionCategory(2, "Transport", "directions_car", Color(0xFF4ECDC4)),
    TransactionCategory(3, "Shopping", "shopping_bag", Color(0xFF45B7D1)),
    TransactionCategory(4, "Loisirs", "sports_esports", Color(0xFF96CEB4)),
    TransactionCategory(5, "Salaire", "account_balance_wallet", Color(0xFFFFEEAD)),
    TransactionCategory(6, "Autre", "more_horiz", Color(0xFFFFD166))
)
