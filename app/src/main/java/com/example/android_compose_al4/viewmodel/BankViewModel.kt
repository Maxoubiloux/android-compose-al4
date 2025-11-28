package com.example.android_compose_al4.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_compose_al4.data.model.Transaction
import com.example.android_compose_al4.data.model.User
import com.example.android_compose_al4.data.repository.BankRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BankViewModel @Inject constructor(
    private val repository: BankRepository
) : ViewModel() {
    
    private val _uiState = mutableStateOf(BankUiState())
    val uiState: State<BankUiState> = _uiState
    
    private val _events = MutableSharedFlow<BankUiEvent>()
    val events = _events.asSharedFlow()
    
    init {
        loadUserData()
        loadTransactions()
    }
    
    private fun loadUserData() {
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
}

data class BankUiState(
    val user: User? = null,
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class BankUiEvent {
    data class ShowMessage(val message: String) : BankUiEvent()
}
