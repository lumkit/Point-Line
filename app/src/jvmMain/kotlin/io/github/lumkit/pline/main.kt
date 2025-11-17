package io.github.lumkit.pline

import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.jetbrains.compose.resources.stringResource
import point_line.app.generated.resources.Res
import point_line.app.generated.resources.app_name

fun main() = application {
    val state = rememberWindowState(
        position = WindowPosition(Alignment.Center),
    )
    Window(
        state = state,
        onCloseRequest = ::exitApplication,
        title = stringResource(Res.string.app_name),
    ) {
        App()
    }
}
