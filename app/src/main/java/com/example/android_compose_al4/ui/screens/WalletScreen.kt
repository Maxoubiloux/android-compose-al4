package com.example.android_compose_al4.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.android_compose_al4.data.model.BankAccount
import com.example.android_compose_al4.viewmodel.BankViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    viewModel: BankViewModel,
    onAccountClick: (BankAccount) -> Unit = {}
) {
    val accounts by viewModel.accounts.collectAsState(initial = emptyList())
    val currentAccount by viewModel.currentAccount.collectAsState(initial = null)

    val displayedAccounts = remember(accounts, currentAccount) {
        val currentId = currentAccount?.id
        buildList {
            currentAccount?.let { add(it) }
            addAll(accounts.filter { !it.id.isNullOrBlank() && it.id != currentId })
        }
    }

    var showCreateAccountDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf<BankAccount.AccountType?>(null) }
    var initialDepositInput by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        viewModel.loadUserData()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mes comptes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            TextButton(onClick = { showCreateAccountDialog = true }) {
                Text("Souscrire")
            }
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = displayedAccounts,
                key = { account -> account.id ?: account.accountNumber }
            ) { account ->
                AccountCard(
                    account = account,
                    onClick = { onAccountClick(account) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Cartes",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Revolut",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = viewModel.uiState.value.user?.name?.uppercase() ?: "JOHN DOE",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "••••  ••••  ••••  1234",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    
                    Text(
                        text = "VISA",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showCreateAccountDialog) {
        val availableTypes = remember(accounts) {
            listOf(
                BankAccount.AccountType.LIVRET_A,
                BankAccount.AccountType.LIVRET_JEUNE,
                BankAccount.AccountType.PEL
            ).filter { type -> accounts.none { it.type == type } }
        }

        if (selectedType == null && availableTypes.isNotEmpty()) {
            selectedType = availableTypes.first()
        }

        AlertDialog(
            onDismissRequest = {
                showCreateAccountDialog = false
                selectedType = null
                initialDepositInput = ""
            },
            title = { Text("Souscrire à un compte") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (availableTypes.isEmpty()) {
                        Text("Tous les comptes d'épargne disponibles sont déjà ouverts.")
                    } else {
                        var expanded by remember { mutableStateOf(false) }

                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedType?.name ?: "Choisir un type",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            availableTypes.forEach { type ->
                                DropdownMenuItem(
                                    onClick = {
                                        selectedType = type
                                        expanded = false
                                    },
                                    text = { Text(type.name) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = initialDepositInput,
                            onValueChange = { initialDepositInput = it },
                            label = { Text("Dépôt initial (optionnel)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = availableTypes.isNotEmpty() && selectedType != null,
                    onClick = {
                        val deposit = initialDepositInput
                            .replace(',', '.')
                            .toDoubleOrNull() ?: 0.0

                        val type = selectedType
                        if (type != null) {
                            viewModel.createAccount(type = type, initialBalance = deposit)
                        }

                        showCreateAccountDialog = false
                        selectedType = null
                        initialDepositInput = ""
                    }
                ) {
                    Text("Créer")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateAccountDialog = false
                        selectedType = null
                        initialDepositInput = ""
                    }
                ) {
                    Text("Annuler")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountCard(
    account: BankAccount,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = account.color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(account.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Wallet,
                    contentDescription = null,
                    tint = account.color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = account.accountName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = account.accountNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = "${String.format(Locale.FRANCE, "%.2f", account.balance)} ${account.currency}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
