package io.github.lumkit.pline.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.lumkit.pline.db.dao.WorkContentChunkDao
import io.github.lumkit.pline.db.dao.WorkDao
import io.github.lumkit.pline.db.entity.Work
import io.github.lumkit.pline.db.entity.WorkContentChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [Work::class, WorkContentChunk::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workDao(): WorkDao
    abstract fun workContentChunkDao(): WorkContentChunkDao

    companion object {
        const val DB_NAME = "point_line_app.db"

        val instance: AppDatabase by lazy {
            builder()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
    }
}

expect fun AppDatabase.Companion.builder(): RoomDatabase.Builder<AppDatabase>

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
