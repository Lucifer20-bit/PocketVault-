package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Budget
import com.example.data.model.Subscription
import com.example.data.model.Transaction
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PocketVaultRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    // ---------------- Transactions ----------------

    fun observeTransactions(userId: String): Flow<List<Transaction>> {
        val path = "users/$userId/transactions"
        return db.collection("users").document(userId).collection("transactions")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Transaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.date }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun addTransaction(userId: String, transaction: Transaction): Result<String> {
        val collection = db.collection("users").document(userId).collection("transactions")
        val docRef = collection.document()
        val payload = mapOf(
            "userId" to userId,
            "title" to transaction.title,
            "amount" to transaction.amount,
            "type" to transaction.type,
            "category" to transaction.category,
            "paymentMethod" to transaction.paymentMethod,
            "date" to transaction.date,
            "notes" to transaction.notes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateTransaction(userId: String, transaction: Transaction): Result<Unit> {
        require(transaction.id.isNotBlank()) { "Transaction ID cannot be blank for update" }
        val docRef = db.collection("users").document(userId).collection("transactions").document(transaction.id)
        val payload = mapOf(
            "title" to transaction.title,
            "amount" to transaction.amount,
            "type" to transaction.type,
            "category" to transaction.category,
            "paymentMethod" to transaction.paymentMethod,
            "date" to transaction.date,
            "notes" to transaction.notes,
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        return try {
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(userId: String, transactionId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("transactions").document(transactionId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // ---------------- Budgets ----------------

    fun observeBudgets(userId: String): Flow<List<Budget>> {
        val path = "users/$userId/budgets"
        return db.collection("users").document(userId).collection("budgets")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Budget::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.category }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun setBudget(userId: String, budget: Budget): Result<String> {
        val collection = db.collection("users").document(userId).collection("budgets")
        val docRef = if (budget.id.isNotBlank()) collection.document(budget.id) else collection.document()
        val isNew = budget.id.isBlank()

        val payload = if (isNew) {
            mapOf(
                "userId" to userId,
                "category" to budget.category,
                "monthlyLimit" to budget.monthlyLimit,
                "month" to budget.month,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        } else {
            mapOf(
                "category" to budget.category,
                "monthlyLimit" to budget.monthlyLimit,
                "month" to budget.month,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }

        return try {
            if (isNew) {
                docRef.set(payload).await()
            } else {
                docRef.update(payload).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteBudget(userId: String, budgetId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("budgets").document(budgetId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // ---------------- Subscriptions ----------------

    fun observeSubscriptions(userId: String): Flow<List<Subscription>> {
        val path = "users/$userId/subscriptions"
        return db.collection("users").document(userId).collection("subscriptions")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Subscription::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.nextRenewalDate }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun addSubscription(userId: String, subscription: Subscription): Result<String> {
        val collection = db.collection("users").document(userId).collection("subscriptions")
        val docRef = collection.document()
        val payload = mapOf(
            "userId" to userId,
            "name" to subscription.name,
            "amount" to subscription.amount,
            "billingCycle" to subscription.billingCycle,
            "category" to subscription.category,
            "nextRenewalDate" to subscription.nextRenewalDate,
            "paymentMethod" to subscription.paymentMethod,
            "isActive" to subscription.isActive,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateSubscription(userId: String, subscription: Subscription): Result<Unit> {
        require(subscription.id.isNotBlank()) { "Subscription ID cannot be blank for update" }
        val docRef = db.collection("users").document(userId).collection("subscriptions").document(subscription.id)
        val payload = mapOf(
            "name" to subscription.name,
            "amount" to subscription.amount,
            "billingCycle" to subscription.billingCycle,
            "category" to subscription.category,
            "nextRenewalDate" to subscription.nextRenewalDate,
            "paymentMethod" to subscription.paymentMethod,
            "isActive" to subscription.isActive,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteSubscription(userId: String, subscriptionId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("subscriptions").document(subscriptionId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
