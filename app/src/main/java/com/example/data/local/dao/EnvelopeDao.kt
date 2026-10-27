package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.EnvelopeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EnvelopeDao {
    @Query("SELECT * FROM envelopes ORDER BY category ASC")
    fun getAllEnvelopes(): Flow<List<EnvelopeEntity>>

    @Query("SELECT * FROM envelopes WHERE month = :month ORDER BY category ASC")
    fun getEnvelopesByMonth(month: String): Flow<List<EnvelopeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnvelope(envelope: EnvelopeEntity): Long

    @Update
    suspend fun updateEnvelope(envelope: EnvelopeEntity)

    @Delete
    suspend fun deleteEnvelope(envelope: EnvelopeEntity)

    @Query("DELETE FROM envelopes WHERE id = :id")
    suspend fun deleteEnvelopeById(id: Long)
}
