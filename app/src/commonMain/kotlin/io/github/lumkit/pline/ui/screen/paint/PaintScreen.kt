package io.github.lumkit.pline.ui.screen.paint

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.github.lumkit.pline.DrawingBoard
import io.github.lumkit.pline.DrawingState
import io.github.lumkit.pline.config.LocalAppSettings
import io.github.lumkit.pline.config.LocalScreenNavController
import io.github.lumkit.pline.db.AppDatabase
import io.github.lumkit.pline.rememberDrawingState
import io.github.lumkit.pline.ui.component.AppScaffold
import io.github.lumkit.pline.ui.component.LoadingDialog
import io.github.lumkit.pline.ui.screen.ScreenRoute
import kotlinx.coroutines.*
import org.jetbrains.compose.resources.stringResource
import point_line.app.generated.resources.Res
import point_line.app.generated.resources.not_naming
import kotlin.time.ExperimentalTime

fun NavGraphBuilder.paintScreen() {
    composable<ScreenRoute.Paint> { navBackStackEntry ->
        val paint = navBackStackEntry.toRoute<ScreenRoute.Paint>()

        PaintScreen(paint = paint)
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalTime::class)
@Composable
fun PaintScreen(
    paint: ScreenRoute.Paint,
    viewModel: PaintViewModel = viewModel { PaintViewModel(AppDatabase.instance) },
) {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val navController = LocalScreenNavController.current
    val settings = LocalAppSettings.current

    val newPaintDialogState = rememberSaveable { mutableStateOf(paint.id == null) }
    val paintState = rememberDrawingState()
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val defaultName = stringResource(Res.string.not_naming)

    val loadingDialogState = rememberSaveable { mutableStateOf(false) }
    val autoSaveWork by settings.autoSaveWork

    LaunchedEffect(paintState) {
        if (paint.id != null) {
            viewModel.loadWork(paintState, paint.id)
        }
    }

    BackHandler(
        !newPaintDialogState.value,
    ) {
        if (!autoSaveWork) {
            navController.popBackStack()
            return@BackHandler
        }

        loadingDialogState.value = true
        scope.launch {
            viewModel.autoSaveWork(
                state = paintState,
                defaultLabel = defaultName,
                density = density,
                direction = direction,
            ) {
                loadingDialogState.value = false
                withContext(Dispatchers.Main) {
                    navController.popBackStack()
                }
            }
        }
    }

    AppScaffold(
        modifier = Modifier.fillMaxSize()
    ) {
        WorkSpace(
            modifier = Modifier.fillMaxSize(),
            paddings = it,
            drawingState = paintState,
        )
    }

    PaintPresetDialog(
        visibleState = newPaintDialogState,
        onDismissRequest = { newPaintDialogState.value = false },
        onCreateWork = { label, _ ->
            scope.launch {
                loadingDialogState.value = true
                try {
                    viewModel.createPaint(
                        state = paintState,
                        label = label,
                        density = density,
                        layoutDirection = direction,
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    newPaintDialogState.value = false
                    loadingDialogState.value = false
                }
            }
        },
        drawingState = paintState,
        navController = navController,
    )

    LoadingDialog(
        loadingDialogState = loadingDialogState,
    )
}


@Composable
private fun WorkSpace(
    modifier: Modifier = Modifier,
    paddings: PaddingValues,
    drawingState: DrawingState,
) {

    Box(
        modifier = modifier
    ) {
        DrawingBoard(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFEBEBEB),
            state = drawingState,
        )

    }
}

