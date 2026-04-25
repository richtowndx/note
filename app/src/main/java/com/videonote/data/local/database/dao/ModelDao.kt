package com.videonote.data.local.database.dao

import androidx.room.*
import com.videonote.data.local.database.entity.ModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {
    @Query("SELECT * FROM models ORDER BY createdAt DESC")
    fun getAllModels(): Flow<List<ModelEntity>>

    @Query("SELECT * FROM models WHERE providerId = :providerId")
    fun getModelsByProvider(providerId: String): Flow<List<ModelEntity>>

    @Query("SELECT * FROM models WHERE providerId = :providerId")
    suspend fun getModelsByProviderSync(providerId: String): List<ModelEntity>

    @Query("SELECT * FROM models WHERE id = :id")
    suspend fun getModelById(id: Int): ModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: ModelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<ModelEntity>)

    @Update
    suspend fun updateModel(model: ModelEntity)

    @Delete
    suspend fun deleteModel(model: ModelEntity)

    @Query("DELETE FROM models WHERE id = :id")
    suspend fun deleteModelById(id: Int)

    @Query("DELETE FROM models")
    suspend fun deleteAllModels()
}