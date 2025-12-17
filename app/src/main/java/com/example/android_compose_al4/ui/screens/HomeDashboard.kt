package com.example.android_compose_al4.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.android_compose_al4.ui.components.TransactionItem
import com.example.android_compose_al4.viewmodel.BankViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboard(
    viewModel: BankViewModel,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToTransfer: () -> Unit = {}
) {
    val uiState = viewModel.uiState.value
    val transactions = uiState.transactions
    val currentAccount by viewModel.currentAccount.collectAsState(initial = null)
    
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    
    val onDismissDialog = { showAddTransactionDialog = false }

    val sortedTransactions = remember(transactions) {
        transactions.sortedByDescending { parseTransactionDateOrNow(it.date).time }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Solde disponible",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = formatCurrency(currentAccount?.balance ?: 0.0),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ActionButton(
                        text = "Ajouter",
                        icon = Icons.Default.Add,
                        onClick = { showAddTransactionDialog = true }
                    )
                    ActionButton(
                        text = "Envoyer",
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        onClick = onNavigateToTransfer
                    )
                    ActionButton(
                        text = "Dépenser",
                        icon = Icons.Default.Payment,
                        onClick = { }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dernières transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            TextButton(onClick = onNavigateToHistory) {
                Text("Voir tout")
            }
        }
        
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Aucune transaction récente")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sortedTransactions.take(5)) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        onTransactionClick = { }
                    )
                }
            }
        }
    }

    if (showAddTransactionDialog) {
        AddTransactionDialog(
            onDismiss = onDismissDialog,
            onConfirm = onDismissDialog
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter une transaction") },
        text = {
            Text("Formulaire d'ajout de transaction à implémenter")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
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
        SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.FRANCE).parse(raw)
    } catch (_: Exception) {
        null
    }
    if (javaDateToString != null) return javaDateToString

    return Date()
}
