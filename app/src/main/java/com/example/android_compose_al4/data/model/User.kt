package com.example.android_compose_al4.data.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val accounts: List<BankAccount>,
    val profilePicture: String? = null
)

data class UserAccounts(
    val currentAccount: BankAccount?,
    val livretA: BankAccount?,
    val livretJeune: BankAccount?,
    val pel: BankAccount?
)
