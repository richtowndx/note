package com.videonote.data.local.database.dao

import androidx.room.*
import com.videonote.data.local.database.entity.ProviderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderDao {
    @Query("SELECT * FROM providers ORDER BY createdAt DESC")
    fun getAllProviders(): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM providers WHERE enabled = 1")
    fun getEnabledProviders(): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM providers WHERE id = :id")
    suspend fun getProviderById(id: String): ProviderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: ProviderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProviders(providers: List<ProviderEntity>)

    @Update
    suspend fun updateProvider(provider: ProviderEntity)

    @Delete
    suspend fun deleteProvider(provider: ProviderEntity)

    @Query("DELETE FROM providers WHERE id = :id")
    suspend fun deleteProviderById(id: String)

    @Query("DELETE FROM providers")
    suspend fun deleteAllProviders()

    @Query("UPDATE providers SET enabled = :enabled WHERE id = :id")
    suspend fun updateProviderEnabled(id: String, enabled: Int)
}