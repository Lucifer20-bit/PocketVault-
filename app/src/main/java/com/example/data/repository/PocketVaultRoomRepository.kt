package com.example.data.repository

import com.example.data.local.dao.BudgetEnvelopeDao
import com.example.data.local.dao.EnvelopeDao
import com.example.data.local.dao.ExpenseDao
import com.example.data.local.dao.SubscriptionDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.BudgetEnvelopeEntity
import com.example.data.local.entity.EnvelopeEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.SubscriptionEntity
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class PocketVaultRoomRepository(
    private val transactionDao: TransactionDao,
    private val budgetEnvelopeDao: BudgetEnvelopeDao,
    private val subscriptionDao: SubscriptionDao,
    private val expenseDao: ExpenseDao? = null,
    private val envelopeDao: EnvelopeDao? = null
) {
    // Expenses (ExpenseEntity)
    fun observeAllExpenses(): Flow<List<ExpenseEntity>> =
        expenseDao?.getAllExpenses() ?: emptyFlow()

    suspend fun insertExpense(expense: ExpenseEntity): Long =
        expenseDao?.insertExpense(expense) ?: 0L

    suspend fun updateExpense(expense: ExpenseEntity) {
        expenseDao?.updateExpense(expense)
    }

    suspend fun deleteExpense(id: Long) {
        expenseDao?.deleteExpenseById(id)
    }

    // Envelopes (EnvelopeEntity)
    fun observeEnvelopes(): Flow<List<EnvelopeEntity>> =
        envelopeDao?.getAllEnvelopes() ?: emptyFlow()

    suspend fun insertEnvelopeModel(envelope: EnvelopeEntity): Long =
        envelopeDao?.insertEnvelope(envelope) ?: 0L

    suspend fun updateEnvelopeModel(envelope: EnvelopeEntity) {
        envelopeDao?.updateEnvelope(envelope)
    }

    suspend fun deleteEnvelopeModel(id: Long) {
        envelopeDao?.deleteEnvelopeById(id)
    }

    // Transactions
    fun observeAllTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactions()

    fun observeTransactionsByMonth(month: String): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByMonth(month)

    suspend fun insertTransaction(transaction: TransactionEntity): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(id: Long) =
        transactionDao.deleteTransactionById(id)

    // Budget Envelopes
    fun observeAllEnvelopes(): Flow<List<BudgetEnvelopeEntity>> =
        budgetEnvelopeDao.getAllEnvelopes()

    fun observeEnvelopesByMonth(month: String): Flow<List<BudgetEnvelopeEntity>> =
        budgetEnvelopeDao.getEnvelopesByMonth(month)

    suspend fun insertEnvelope(envelope: BudgetEnvelopeEntity): Long =
        budgetEnvelopeDao.insertEnvelope(envelope)

    suspend fun updateEnvelope(envelope: BudgetEnvelopeEntity) =
        budgetEnvelopeDao.updateEnvelope(envelope)

    suspend fun deleteEnvelope(id: Long) =
        budgetEnvelopeDao.deleteEnvelopeById(id)

    // Subscriptions
    fun observeSubscriptions(): Flow<List<SubscriptionEntity>> =
        subscriptionDao.getAllSubscriptions()

    suspend fun insertSubscription(subscription: SubscriptionEntity): Long =
        subscriptionDao.insertSubscription(subscription)

    suspend fun updateSubscription(subscription: SubscriptionEntity) =
        subscriptionDao.updateSubscription(subscription)

    suspend fun deleteSubscription(id: Long) =
        subscriptionDao.deleteSubscriptionById(id)
}
