package com.example.android_compose_al4.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.android_compose_al4.data.model.BankAccount
import com.example.android_compose_al4.ui.components.TransactionItem
import com.example.android_compose_al4.viewmodel.BankViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Locale
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    viewModel: BankViewModel,
    accountId: String,
    onBackClick: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState(initial = emptyList())
    val currentAccount by viewModel.currentAccount.collectAsState(initial = null)
    val transactions = viewModel.uiState.value.transactions
    val account = remember(accounts, currentAccount, accountId) {
        (accounts + listOfNotNull(currentAccount)).firstOrNull { it.id == accountId }
    }

    var showCloseConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(account, accounts) {
        if (account == null && accounts.isNotEmpty()) {
            onBackClick()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text(account?.accountName ?: "Compte") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour"
                    )
                }
            },
            actions = {
                val canClose = account?.type != null && account.type != BankAccount.AccountType.CURRENT
                if (canClose) {
                    TextButton(onClick = { showCloseConfirm = true }) {
                        Text("Clôturer")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (account == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chargement du compte...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            return
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            tonalElevation = 1.dp,
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = account.accountName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = account.accountNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCurrency(account.balance),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (account.type != BankAccount.AccountType.CURRENT) {
                    Text(
                        text = "Plafond: ${formatCurrency(account.maxDeposit)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val accountTransactions = remember(transactions, accountId) {
            transactions
                .filter { it.accountId == accountId }
                .sortedByDescending { parseTransactionDateOrNow(it.date).time }
        }

        Text(
            text = "Historique",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (accountTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Aucune transaction pour ce compte")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(accountTransactions) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        onTransactionClick = { }
                    )
                }
            }
        }
    }

    if (showCloseConfirm) {
        AlertDialog(
            onDismissRequest = { showCloseConfirm = false },
            title = { Text("Clôturer le compte") },
            text = {
                Text("Le solde sera transféré vers le compte courant, puis le compte sera supprimé.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCloseConfirm = false
                        if (account != null) {
                            viewModel.closeAccount(account)
                        }
                    }
                ) {
                    Text("Clôturer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseConfirm = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.FRANCE)
    format.maximumFractionDigits = 2
    format.currency = Currency.getInstance("EUR")
    return format.format(amount)
}

private fun parseTransactionDateOrNow(raw: String): Date {
    val iso = try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(raw)
    } catch (_: Exception) {
        null
    }
    if (iso != null) return iso

    val javaDateToString = try {
        SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH).parse(raw)
    } catch (_: Exception) {
        null
    }
    if (javaDateToString != null) return javaDateToString

    return Date()
}
