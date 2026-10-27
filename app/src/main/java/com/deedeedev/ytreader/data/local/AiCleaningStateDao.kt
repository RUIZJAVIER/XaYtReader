package com.deedeedev.ytreader.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AiCleaningStateDao {
    @Query("SELECT * FROM ai_cleaning_states WHERE subtitleId = :subtitleId")
    fun observeBySubtitleId(subtitleId: Long): Flow<AiCleaningStateEntity?>

    @Query("SELECT * FROM ai_cleaning_states WHERE subtitleId = :subtitleId")
    suspend fun getBySubtitleId(subtitleId: Long): AiCleaningStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(state: AiCleaningStateEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(state: AiCleaningStateEntity)

    @Query("UPDATE ai_cleaning_states SET aiCleaningInProgress = 1, aiCleaningSourceText = :sourceText, aiCleaningErrorSummary = NULL, aiCleaningErrorLog = NULL, aiCleaningPendingResult = NULL, aiCleaningUpdatedAt = :updatedAt WHERE subtitleId = :subtitleId")
    suspend fun markAiCleaningQueuedExistingRow(subtitleId: Long, sourceText: String, updatedAt: Long)

    @Query("UPDATE ai_cleaning_states SET aiCleaningPendingResult = :result, aiCleaningSourceText = NULL, aiCleaningInProgress = 0, aiCleaningUpdatedAt = :updatedAt WHERE subtitleId = :subtitleId")
    suspend fun storeAiCleaningResultExistingRow(subtitleId: Long, result: String, updatedAt: Long)

    @Query("UPDATE ai_cleaning_states SET aiCleaningErrorSummary = :summary, aiCleaningErrorLog = :log, aiCleaningSourceText = NULL, aiCleaningInProgress = 0, aiCleaningUpdatedAt = :updatedAt WHERE subtitleId = :subtitleId")
    suspend fun storeAiCleaningFailureExistingRow(subtitleId: Long, summary: String?, log: String?, updatedAt: Long)

    @Query("UPDATE ai_cleaning_states SET aiCleaningInProgress = 0, aiCleaningSourceText = NULL, aiCleaningPendingResult = NULL, aiCleaningErrorSummary = NULL, aiCleaningErrorLog = NULL, aiCleaningUpdatedAt = :updatedAt WHERE subtitleId = :subtitleId")
    suspend fun clearAiCleaningStateExistingRow(subtitleId: Long, updatedAt: Long)

    @Transaction
    suspend fun markAiCleaningQueued(subtitleId: Long, sourceText: String, updatedAt: Long) {
        insertIfNotExists(AiCleaningStateEntity(subtitleId = subtitleId, aiCleaningUpdatedAt = updatedAt))
        markAiCleaningQueuedExistingRow(subtitleId, sourceText, updatedAt)
    }

    @Transaction
    suspend fun storeAiCleaningResult(subtitleId: Long, result: String, updatedAt: Long) {
        insertIfNotExists(AiCleaningStateEntity(subtitleId = subtitleId, aiCleaningUpdatedAt = updatedAt))
        storeAiCleaningResultExistingRow(subtitleId, result, updatedAt)
    }

    @Transaction
    suspend fun storeAiCleaningFailure(subtitleId: Long, summary: String?, log: String?, updatedAt: Long) {
        insertIfNotExists(AiCleaningStateEntity(subtitleId = subtitleId, aiCleaningUpdatedAt = updatedAt))
        storeAiCleaningFailureExistingRow(subtitleId, summary, log, updatedAt)
    }

    @Transaction
    suspend fun cancelAiCleaning(subtitleId: Long, updatedAt: Long) {
        insertIfNotExists(AiCleaningStateEntity(subtitleId = subtitleId, aiCleaningUpdatedAt = updatedAt))
        clearAiCleaningStateExistingRow(subtitleId, updatedAt)
    }

    @Query("UPDATE ai_cleaning_states SET aiCleaningPendingResult = NULL WHERE subtitleId = :subtitleId")
    suspend fun clearAiCleaningResult(subtitleId: Long)

    @Query("UPDATE ai_cleaning_states SET aiCleaningErrorSummary = NULL, aiCleaningErrorLog = NULL WHERE subtitleId = :subtitleId")
    suspend fun clearAiCleaningError(subtitleId: Long)

    @Query("DELETE FROM ai_cleaning_states WHERE subtitleId = :subtitleId")
    suspend fun delete(subtitleId: Long)
}
