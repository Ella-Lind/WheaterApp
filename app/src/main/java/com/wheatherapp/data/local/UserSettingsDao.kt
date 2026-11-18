package com.wheatherapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wheatherapp.data.local.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for accessing single-row user settings.
 */
@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getUserSettings(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getUserSettingsSync(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: UserSettingsEntity)
}
