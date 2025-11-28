package com.example.android_compose_al4.data.model

import androidx.compose.ui.graphics.Color

data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val balance: Double,
    val accountNumber: String,
    val profilePicture: String? = null
)

data class BankAccount(
    val accountNumber: String,
    val accountName: String,
    val balance: Double,
    val currency: String = "EUR",
    val color: Color = Color(0xFF6200EE)
)
