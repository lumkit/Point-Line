package io.github.lumkit.pline.db.entity

import androidx.room.Entity
import androidx.room.Index
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "tb_work_chunk",
    primaryKeys = ["workId", "seq"],
    indices = [Index(value = ["workId"])]
)
data class WorkContentChunk(
    val workId: Long,
    val seq: Int,
    val bytes: ByteArray,
    val encoding: String,
    val version: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as WorkContentChunk

        if (workId != other.workId) return false
        if (seq != other.seq) return false
        if (version != other.version) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (encoding != other.encoding) return false

        return true
    }

    override fun hashCode(): Int {
        var result = workId.hashCode()
        result = 31 * result + seq
        result = 31 * result + version
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + encoding.hashCode()
        return result
    }
}
