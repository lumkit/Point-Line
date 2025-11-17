package io.github.lumkit.pline.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Serializable
@Entity(
    tableName = "tb_work",
    indices = [
        Index(value = ["label"]),
        Index(value = ["updateAt"]),
        Index(value = ["deleted"]),
    ]
)
data class Work(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val frameWidth: Int? = null,
    val frameHeight: Int? = null,
    val contentJson: String? = null,
    val contentVersion: Int = 1,
    val contentType: String? = "json",
    val contentEncoding: String? = null,
    val coverThumb: ByteArray? = null,
    val coverThumbWidth: Int? = null,
    val coverThumbHeight: Int? = null,
    val coverThumbMime: String? = null,
    val coverUpdatedAt: Long = 0,
    val createAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updateAt: Long = 0,
    val deleted: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Work

        if (id != other.id) return false
        if (frameWidth != other.frameWidth) return false
        if (frameHeight != other.frameHeight) return false
        if (contentVersion != other.contentVersion) return false
        if (coverThumbWidth != other.coverThumbWidth) return false
        if (coverThumbHeight != other.coverThumbHeight) return false
        if (coverUpdatedAt != other.coverUpdatedAt) return false
        if (createAt != other.createAt) return false
        if (updateAt != other.updateAt) return false
        if (deleted != other.deleted) return false
        if (label != other.label) return false
        if (contentJson != other.contentJson) return false
        if (contentType != other.contentType) return false
        if (contentEncoding != other.contentEncoding) return false
        if (!coverThumb.contentEquals(other.coverThumb)) return false
        if (coverThumbMime != other.coverThumbMime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (frameWidth ?: 0)
        result = 31 * result + (frameHeight ?: 0)
        result = 31 * result + contentVersion
        result = 31 * result + (coverThumbWidth ?: 0)
        result = 31 * result + (coverThumbHeight ?: 0)
        result = 31 * result + coverUpdatedAt.hashCode()
        result = 31 * result + createAt.hashCode()
        result = 31 * result + updateAt.hashCode()
        result = 31 * result + deleted.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + (contentJson?.hashCode() ?: 0)
        result = 31 * result + (contentType?.hashCode() ?: 0)
        result = 31 * result + (contentEncoding?.hashCode() ?: 0)
        result = 31 * result + (coverThumb?.contentHashCode() ?: 0)
        result = 31 * result + (coverThumbMime?.hashCode() ?: 0)
        return result
    }
}
