package io.github.lumkit.pline.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.lumkit.pline.db.entity.WorkContentChunk

@Dao
abstract class WorkContentChunkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(chunk: WorkContentChunk)

    @Query("DELETE FROM tb_work_chunk WHERE workId = :workId")
    abstract suspend fun deleteByWorkId(workId: Long)

    @Query("SELECT * FROM tb_work_chunk WHERE workId = :workId ORDER BY seq ASC")
    abstract suspend fun listByWorkId(workId: Long): List<WorkContentChunk>

    @Transaction
    open suspend fun saveWorkContent(workId: Long, data: ByteArray, encoding: String, version: Int, chunkSize: Int = 262_144) {
        deleteByWorkId(workId)
        var seq = 0
        var offset = 0
        while (offset < data.size) {
            val end = kotlin.math.min(offset + chunkSize, data.size)
            val slice = data.copyOfRange(offset, end)
            insert(WorkContentChunk(workId = workId, seq = seq, bytes = slice, encoding = encoding, version = version))
            seq++
            offset = end
        }
    }

    @Transaction
    open suspend fun loadWorkContent(workId: Long): Pair<ByteArray, String?> {
        val chunks = listByWorkId(workId)
        if (chunks.isEmpty()) return Pair(ByteArray(0), null)
        val total = chunks.sumOf { it.bytes.size }
        val out = ByteArray(total)
        var p = 0
        chunks.forEach { c ->
            c.bytes.copyInto(out, destinationOffset = p)
            p += c.bytes.size
        }
        val enc = chunks.firstOrNull()?.encoding
        return Pair(out, enc)
    }
}