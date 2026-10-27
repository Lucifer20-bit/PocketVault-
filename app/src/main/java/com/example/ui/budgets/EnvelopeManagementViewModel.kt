package com.example.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.BudgetEnvelopeEntity
import com.example.data.repository.PocketVaultRoomRepository
import com.example.ui.BudgetStatus
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

data class EnvelopeProgress(
    val id: Long,
    val category: String,
    val monthlyLimit: Double,
    val spent: Double,
    val remaining: Double,
    val progressRatio: Float,
    val status: BudgetStatus,
    val month: String
)

data class EnvelopeManagementUiState(
    val isLoading: Boolean = false,
    val selectedMonth: String = currentMonthString(),
    val envelopes: List<EnvelopeProgress> = emptyList(),
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val totalRemaining: Double = 0.0,
    val overallProgressRatio: Float = 0f,
    val statusSummary: Map<BudgetStatus, Int> = emptyMap(),
    val userNotification: String? = null
)

private fun currentMonthString(): String {
    val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
    return sdf.format(Date())
}

class EnvelopeManagementViewModel(
    private val repository: PocketVaultRoomRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(currentMonthString())
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _notification = MutableStateFlow<String?>(null)
    val notification = _notification.asStateFlow()

    val uiState: StateFlow<EnvelopeManagementUiState> = combine(
        repository.observeAllEnvelopes(),
        repository.observeAllTransactions(),
        _selectedMonth
    ) { allEnvelopes, allTransactions, month ->
        // Filter transactions to selected month's expenses
        val monthExpenses = allTransactions.filter {
            (month.isBlank() || it.date.startsWith(month)) &&
                    it.type.equals("expense", ignoreCase = true)
        }

        // Filter envelopes for selected month or 'all'
        val monthEnvelopes = allEnvelopes.filter {
            month.isBlank() || it.month == month || it.month == "all"
        }

        var totalBudget = 0.0
        var totalSpent = 0.0
        val statusCount = mutableMapOf(
            BudgetStatus.SAFE to 0,
            BudgetStatus.WARNING to 0,
            BudgetStatus.EXCEEDED to 0
        )

        val progressList = monthEnvelopes.map { env ->
            val spentForCategory = monthExpenses
                .filter { it.category.equals(env.category, ignoreCase = true) }
                .sumOf { it.amount }

            totalBudget += env.monthlyLimit
            totalSpent += spentForCategory

            val ratio = if (env.monthlyLimit > 0) (spentForCategory / env.monthlyLimit).toFloat() else 0f
            val status = when {
                ratio >= 1.0f -> BudgetStatus.EXCEEDED
                ratio >= 0.75f -> BudgetStatus.WARNING
                else -> BudgetStatus.SAFE
            }

            statusCount[status] = (statusCount[status] ?: 0) + 1

            EnvelopeProgress(
                id = env.id,
                category = env.category,
                monthlyLimit = env.monthlyLimit,
                spent = spentForCategory,
                remaining = env.monthlyLimit - spentForCategory,
                progressRatio = ratio,
                status = status,
                month = env.month
            )
        }

        val overallRatio = if (totalBudget > 0) (totalSpent / totalBudget).toFloat().coerceIn(0f, 1f) else 0f
        val remainingBudget = (totalBudget - totalSpent).coerceAtLeast(0.0)

        EnvelopeManagementUiState(
            isLoading = false,
            selectedMonth = month,
            envelopes = progressList,
            totalBudget = totalBudget,
            totalSpent = totalSpent,
            totalRemaining = remainingBudget,
            overallProgressRatio = overallRatio,
            statusSummary = statusCount,
            userNotification = null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = EnvelopeManagementUiState(isLoading = true)
    )

    fun selectMonth(month: String) {
        _selectedMonth.value = month
    }

    fun saveEnvelope(
        id: Long = 0,
        category: String,
        monthlyLimit: Double,
        month: String = _selectedMonth.value,
        onSuccess: () -> Unit = {}
    ) {
        if (monthlyLimit <= 0) {
            _notification.value = "Monthly limit must be greater than zero"
            return
        }

        viewModelScope.launch {
            try {
                val entity = BudgetEnvelopeEntity(
                    id = id,
                    category = category.trim(),
                    monthlyLimit = monthlyLimit,
                    month = month.trim().ifBlank { currentMonthString() }
                )

                if (id > 0) {
                    repository.updateEnvelope(entity)
                    _notification.value = "Envelope updated for $category"
                } else {
                    repository.insertEnvelope(entity)
                    _notification.value = "New envelope created for $category"
                }
                onSuccess()
            } catch (e: Exception) {
                _notification.value = e.localizedMessage ?: "Failed to save envelope"
            }
        }
    }

    fun deleteEnvelope(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteEnvelope(id)
                _notification.value = "Envelope deleted"
            } catch (e: Exception) {
                _notification.value = e.localizedMessage ?: "Failed to delete envelope"
            }
        }
    }

    fun clearNotification() {
        _notification.value = null
    }
}
