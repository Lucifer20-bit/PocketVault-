package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_envelopes")
data class BudgetEnvelopeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val monthlyLimit: Double,
    val month: String, // "YYYY-MM"
    val timestamp: Long = System.currentTimeMillis()
)
