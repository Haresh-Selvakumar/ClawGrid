package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AuditEventEntity
import com.example.data.model.RoomSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditEventDao {
    @Query("SELECT * FROM audit_events ORDER BY epochMs DESC LIMIT 300")
    fun observeEvents(): Flow<List<AuditEventEntity>>

    @Query("SELECT * FROM audit_events ORDER BY epochMs DESC LIMIT 100")
    suspend fun getRecentEvents(): List<AuditEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: AuditEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<AuditEventEntity>)

    @Query("UPDATE audit_events SET syncedToSupabase = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("DELETE FROM audit_events")
    suspend fun clearAll()
}

@Dao
interface RoomSnapshotDao {
    @Query("SELECT * FROM room_snapshots")
    suspend fun getAllRooms(): List<RoomSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRooms(rooms: List<RoomSnapshotEntity>)
}

@Database(
    entities = [AuditEventEntity::class, RoomSnapshotEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ClawGridDatabase : RoomDatabase() {
    abstract fun auditEventDao(): AuditEventDao
    abstract fun roomSnapshotDao(): RoomSnapshotDao

    companion object {
        @Volatile
        private var INSTANCE: ClawGridDatabase? = null

        fun getInstance(context: Context): ClawGridDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ClawGridDatabase::class.java,
                    "clawgrid_v2.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
