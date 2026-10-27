package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Database Entity representing a recurring Subscription for local storage.
 */
@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val billingCycle: String, // "monthly", "yearly", "weekly"
    val category: String,
    val nextRenewalDate: String, // "YYYY-MM-DD"
    val paymentMethod: String,
    val isActive: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

typealias LocalSubscription = SubscriptionEntity
