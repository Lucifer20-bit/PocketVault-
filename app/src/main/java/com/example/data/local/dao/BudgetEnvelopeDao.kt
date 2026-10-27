package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.BudgetEnvelopeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetEnvelopeDao {
    @Query("SELECT * FROM budget_envelopes ORDER BY category ASC")
    fun getAllEnvelopes(): Flow<List<BudgetEnvelopeEntity>>

    @Query("SELECT * FROM budget_envelopes WHERE month = :month ORDER BY category ASC")
    fun getEnvelopesByMonth(month: String): Flow<List<BudgetEnvelopeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnvelope(envelope: BudgetEnvelopeEntity): Long

    @Update
    suspend fun updateEnvelope(envelope: BudgetEnvelopeEntity)

    @Delete
    suspend fun deleteEnvelope(envelope: BudgetEnvelopeEntity)

    @Query("DELETE FROM budget_envelopes WHERE id = :id")
    suspend fun deleteEnvelopeById(id: Long)
}
