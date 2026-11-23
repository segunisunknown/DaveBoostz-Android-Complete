package com.daveboostz.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "game_sessions")
data class GameSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val gameName: String,
    val startedAtMs: Long,
    val durationSec: Long,
    val avgFps: Int?,
    val maxTempC: Float,
    val profileUsed: String
)

@Entity(tableName = "per_game_settings")
data class PerGameSettingsEntity(
    @PrimaryKey val packageName: String,
    val overlayEnabled: Boolean = true,
    val autoBoostEnabled: Boolean = true,
    val targetFps: Int = 60,
    val rendererBackend: String = "OpenGL",
    val blockNotifications: Boolean = true,
    val smoothSystemUi: Boolean = false
)

@Dao
interface DaveBoostzDao {
    @Query("SELECT * FROM game_sessions ORDER BY startedAtMs DESC")
    fun observeSessions(): Flow<List<GameSessionEntity>>

    @Insert
    suspend fun insertSession(session: GameSessionEntity)

    @Query("DELETE FROM game_sessions")
    suspend fun deleteAllSessions()

    @Query("SELECT * FROM per_game_settings ORDER BY packageName ASC")
    fun observeAllSettings(): Flow<List<PerGameSettingsEntity>>

    @Query("SELECT * FROM per_game_settings WHERE packageName = :pkg LIMIT 1")
    suspend fun getSettings(pkg: String): PerGameSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: PerGameSettingsEntity)
}

@Database(
    entities = [GameSessionEntity::class, PerGameSettingsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DaveBoostzDatabase : RoomDatabase() {
    abstract fun dao(): DaveBoostzDao
}
