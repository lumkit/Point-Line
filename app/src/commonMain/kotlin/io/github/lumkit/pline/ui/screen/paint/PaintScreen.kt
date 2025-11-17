package io.github.lumkit.pline.ui.screen.paint

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.github.lumkit.pline.DrawingBoard
import io.github.lumkit.pline.DrawingState
import io.github.lumkit.pline.config.LocalScreenNavController
import io.github.lumkit.pline.db.AppDatabase
import io.github.lumkit.pline.rememberDrawingState
import io.github.lumkit.pline.ui.component.AppScaffold
import io.github.lumkit.pline.ui.screen.ScreenRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import point_line.app.generated.resources.Res
import point_line.app.generated.resources.not_naming

fun NavGraphBuilder.paintScreen() {
    composable<ScreenRoute.Paint> { navBackStackEntry ->
        val paint = navBackStackEntry.toRoute<ScreenRoute.Paint>()

        PaintScreen(paint = paint)
    }
}

@Composable
fun PaintScreen(
    paint: ScreenRoute.Paint,
    viewModel: PaintViewModel = viewModel { PaintViewModel(AppDatabase.instance) },
) {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val navController = LocalScreenNavController.current
    val newPaintDialogState = rememberSaveable { mutableStateOf(paint.id == null) }
    val paintState = rememberDrawingState()
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val defaultName = stringResource(Res.string.not_naming)

    LaunchedEffect(paintState) {
        if (paint.id != null) {
            viewModel.loadWork(paintState, paint.id)
        }
    }

    DisposableEffect(paintState) {
        onDispose {
            if (viewModel.workId.value != 0L) {
                CoroutineScope(Dispatchers.IO).launch {
                    async {
                        viewModel.saveWork(
                            state = paintState,
                            defaultLabel = defaultName,
                            id = viewModel.workId.value,
                        )
                    }
                    async {
                        viewModel.saveThumbnail(
                            state = paintState,
                            density = density,
                            layoutDirection = direction,
                            id = viewModel.workId.value,
                        )
                    }
                }
            }
        }
    }

    AppScaffold(
        modifier = Modifier.fillMaxSize()
    ) {
        WorkSpace(
            modifier = Modifier.fillMaxSize()
                .padding(it),
            drawingState = paintState,
        )
    }

    PaintPresetDialog(
        visibleState = newPaintDialogState,
        onDismissRequest = { newPaintDialogState.value = false },
        onCreateWork = {  label, _ ->
        scope.launch {
                viewModel.createPaint(
                    state = paintState,
                    label = label,
                )
            }
        },
        drawingState = paintState,
        navController = navController,
    )
}

@Composable
private fun WorkSpace(
    modifier: Modifier = Modifier,
    drawingState: DrawingState,
) {

    DrawingBoard(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFEBEBEB),
        state = drawingState,
    )
}
