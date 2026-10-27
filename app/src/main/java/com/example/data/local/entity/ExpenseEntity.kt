package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Database Entity representing an Expense for local storage.
 */
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val paymentMethod: String = "Credit Card",
    val date: String, // "YYYY-MM-DD"
    val notes: String? = null,
    val isRecurring: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

typealias Expense = ExpenseEntity
