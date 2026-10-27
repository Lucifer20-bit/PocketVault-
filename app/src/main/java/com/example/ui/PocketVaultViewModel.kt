package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Budget
import com.example.data.model.Subscription
import com.example.data.model.Transaction
import com.example.data.repository.PocketVaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class BudgetStatus {
    SAFE,       // < 75%
    WARNING,    // 75% - 99%
    EXCEEDED    // >= 100%
}

data class CategoryBudgetProgress(
    val category: String,
    val monthlyLimit: Double,
    val spent: Double,
    val remaining: Double,
    val progressRatio: Float,
    val status: BudgetStatus,
    val budgetId: String
)

data class FinancialOverview(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    val totalBudget: Double = 0.0,
    val totalBudgetRemaining: Double = 0.0,
    val categorySpending: Map<String, Double> = emptyMap()
)

class PocketVaultViewModel(
    private val repository: PocketVaultRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(currentMonthString())
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()

    // 1. Transactions observation
    val transactions: StateFlow<UiState<List<Transaction>>> = repository.observeTransactions(currentUserId)
        .map<List<Transaction>, UiState<List<Transaction>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing transactions", error)
            emit(UiState.Error(error.message ?: "Failed to observe transactions"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // 2. Budgets observation
    val budgets: StateFlow<UiState<List<Budget>>> = repository.observeBudgets(currentUserId)
        .map<List<Budget>, UiState<List<Budget>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing budgets", error)
            emit(UiState.Error(error.message ?: "Failed to observe budgets"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // 3. Subscriptions observation
    val subscriptions: StateFlow<UiState<List<Subscription>>> = repository.observeSubscriptions(currentUserId)
        .map<List<Subscription>, UiState<List<Subscription>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing subscriptions", error)
            emit(UiState.Error(error.message ?: "Failed to observe subscriptions"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // 4. Derived Financial Overview for current month
    val overview: StateFlow<FinancialOverview> = combine(
        transactions,
        budgets,
        _selectedMonth
    ) { txState, bgState, month ->
        val txList = (txState as? UiState.Success)?.data ?: emptyList()
        val bgList = (bgState as? UiState.Success)?.data ?: emptyList()

        val filteredTx = txList.filter { it.date.startsWith(month) || month.isEmpty() }

        var income = 0.0
        var expense = 0.0
        val categoryMap = mutableMapOf<String, Double>()

        for (tx in filteredTx) {
            if (tx.type == "income") {
                income += tx.amount
            } else {
                expense += tx.amount
                val curr = categoryMap.getOrDefault(tx.category, 0.0)
                categoryMap[tx.category] = curr + tx.amount
            }
        }

        val totalBudget = bgList.sumOf { it.monthlyLimit }
        val remainingBudget = (totalBudget - expense).coerceAtLeast(0.0)

        FinancialOverview(
            totalIncome = income,
            totalExpense = expense,
            netBalance = income - expense,
            totalBudget = totalBudget,
            totalBudgetRemaining = remainingBudget,
            categorySpending = categoryMap
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = FinancialOverview()
    )

    // 5. Derived Budget Progress list
    val budgetProgressList: StateFlow<List<CategoryBudgetProgress>> = combine(
        budgets,
        transactions,
        _selectedMonth
    ) { bgState, txState, month ->
        val bgList = (bgState as? UiState.Success)?.data ?: emptyList()
        val txList = (txState as? UiState.Success)?.data ?: emptyList()
        val monthExpenses = txList.filter { (it.date.startsWith(month) || month.isEmpty()) && it.type == "expense" }

        bgList.map { budget ->
            val spent = monthExpenses
                .filter { it.category.equals(budget.category, ignoreCase = true) }
                .sumOf { it.amount }

            val ratio = if (budget.monthlyLimit > 0) (spent / budget.monthlyLimit).toFloat() else 0f
            val status = when {
                ratio >= 1.0f -> BudgetStatus.EXCEEDED
                ratio >= 0.75f -> BudgetStatus.WARNING
                else -> BudgetStatus.SAFE
            }

            CategoryBudgetProgress(
                category = budget.category,
                monthlyLimit = budget.monthlyLimit,
                spent = spent,
                remaining = budget.monthlyLimit - spent,
                progressRatio = ratio,
                status = status,
                budgetId = budget.id
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    fun selectMonth(month: String) {
        _selectedMonth.value = month
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    // ---------------- Actions ----------------

    fun addTransaction(tx: Transaction, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.addTransaction(currentUserId, tx)
            if (result.isSuccess) {
                _userMessage.value = "Transaction saved!"
                onComplete()
            } else {
                _userMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to save transaction"
            }
        }
    }

    fun updateTransaction(tx: Transaction, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.updateTransaction(currentUserId, tx)
            if (result.isSuccess) {
                _userMessage.value = "Transaction updated!"
                onComplete()
            } else {
                _userMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to update transaction"
            }
        }
    }

    fun deleteTransaction(txId: String) {
        viewModelScope.launch {
            val result = repository.deleteTransaction(currentUserId, txId)
            if (result.isSuccess) {
                _userMessage.value = "Transaction deleted"
            }
        }
    }

    fun saveBudget(budget: Budget, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.setBudget(currentUserId, budget)
            if (result.isSuccess) {
                _userMessage.value = "Budget envelope saved!"
                onComplete()
            } else {
                _userMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to save budget"
            }
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            val result = repository.deleteBudget(currentUserId, budgetId)
            if (result.isSuccess) {
                _userMessage.value = "Budget deleted"
            }
        }
    }

    fun addSubscription(sub: Subscription, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.addSubscription(currentUserId, sub)
            if (result.isSuccess) {
                _userMessage.value = "Subscription added!"
                onComplete()
            } else {
                _userMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to add subscription"
            }
        }
    }

    fun updateSubscription(sub: Subscription, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.updateSubscription(currentUserId, sub)
            if (result.isSuccess) {
                _userMessage.value = "Subscription updated!"
                onComplete()
            } else {
                _userMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to update subscription"
            }
        }
    }

    fun deleteSubscription(subId: String) {
        viewModelScope.launch {
            val result = repository.deleteSubscription(currentUserId, subId)
            if (result.isSuccess) {
                _userMessage.value = "Subscription deleted"
            }
        }
    }

    companion object {
        private const val TAG = "PocketVaultVM"

        fun currentMonthString(): String {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            return sdf.format(Date())
        }

        fun currentDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(Date())
        }
    }
}
