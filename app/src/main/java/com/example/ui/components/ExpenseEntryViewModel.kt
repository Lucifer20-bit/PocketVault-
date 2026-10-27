package com.example.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.PocketVaultRoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExpenseEntryFormState(
    val id: Long = 0,
    val title: String = "",
    val amountText: String = "",
    val type: String = "expense", // "expense" or "income"
    val category: String = "Food",
    val paymentMethod: String = "Credit Card",
    val date: String = currentDateString(),
    val notes: String = "",
    val isSaving: Boolean = false,
    val titleError: String? = null,
    val amountError: String? = null,
    val generalError: String? = null,
    val saveSuccess: Boolean = false
) {
    val isEditing: Boolean get() = id > 0
}

private fun currentDateString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return sdf.format(Date())
}

class ExpenseEntryViewModel(
    private val repository: PocketVaultRoomRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(ExpenseEntryFormState())
    val formState: StateFlow<ExpenseEntryFormState> = _formState.asStateFlow()

    fun onTitleChange(newTitle: String) {
        _formState.update {
            it.copy(
                title = newTitle,
                titleError = if (newTitle.isBlank()) "Title is required" else null
            )
        }
    }

    fun onAmountChange(newAmount: String) {
        val parsed = newAmount.toDoubleOrNull()
        val error = when {
            newAmount.isBlank() -> "Amount is required"
            parsed == null -> "Enter a valid number"
            parsed <= 0 -> "Amount must be greater than 0"
            else -> null
        }
        _formState.update {
            it.copy(
                amountText = newAmount,
                amountError = error
            )
        }
    }

    fun onTypeChange(newType: String) {
        _formState.update { it.copy(type = newType) }
    }

    fun onCategoryChange(newCategory: String) {
        _formState.update { it.copy(category = newCategory) }
    }

    fun onPaymentMethodChange(newMethod: String) {
        _formState.update { it.copy(paymentMethod = newMethod) }
    }

    fun onDateChange(newDate: String) {
        _formState.update { it.copy(date = newDate) }
    }

    fun onNotesChange(newNotes: String) {
        _formState.update { it.copy(notes = newNotes) }
    }

    fun populateForEdit(transaction: TransactionEntity) {
        _formState.value = ExpenseEntryFormState(
            id = transaction.id,
            title = transaction.title,
            amountText = transaction.amount.toString(),
            type = transaction.type,
            category = transaction.category,
            paymentMethod = transaction.paymentMethod,
            date = transaction.date,
            notes = transaction.notes ?: "",
            isSaving = false
        )
    }

    fun resetForm() {
        _formState.value = ExpenseEntryFormState()
    }

    fun save(onSuccess: () -> Unit = {}) {
        val current = _formState.value
        val parsedAmount = current.amountText.toDoubleOrNull()

        var hasError = false
        var titleErr: String? = null
        var amountErr: String? = null

        if (current.title.isBlank()) {
            titleErr = "Title / merchant name cannot be blank"
            hasError = true
        }

        if (parsedAmount == null || parsedAmount <= 0) {
            amountErr = "Enter a positive amount"
            hasError = true
        }

        if (hasError) {
            _formState.update {
                it.copy(
                    titleError = titleErr,
                    amountError = amountErr
                )
            }
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true, generalError = null) }
            try {
                val entity = TransactionEntity(
                    id = current.id,
                    title = current.title.trim(),
                    amount = parsedAmount!!,
                    type = current.type,
                    category = current.category,
                    paymentMethod = current.paymentMethod,
                    date = current.date.trim().ifBlank { currentDateString() },
                    notes = current.notes.trim().ifBlank { null }
                )

                if (current.isEditing) {
                    repository.updateTransaction(entity)
                } else {
                    repository.insertTransaction(entity)
                }

                _formState.update { it.copy(isSaving = false, saveSuccess = true) }
                onSuccess()
            } catch (e: Exception) {
                _formState.update {
                    it.copy(
                        isSaving = false,
                        generalError = e.localizedMessage ?: "Failed to save transaction to local database"
                    )
                }
            }
        }
    }
}
