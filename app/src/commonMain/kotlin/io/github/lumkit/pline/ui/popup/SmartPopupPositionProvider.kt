package io.github.lumkit.pline.ui.popup

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import kotlin.math.roundToInt

class SmartPopupPositionProvider(
    private val density: Density,
    private val preferredDirection: Direction = Direction.Below,
    private val margin: Dp = 8.dp
) : PopupPositionProvider {

    enum class Direction { Above, Below, Left, Right }

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        return with(density) {
            val marginPx = margin.toPx().roundToInt()
            val belowY = anchorBounds.bottom
            val aboveY = anchorBounds.top - popupContentSize.height

            val xCenter = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
            val xLeft = anchorBounds.left
            val xRight = anchorBounds.right - popupContentSize.width

            // Candidate positions
            val candidates = when (preferredDirection) {
                Direction.Below -> listOf(
                    IntOffset(xCenter, belowY),
                    IntOffset(xLeft, belowY),
                    IntOffset(xRight, belowY),
                    IntOffset(xCenter, aboveY),
                    IntOffset(xLeft, aboveY),
                    IntOffset(xRight, aboveY)
                )
                Direction.Above -> listOf(
                    IntOffset(xCenter, aboveY),
                    IntOffset(xLeft, aboveY),
                    IntOffset(xRight, aboveY),
                    IntOffset(xCenter, belowY),
                    IntOffset(xLeft, belowY),
                    IntOffset(xRight, belowY)
                )
                Direction.Left -> listOf(
                    IntOffset(anchorBounds.left - popupContentSize.width, anchorBounds.top),
                    IntOffset(anchorBounds.left - popupContentSize.width, anchorBounds.bottom - popupContentSize.height),
                    IntOffset(anchorBounds.right, anchorBounds.top)
                )
                Direction.Right -> listOf(
                    IntOffset(anchorBounds.right, anchorBounds.top),
                    IntOffset(anchorBounds.right, anchorBounds.bottom - popupContentSize.height),
                    IntOffset(anchorBounds.left - popupContentSize.width, anchorBounds.top)
                )
            }

            val fit = candidates.firstOrNull { candidate ->
                candidate.x >= marginPx &&
                        candidate.y >= marginPx &&
                        candidate.x + popupContentSize.width <= windowSize.width - marginPx &&
                        candidate.y + popupContentSize.height <= windowSize.height - marginPx
            } ?: candidates.first() // fallback to first if none fit

            // Clamp to window rect with margins to be safe
            val clampedX = fit.x.coerceIn(marginPx, windowSize.width - popupContentSize.width - marginPx)
            val clampedY = fit.y.coerceIn(marginPx, windowSize.height - popupContentSize.height - marginPx)

            IntOffset(clampedX, clampedY)
        }
    }
}
