package com.example.polyglottyper.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.polyglottyper.domain.model.WpmPoint
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "game_results",
    // Indices back the WHERE/ORDER BY/GROUP BY columns of every query below.
    indices = [Index("timestamp"), Index("languageCode"), Index("layoutId")],
)
data class GameResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val languageCode: String,
    val layoutId: String,
    val modeId: String,
    val wpm: Int,
    val accuracy: Int,
    val correctWords: Int,
    val incorrectWords: Int,
    val durationSeconds: Int,
    val timestamp: Long,
)

/** Query-result POJO for the per-language aggregate query. */
data class LanguageAggregateRow(
    val languageCode: String,
    val bestWpm: Int,
    val avgAccuracy: Double,
    val gamesPlayed: Int,
)

/** Query-result POJO for the per-layout aggregate query. */
data class LayoutAggregateRow(
    val layoutId: String,
    val bestWpm: Int,
    val avgAccuracy: Double,
    val gamesPlayed: Int,
)

@Dao
interface GameResultDao {

    // Returns the new row id (useful for future relations); callers that don't
    // need it simply ignore it.
    @Insert
    suspend fun insert(result: GameResultEntity): Long

    @Query("SELECT * FROM game_results ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentResults(limit: Int): Flow<List<GameResultEntity>>

    @Query(
        """
        SELECT languageCode,
               MAX(wpm)      AS bestWpm,
               AVG(accuracy) AS avgAccuracy,
               COUNT(*)      AS gamesPlayed
        FROM game_results
        GROUP BY languageCode
        """
    )
    fun observeLanguageAggregates(): Flow<List<LanguageAggregateRow>>

    @Query(
        """
        SELECT layoutId,
               MAX(wpm)      AS bestWpm,
               AVG(accuracy) AS avgAccuracy,
               COUNT(*)      AS gamesPlayed
        FROM game_results
        GROUP BY layoutId
        """
    )
    fun observeLayoutAggregates(): Flow<List<LayoutAggregateRow>>

    /** Drives the WPM sparkline; Room maps the two columns onto [WpmPoint]. */
    @Query("SELECT wpm, timestamp FROM game_results ORDER BY timestamp DESC LIMIT :limit")
    fun observeWpmHistory(limit: Int): Flow<List<WpmPoint>>

    // ── One-shot rollups for achievement evaluation ──────────

    @Query("SELECT COUNT(*) FROM game_results")
    suspend fun countAll(): Int

    @Query("SELECT MAX(wpm) FROM game_results")
    suspend fun maxWpm(): Int?

    @Query("SELECT MAX(correctWords) FROM game_results WHERE accuracy = 100")
    suspend fun maxPerfectRun(): Int?

    @Query("SELECT COUNT(DISTINCT languageCode) FROM game_results")
    suspend fun countLanguages(): Int

    @Query("SELECT COUNT(DISTINCT layoutId) FROM game_results")
    suspend fun countLayouts(): Int

    @Query("SELECT MAX(durationSeconds) FROM game_results WHERE modeId = 'RAIN'")
    suspend fun maxRainSeconds(): Int?

    @Query("SELECT MAX(correctWords) FROM game_results WHERE modeId = 'RAIN'")
    suspend fun maxRainWords(): Int?
}

@Database(entities = [GameResultEntity::class], version = 2, exportSchema = true)
abstract class TypingDatabase : RoomDatabase() {
    abstract fun gameResultDao(): GameResultDao

    companion object {
        /**
         * v1 users keep their full history; pre-layout results are attributed
         * to QWERTY. Index names match exactly what Room expects from the
         * entity annotations, so schema validation passes after migration.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE game_results ADD COLUMN layoutId TEXT NOT NULL DEFAULT 'qwerty'"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_game_results_timestamp` " +
                        "ON `game_results`(`timestamp`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_game_results_languageCode` " +
                        "ON `game_results`(`languageCode`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_game_results_layoutId` " +
                        "ON `game_results`(`layoutId`)"
                )
            }
        }
    }
}
