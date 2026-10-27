package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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

@Database(
    entities = [
        TransactionEntity::class,
        BudgetEnvelopeEntity::class,
        SubscriptionEntity::class,
        ExpenseEntity::class,
        EnvelopeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PocketVaultDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetEnvelopeDao(): BudgetEnvelopeDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun envelopeDao(): EnvelopeDao

    companion object {
        @Volatile
        private var INSTANCE: PocketVaultDatabase? = null

        fun getDatabase(context: Context): PocketVaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PocketVaultDatabase::class.java,
                    "pocketvault_room_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
