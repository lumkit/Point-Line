package io.github.lumkit.pline.db.repo

import io.github.lumkit.pline.DrawingState
import io.github.lumkit.pline.db.AppDatabase
import io.github.lumkit.pline.db.entity.Work
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalTime::class)
class WorkRepository(private val db: AppDatabase) {

    suspend fun create(label: String, frameWidth: Int? = null, frameHeight: Int? = null, contentJson: String, coverThumb: ByteArray? = null, coverThumbWidth: Int? = null, coverThumbHeight: Int? = null, coverThumbMime: String? = null): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        return db.workDao().insert(
            Work(
                label = label,
                frameWidth = frameWidth,
                frameHeight = frameHeight,
                updateAt = now,
                contentJson = contentJson,
            )
        )
    }

    suspend fun saveState(workId: Long, state: DrawingState, encoding: String = "utf-8", version: Int = 1, chunkSize: Int = 262_144) {
        val json = state.export()
        val bytes = json.toByteArray(Charsets.UTF_8)
        db.workContentChunkDao().saveWorkContent(workId, bytes, encoding, version, chunkSize)
        val w = db.workDao().getById(workId) ?: return
        val now = Clock.System.now().toEpochMilliseconds()
        db.workDao().update(
            w.copy(
                contentJson = json,
                contentVersion = version,
                contentType = "json",
                contentEncoding = encoding,
                frameWidth = state.frame.width,
                frameHeight = state.frame.height,
                updateAt = now,
            )
        )
    }

    suspend fun loadStateJson(workId: Long): String? {
        val (bytes, _) = db.workContentChunkDao().loadWorkContent(workId)
        return if(bytes.isNotEmpty()) {
            bytes.decodeToString()
        } else {
            val w = db.workDao().getById(workId)
            w?.contentJson
        } ?: ""
    }

    suspend fun setCoverThumb(workId: Long, coverBytes: ByteArray, width: Int, height: Int, mime: String) {
        val w = db.workDao().getById(workId) ?: return
        val now = Clock.System.now().toEpochMilliseconds()
        db.workDao().update(
            w.copy(
                coverThumb = coverBytes,
                coverThumbWidth = width,
                coverThumbHeight = height,
                coverThumbMime = mime,
                coverUpdatedAt = now,
                updateAt = now,
            )
        )
    }

    suspend fun updateLabel(workId: Long, label: String) {
        val w = db.workDao().getById(workId) ?: return
        val now = Clock.System.now().toEpochMilliseconds()
        db.workDao().update(w.copy(label = label, updateAt = now))
    }

    suspend fun getWork(workId: Long): Work? = db.workDao().getById(workId)

    suspend fun markDeleted(workId: Long, deleted: Boolean) {
        val w = db.workDao().getById(workId) ?: return
        val now = Clock.System.now().toEpochMilliseconds()
        db.workDao().update(w.copy(deleted = deleted, updateAt = now))
    }

    suspend fun get(workId: Long): Work? = db.workDao().getById(workId)

    suspend fun listActive(): List<Work> = db.workDao().listActive()
    
    fun observe(workId: Long): Flow<Work?> = db.workDao().observeById(workId)
    
    fun observeActive(): Flow<List<Work>> = db.workDao().observeActive()
}
