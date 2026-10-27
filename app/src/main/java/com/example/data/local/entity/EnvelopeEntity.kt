package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Database Entity representing a Budget Envelope for local storage.
 */
@Entity(tableName = "envelopes")
data class EnvelopeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val monthlyLimit: Double,
    val spent: Double = 0.0,
    val month: String, // "YYYY-MM"
    val timestamp: Long = System.currentTimeMillis()
)

typealias Envelope = EnvelopeEntity
