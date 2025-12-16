package com.example.android_compose_al4.data.model

import androidx.compose.ui.graphics.Color

data class BankAccount(
    val id: String? = null,
    val accountNumber: String,
    val accountName: String,
    val balance: Double,
    val type: AccountType,
    val currency: String = "EUR",
    val color: Color = Color(0xFF6200EE)
) {
    val maxDeposit: Double
        get() = when (type) {
            AccountType.LIVRET_A -> 22950.0
            AccountType.LIVRET_JEUNE -> 1600.0
            AccountType.PEL -> 61200.0
            else -> Double.MAX_VALUE
        }
    enum class AccountType {
        CURRENT,
        LIVRET_A,
        LIVRET_JEUNE,
        PEL
    }
}

fun List<BankAccount>.toUserAccounts(): UserAccounts {
    return UserAccounts(
        currentAccount = find { it.type == BankAccount.AccountType.CURRENT },
        livretA = find { it.type == BankAccount.AccountType.LIVRET_A },
        livretJeune = find { it.type == BankAccount.AccountType.LIVRET_JEUNE },
        pel = find { it.type == BankAccount.AccountType.PEL }
    )
}
