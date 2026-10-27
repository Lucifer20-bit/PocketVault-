package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: String, // "expense" or "income"
    val category: String,
    val paymentMethod: String,
    val date: String, // "YYYY-MM-DD"
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
