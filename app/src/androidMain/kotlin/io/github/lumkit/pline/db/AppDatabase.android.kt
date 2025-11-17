package io.github.lumkit.pline.db

import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.lumkit.pline.application

actual fun AppDatabase.Companion.builder(): RoomDatabase.Builder<AppDatabase> {
    val appContext = application
    val dbFile = appContext.getDatabasePath(DB_NAME)
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}
