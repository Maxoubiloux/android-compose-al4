package com.example.android_compose_al4.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.android_compose_al4.data.model.BankAccount
import com.example.android_compose_al4.viewmodel.BankViewModel
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import java.util.Locale

@Composable
fun TransferScreen(
    viewModel: BankViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState
    val transactionState by viewModel.transactionState
    val amountValue = transactionState.amountInput.replace(',', '.').toDoubleOrNull() ?: 0.0
    val accounts = uiState.accounts?.let { userAccounts ->
        listOfNotNull(
            userAccounts.currentAccount,
            userAccounts.livretA,
            userAccounts.livretJeune,
            userAccounts.pel
        )
    } ?: emptyList()

    LaunchedEffect(Unit) {
        viewModel.loadUserData()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Effectuer un virement",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        AccountSelector(
            title = "Compte source",
            accounts = accounts,
            selectedAccount = transactionState.fromAccount,
            onAccountSelected = { viewModel.setSourceAccount(it) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        AccountSelector(
            title = "Compte cible",
            accounts = accounts,
            selectedAccount = transactionState.toAccount,
            onAccountSelected = { viewModel.setTargetAccount(it) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = transactionState.amountInput,
            onValueChange = { viewModel.updateTransactionAmount(it) },
            label = { Text("Montant") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.makeTransfer() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = transactionState.fromAccount != null &&
                     transactionState.toAccount != null &&
                     amountValue > 0
        ) {
            Text("Effectuer le virement")
        }
    }
}

@Composable
private fun AccountSelector(
    title: String,
    accounts: List<BankAccount>,
    selectedAccount: BankAccount?,
    onAccountSelected: (BankAccount) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = title,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedAccount?.accountName ?: "Sélectionner un compte",
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded)
                    Icons.Default.ArrowDropUp
                else
                    Icons.Default.ArrowDropDown,
                contentDescription = null
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            accounts.forEach { account ->
                AccountDropdownItem(
                    account = account,
                    isSelected = selectedAccount?.accountNumber == account.accountNumber,
                    onSelect = {
                        onAccountSelected(account)
                        expanded = false
                    }
                )
            }
        }

        selectedAccount?.let { account ->
            Text(
                text = "Solde : ${String.format(Locale.US, "%.2f", account.balance)} €" +
                      if (account.type != BankAccount.AccountType.CURRENT) " (Plafond: ${account.maxDeposit} €)" else "",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun AccountDropdownItem(
    account: BankAccount,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    DropdownMenuItem(
        onClick = onSelect,
        text = {
            Column {
                Text(account.accountName)
                Text(
                    text = "${String.format(Locale.US, "%.2f", account.balance)} €",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        leadingIcon = {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Spacer(modifier = Modifier.size(24.dp))
            }
        }
    )
}
