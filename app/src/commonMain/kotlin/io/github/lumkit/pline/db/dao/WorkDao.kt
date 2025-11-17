package io.github.lumkit.pline.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.lumkit.pline.db.entity.Work
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(work: Work): Long

    @Update
    suspend fun update(work: Work)

    @Query("SELECT * FROM tb_work WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Work?

    @Query("SELECT * FROM tb_work WHERE deleted = 0 ORDER BY createAt DESC")
    suspend fun listActive(): List<Work>

    @Query("SELECT * FROM tb_work WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<Work?>

    @Query("SELECT * FROM tb_work WHERE deleted = 0 ORDER BY createAt DESC")
    fun observeActive(): Flow<List<Work>>
}
