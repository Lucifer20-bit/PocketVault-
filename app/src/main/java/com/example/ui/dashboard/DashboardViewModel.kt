package com.example.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.PocketVaultRoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun currentMonthString(): String {
    val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
    return sdf.format(Date())
}

data class DashboardMetrics(
    val netBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalBudget: Double = 0.0,
    val totalBudgetRemaining: Double = 0.0,
    val categorySpendingMap: Map<String, Double> = emptyMap()
)

data class DashboardUiState(
    val isLoading: Boolean = false,
    val transactions: List<TransactionEntity> = emptyList(),
    val metrics: DashboardMetrics = DashboardMetrics(),
    val selectedMonth: String = currentMonthString(),
    val typeFilter: String = "all", // "all", "expense", "income"
    val categoryFilter: String? = null,
    val errorMessage: String? = null
)

class DashboardViewModel(
    private val repository: PocketVaultRoomRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(currentMonthString())
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _typeFilter = MutableStateFlow("all")
    val typeFilter = _typeFilter.asStateFlow()

    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter = _categoryFilter.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.observeAllTransactions(),
        repository.observeAllEnvelopes(),
        _selectedMonth,
        _typeFilter,
        _categoryFilter
    ) { allTransactions, envelopes, month, type, category ->
        val monthTransactions = allTransactions.filter {
            month.isBlank() || it.date.startsWith(month)
        }

        var income = 0.0
        var expense = 0.0
        val categorySpending = mutableMapOf<String, Double>()

        for (tx in monthTransactions) {
            if (tx.type.equals("income", ignoreCase = true)) {
                income += tx.amount
            } else {
                expense += tx.amount
                val current = categorySpending.getOrDefault(tx.category, 0.0)
                categorySpending[tx.category] = current + tx.amount
            }
        }

        val totalBudget = envelopes.filter { month.isBlank() || it.month == month || it.month == "all" }
            .sumOf { it.monthlyLimit }
        val remainingBudget = (totalBudget - expense).coerceAtLeast(0.0)

        val filteredTransactions = monthTransactions.filter { tx ->
            val matchesType = type == "all" || tx.type.equals(type, ignoreCase = true)
            val matchesCategory = category == null || tx.category.equals(category, ignoreCase = true)
            matchesType && matchesCategory
        }

        DashboardUiState(
            isLoading = false,
            transactions = filteredTransactions,
            metrics = DashboardMetrics(
                netBalance = income - expense,
                totalIncome = income,
                totalExpense = expense,
                totalBudget = totalBudget,
                totalBudgetRemaining = remainingBudget,
                categorySpendingMap = categorySpending
            ),
            selectedMonth = month,
            typeFilter = type,
            categoryFilter = category,
            errorMessage = null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun selectMonth(month: String) {
        _selectedMonth.value = month
    }

    fun setTypeFilter(type: String) {
        _typeFilter.value = type
    }

    fun setCategoryFilter(category: String?) {
        _categoryFilter.value = category
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(id)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to delete transaction"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        fun currentMonthString(): String {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            return sdf.format(Date())
        }
    }
}
