package com.example.android_compose_al4.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_compose_al4.data.model.*
import com.example.android_compose_al4.data.repository.BankRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class BankViewModel @Inject constructor(
    private val repository: BankRepository
) : ViewModel() {

    private val _uiState = mutableStateOf(BankUiState())
    val uiState: State<BankUiState> = _uiState

    private val _accounts = MutableStateFlow<List<BankAccount>>(emptyList())
    val accounts: StateFlow<List<BankAccount>> = _accounts.asStateFlow()

    private val _currentAccount = MutableStateFlow<BankAccount?>(null)
    val currentAccount: StateFlow<BankAccount?> = _currentAccount.asStateFlow()

    private val _events = MutableSharedFlow<BankUiEvent>()
    val events = _events.asSharedFlow()

    private val _transactionState = mutableStateOf(TransactionState())
    val transactionState: State<TransactionState> = _transactionState

    init {
        loadUserData()
        loadTransactions()
    }

    fun loadUserData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.getUserProfile()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Erreur inconnue lors du chargement du profil",
                        isLoading = false
                    )
                }
                .collect { user ->
                    _uiState.value = _uiState.value.copy(
                        user = user,
                        isLoading = false
                    )
                }

            loadAccounts()
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            repository.getAccounts()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Erreur lors du chargement des comptes"
                    )
                }
                .collect { accounts ->
                    _accounts.value = accounts
                    _uiState.value = _uiState.value.copy(
                        accounts = accounts.toUserAccounts()
                    )
                }

            loadCurrentAccount()
        }
    }

    private fun loadCurrentAccount() {
        viewModelScope.launch {
            repository.getCurrentAccount()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Erreur lors du chargement du compte courant"
                    )
                }
                .collect { account ->
                    _currentAccount.value = account
                }
        }
    }

    fun loadTransactions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getTransactions()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Erreur lors du chargement des transactions",
                        isLoading = false
                    )
                }
                .collect { transactions ->
                    _uiState.value = _uiState.value.copy(
                        transactions = transactions,
                        isLoading = false
                    )
                }
        }
    }

    fun showUiMessage(message: String) {
        showMessage(message)
    }

    fun createAccount(
        type: BankAccount.AccountType,
        initialBalance: Double = 0.0
    ) {
        viewModelScope.launch {
            try {
                if (initialBalance < 0) {
                    showMessage("Le solde initial ne peut pas être négatif")
                    return@launch
                }

                val existing = _accounts.value
                if (existing.any { it.type == type }) {
                    showMessage("Un compte de ce type existe déjà")
                    return@launch
                }

                val previewAccount = BankAccount(
                    accountNumber = "",
                    accountName = "",
                    balance = 0.0,
                    type = type
                )
                if (type != BankAccount.AccountType.CURRENT && initialBalance > previewAccount.maxDeposit) {
                    showMessage("Le plafond du compte est dépassé (max: ${previewAccount.maxDeposit}€)")
                    return@launch
                }

                val account = BankAccount(
                    accountNumber = generateAccountNumber(),
                    accountName = defaultAccountName(type),
                    balance = initialBalance,
                    type = type,
                    currency = "EUR",
                    color = defaultAccountColor(type)
                )

                val result = repository.createAccount(account).first()
                if (result.isSuccess) {
                    val created = result.getOrThrow()
                    val updated = _accounts.value + created
                    _accounts.value = updated
                    _uiState.value = _uiState.value.copy(accounts = updated.toUserAccounts())
                    _currentAccount.value = updated.find { it.type == BankAccount.AccountType.CURRENT }
                    showMessage("Compte créé avec succès")
                } else {
                    throw (result.exceptionOrNull() ?: Exception("Échec de création du compte"))
                }
            } catch (e: Exception) {
                showMessage("Erreur lors de la création du compte: ${e.message}")
            }
        }
    }

    fun closeAccount(account: BankAccount) {
        viewModelScope.launch {
            try {
                if (account.type == BankAccount.AccountType.CURRENT) {
                    showMessage("Impossible de clôturer le compte courant")
                    return@launch
                }

                val accountId = account.id
                if (accountId.isNullOrBlank()) {
                    showMessage("Impossible de clôturer ce compte: identifiant manquant")
                    return@launch
                }

                if (account.balance < 0) {
                    showMessage("Impossible de clôturer un compte avec solde négatif")
                    return@launch
                }

                val current = _accounts.value.find { it.type == BankAccount.AccountType.CURRENT }
                if (current == null || current.id.isNullOrBlank()) {
                    showMessage("Impossible de clôturer: compte courant introuvable")
                    return@launch
                }

                if (account.balance > 0) {
                    persistTransfer(
                        fromAccount = account,
                        toAccount = current,
                        amount = account.balance,
                        title = "Clôture ${account.accountName} → ${current.accountName}"
                    )
                }

                val deleteResult = repository.deleteAccount(accountId).first()
                if (deleteResult.isFailure) {
                    throw (deleteResult.exceptionOrNull() ?: Exception("Échec de suppression du compte"))
                }

                val updated = _accounts.value.filterNot { it.id == accountId }
                _accounts.value = updated
                _uiState.value = _uiState.value.copy(accounts = updated.toUserAccounts())
                _currentAccount.value = updated.find { it.type == BankAccount.AccountType.CURRENT }

                showMessage("Compte clôturé")
            } catch (e: Exception) {
                showMessage("Erreur lors de la clôture: ${e.message}")
            }
        }
    }

    private suspend fun persistTransfer(
        fromAccount: BankAccount,
        toAccount: BankAccount,
        amount: Double,
        title: String
    ) {
        if (amount <= 0) {
            throw IllegalArgumentException("Montant invalide")
        }
        if (fromAccount.balance < amount) {
            throw IllegalArgumentException("Solde insuffisant")
        }
        if (toAccount.type != BankAccount.AccountType.CURRENT && (toAccount.balance + amount) > toAccount.maxDeposit) {
            throw IllegalArgumentException("Plafond du compte dépassé")
        }
        if (fromAccount.type != BankAccount.AccountType.CURRENT && toAccount.type != BankAccount.AccountType.CURRENT) {
            throw IllegalArgumentException("Les virements entre comptes d'épargne ne sont pas autorisés")
        }
        if (fromAccount.type == BankAccount.AccountType.PEL && amount < fromAccount.balance) {
            throw IllegalArgumentException("Pour retirer de l'argent d'un PEL, vous devez le clôturer entièrement")
        }

        val fromAccountId = fromAccount.id
        val toAccountId = toAccount.id
        if (fromAccountId.isNullOrBlank() || toAccountId.isNullOrBlank()) {
            throw IllegalStateException("Identifiant de compte manquant")
        }

        val updatedFrom = fromAccount.copy(balance = fromAccount.balance - amount)
        val updatedTo = toAccount.copy(balance = toAccount.balance + amount)

        val updatedAccounts = _accounts.value.toMutableList().apply {
            val fromIndex = indexOfFirst { it.id == fromAccountId }
            val toIndex = indexOfFirst { it.id == toAccountId }
            if (fromIndex < 0 || toIndex < 0) {
                throw IllegalStateException("Compte introuvable dans la liste chargée")
            }
            set(fromIndex, updatedFrom)
            set(toIndex, updatedTo)
        }

        val category = TransactionCategory(7, "Virement", "swap_horiz", Color(0xFF9C27B0))
        val now = Date().toString()
        val debit = Transaction(
            title = title,
            amount = -amount,
            date = now,
            category = category,
            accountId = fromAccountId
        )
        val credit = Transaction(
            title = title,
            amount = amount,
            date = now,
            category = category,
            accountId = toAccountId
        )

        val updateResult = repository.updateAccounts(updatedAccounts).first()
        if (updateResult.isFailure) {
            throw (updateResult.exceptionOrNull() ?: Exception("Échec de mise à jour des comptes"))
        }

        val debitResult = repository.addTransaction(debit).first()
        if (debitResult.isFailure) {
            throw (debitResult.exceptionOrNull() ?: Exception("Échec d'enregistrement du débit"))
        }

        val creditResult = repository.addTransaction(credit).first()
        if (creditResult.isFailure) {
            throw (creditResult.exceptionOrNull() ?: Exception("Échec d'enregistrement du crédit"))
        }

        _accounts.value = updatedAccounts
        _uiState.value = _uiState.value.copy(
            accounts = updatedAccounts.toUserAccounts(),
            transactions = _uiState.value.transactions + listOf(
                debitResult.getOrNull() ?: debit,
                creditResult.getOrNull() ?: credit
            )
        )
        _currentAccount.value = updatedAccounts.find { it.type == BankAccount.AccountType.CURRENT }
    }

    private fun defaultAccountName(type: BankAccount.AccountType): String {
        return when (type) {
            BankAccount.AccountType.CURRENT -> "Compte Courant"
            BankAccount.AccountType.LIVRET_A -> "Livret A"
            BankAccount.AccountType.LIVRET_JEUNE -> "Livret Jeune"
            BankAccount.AccountType.PEL -> "PEL"
        }
    }

    private fun defaultAccountColor(type: BankAccount.AccountType): Color {
        return when (type) {
            BankAccount.AccountType.CURRENT -> Color(0xFF6200EE)
            BankAccount.AccountType.LIVRET_A -> Color(0xFF03A9F4)
            BankAccount.AccountType.LIVRET_JEUNE -> Color(0xFFFF9800)
            BankAccount.AccountType.PEL -> Color(0xFF9C27B0)
        }
    }

    private fun generateAccountNumber(): String {
        val part1 = (1000..9999).random()
        val part2 = (1000..9999).random()
        val part3 = (1000..9999).random()
        val part4 = (1000..9999).random()
        return "FR76 $part1 $part2 $part3 $part4"
    }

    fun updateTransactionAmount(amount: String) {
        val normalized = amount.replace(',', '.')
        _transactionState.value = _transactionState.value.copy(
            amountInput = amount,
            amount = normalized.toDoubleOrNull() ?: 0.0
        )
    }

    fun setSourceAccount(account: BankAccount) {
        _transactionState.value = _transactionState.value.copy(
            fromAccount = account,
            toAccount = if (account.type == _transactionState.value.toAccount?.type) {
                null
            } else {
                _transactionState.value.toAccount
            }
        )
    }

    fun setTargetAccount(account: BankAccount) {
        _transactionState.value = _transactionState.value.copy(
            toAccount = account,
            fromAccount = if (account.type == _transactionState.value.fromAccount?.type) {
                null
            } else {
                _transactionState.value.fromAccount
            }
        )
    }

    fun makeTransfer() {
        val state = _transactionState.value
        val fromAccount = state.fromAccount ?: run {
            showMessage("Veuillez sélectionner le compte source")
            return
        }
        val toAccount = state.toAccount ?: run {
            showMessage("Veuillez sélectionner le compte cible")
            return
        }

        when {
            state.amount <= 0 -> {
                showMessage("Le montant doit être supérieur à zéro")
                return
            }
            fromAccount.balance < state.amount -> {
                showMessage("Solde insuffisant sur le compte source")
                return
            }
            toAccount.type != BankAccount.AccountType.CURRENT &&
                    (toAccount.balance + state.amount) > toAccount.maxDeposit -> {
                showMessage("Le plafond du compte est dépassé (max: ${toAccount.maxDeposit}€)")
                return
            }
            fromAccount.type != BankAccount.AccountType.CURRENT &&
                    toAccount.type != BankAccount.AccountType.CURRENT -> {
                showMessage("Les virements entre comptes d'épargne ne sont pas autorisés")
                return
            }
            fromAccount.type == BankAccount.AccountType.PEL && state.amount < fromAccount.balance -> {
                showMessage("Pour retirer de l'argent d'un PEL, vous devez le clôturer entièrement")
                return
            }
        }

        viewModelScope.launch {
            try {
                val fromAccountId = fromAccount.id
                val toAccountId = toAccount.id
                if (fromAccountId.isNullOrBlank() || toAccountId.isNullOrBlank()) {
                    showMessage("Impossible d'effectuer le virement: identifiant de compte manquant")
                    return@launch
                }

                val updatedFromAccount = fromAccount.copy(
                    balance = fromAccount.balance - state.amount
                )
                val updatedToAccount = toAccount.copy(
                    balance = toAccount.balance + state.amount
                )

                val updatedAccounts = _accounts.value.toMutableList().apply {
                    val fromIndex = indexOfFirst { it.id == fromAccountId }
                    val toIndex = indexOfFirst { it.id == toAccountId }
                    if (fromIndex < 0 || toIndex < 0) {
                        throw IllegalStateException("Compte introuvable dans la liste chargée")
                    }
                    set(fromIndex, updatedFromAccount)
                    set(toIndex, updatedToAccount)
                }

                val category = TransactionCategory(7, "Virement", "swap_horiz", Color(0xFF9C27B0))
                val now = Date().toString()

                val debitTransaction = Transaction(
                    title = "Virement ${fromAccount.accountName} → ${toAccount.accountName}",
                    amount = -state.amount,
                    date = now,
                    category = category,
                    accountId = fromAccountId
                )

                val creditTransaction = Transaction(
                    title = "Virement ${fromAccount.accountName} → ${toAccount.accountName}",
                    amount = state.amount,
                    date = now,
                    category = category,
                    accountId = toAccountId
                )

                val updateResult = repository.updateAccounts(updatedAccounts).first()
                if (updateResult.isFailure) {
                    throw (updateResult.exceptionOrNull() ?: Exception("Échec de mise à jour des comptes"))
                }

                val debitResult = repository.addTransaction(debitTransaction).first()
                if (debitResult.isFailure) {
                    throw (debitResult.exceptionOrNull() ?: Exception("Échec d'enregistrement du débit"))
                }

                val creditResult = repository.addTransaction(creditTransaction).first()
                if (creditResult.isFailure) {
                    throw (creditResult.exceptionOrNull() ?: Exception("Échec d'enregistrement du crédit"))
                }

                val persistedDebit = debitResult.getOrNull() ?: debitTransaction
                val persistedCredit = creditResult.getOrNull() ?: creditTransaction

                _accounts.value = updatedAccounts
                _uiState.value = _uiState.value.copy(
                    accounts = updatedAccounts.toUserAccounts(),
                    transactions = _uiState.value.transactions + listOf(persistedDebit, persistedCredit)
                )

                _currentAccount.value = updatedAccounts.find { it.type == BankAccount.AccountType.CURRENT }

                _transactionState.value = TransactionState()

                showMessage("Virement effectué avec succès")

                loadTransactions()

            } catch (e: Exception) {
                showMessage("Erreur lors du virement: ${e.message}")
            }
        }
    }

    private fun showMessage(message: String) {
        viewModelScope.launch {
            _events.emit(BankUiEvent.ShowMessage(message))
        }
    }
}

data class BankUiState(
    val user: User? = null,
    val accounts: UserAccounts? = null,
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class TransactionState(
    val fromAccount: BankAccount? = null,
    val toAccount: BankAccount? = null,
    val amountInput: String = "",
    val amount: Double = 0.0
)

sealed class BankUiEvent {
    data class ShowMessage(val message: String) : BankUiEvent()
}
