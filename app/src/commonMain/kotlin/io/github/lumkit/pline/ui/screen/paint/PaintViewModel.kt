package io.github.lumkit.pline.ui.screen.paint

import io.github.lumkit.pline.DrawingState
import io.github.lumkit.pline.base.BaseViewModel
import io.github.lumkit.pline.db.AppDatabase
import io.github.lumkit.pline.db.repo.WorkRepository
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.github.lumkit.pline.db.entity.Work
import io.github.lumkit.pline.graphics.StrokeModel
import io.github.lumkit.pline.graphics.rasterizePathStrokeToCanvas
import io.github.lumkit.pline.graphics.rasterizeStampStrokeToCanvas
import io.github.lumkit.pline.util.encodeImageBitmapToPng
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

enum class PaintScreenMode {
    DRAG,
    BRUSH,
    ERASER,
    LAYER,
}

class PaintViewModel(
    db: AppDatabase
): BaseViewModel() {

    private val repository = WorkRepository(db)
    private val _workId = MutableStateFlow(0L)
    val workId: StateFlow<Long> = _workId

    private val _work = MutableStateFlow<Work?>(null)
    val work: StateFlow<Work?> = _work

    private val _label = MutableStateFlow("")
    val label: StateFlow<String> = _label

    private val _screenMode = MutableStateFlow(PaintScreenMode.DRAG)
    val screenMode: StateFlow<PaintScreenMode> = _screenMode

    suspend fun createPaint(state: DrawingState, label: String, density: Density, layoutDirection: LayoutDirection) {
        val id = repository.create(
            label = label,
            frameWidth = state.frame.width,
            frameHeight = state.frame.height,
            contentJson = state.export(),
        )
        saveThumbnail(state, density, layoutDirection, id)
        _workId.value = id
    }

    suspend fun loadWork(state: DrawingState, id: Long?) {
        if (id == null) return
        state.import(repository.loadStateJson(id) ?: "")
        _workId.value = id
        _work.value = repository.getWork(id)
        _work.value?.label?.let { label ->
            _label.value = label
        }
    }

    suspend fun saveWork(state: DrawingState, defaultLabel: String, id: Long?) {
        if (id == null) return
        if (_label.value.isEmpty()) {
            repository.updateLabel(id, "$defaultLabel-$id")
        }
        repository.saveState(id, state)
    }

    suspend fun saveThumbnail(state: DrawingState, density: Density, layoutDirection: LayoutDirection, id: Long?, targetWidth: Int = 480) {
        if (id == null) return
        val fw = state.frame.width
        val fh = state.frame.height
        if (fw <= 0 || fh <= 0) return
        val full = state.backingBitmap ?: run {
            val bmp = ImageBitmap(fw, fh)
            val canvas = Canvas(bmp)
            val ds = CanvasDrawScope()
            ds.draw(density = density, layoutDirection = layoutDirection, canvas = canvas, size = Size(fw.toFloat(), fh.toFloat())) {
                drawRect(color = Color(state.frame.color), size = Size(fw.toFloat(), fh.toFloat()))
            }
            val tiles = state.tiles
            if (tiles.isNotEmpty()) {
                val dsTiles = CanvasDrawScope()
                dsTiles.draw(density = density, layoutDirection = layoutDirection, canvas = canvas, size = Size(fw.toFloat(), fh.toFloat())) {
                    tiles.forEach { t ->
                        drawImage(image = t.bitmap, topLeft = Offset(t.originX.toFloat(), t.originY.toFloat()))
                    }
                }
            } else {
                state.strokes.forEach { s ->
                    if (s.model == StrokeModel.Stamp) {
                        rasterizeStampStrokeToCanvas(canvas, s, fw, fh)
                    } else {
                        rasterizePathStrokeToCanvas(canvas, s, fw, fh)
                    }
                }
            }
            bmp
        }
        val tw = targetWidth.coerceAtLeast(1)
        val th = (fh.toFloat() / fw.toFloat() * tw).roundToInt().coerceAtLeast(1)
        val thumb = ImageBitmap(tw, th)
        val tCanvas = Canvas(thumb)
        val ds2 = CanvasDrawScope()
        ds2.draw(density = density, layoutDirection = layoutDirection, canvas = tCanvas, size = Size(tw.toFloat(), th.toFloat())) {
            withTransform({ scale(scaleX = tw.toFloat() / fw.toFloat(), scaleY = th.toFloat() / fh.toFloat(), pivot = Offset.Zero) }) {
                drawImage(image = full, topLeft = Offset.Zero)
            }
        }
        val bytes = encodeImageBitmapToPng(thumb)
        repository.setCoverThumb(id, bytes, tw, th, "image/png")
    }

    @OptIn(ExperimentalTime::class)
    suspend fun autoSaveWork(
        state: DrawingState,
        defaultLabel: String,
        density: Density,
        direction: LayoutDirection,
        onComplete: suspend () -> Unit,
    ) {
        try {
            if (workId.value != 0L) {
                val snapshotTime = Clock.System.now().toEpochMilliseconds()
                saveWork(
                    state = state,
                    defaultLabel = defaultLabel,
                    id = workId.value,
                )
                saveThumbnail(
                    state = state,
                    density = density,
                    layoutDirection = direction,
                    id = workId.value,
                )
                if (Clock.System.now().toEpochMilliseconds() - snapshotTime < 1000) {
                    delay(1000)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            onComplete()
        }
    }
}
