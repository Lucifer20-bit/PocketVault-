package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Subscription(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val billingCycle: String = "monthly", // "monthly", "yearly", "weekly"
    val category: String = "Entertainment",
    val nextRenewalDate: String = "", // "YYYY-MM-DD"
    val paymentMethod: String = "Credit Card",
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
