package io.github.lumkit.pline.ui.screen.paint

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import io.github.lumkit.pline.DrawingState
import io.github.lumkit.pline.graphics.Frame
import io.github.lumkit.pline.paint.FrameTool
import io.github.lumkit.pline.ui.theme.AppTheme
import io.github.lumkit.pline.ui.theme.ExtraLargeSpacing
import io.github.lumkit.pline.ui.theme.LargeSpacing
import io.github.lumkit.pline.ui.theme.SmallSpacing
import org.jetbrains.compose.resources.stringResource
import point_line.app.generated.resources.Res
import point_line.app.generated.resources.new_paint

@Composable
fun PaintPresetDialog(
    visibleState: State<Boolean>,
    onDismissRequest: () -> Unit,
    onCreateWork: (String, Frame) -> Unit,
    drawingState: DrawingState,
    navController: NavHostController,
) {
    if (visibleState.value) {
        var label by remember { mutableStateOf("") }

        Dialog(
            onDismissRequest = {
                onDismissRequest()
                navController.popBackStack()
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
            )
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = ExtraLargeSpacing),
                colors = CardDefaults.cardColors(
                    containerColor = AppTheme.colorScheme.surface,
                    contentColor = AppTheme.colorScheme.onSurface,
                ),
                shape = RoundedCornerShape(ExtraLargeSpacing)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .padding(vertical = ExtraLargeSpacing)
                ) {
                    Header()
                    Spacer(Modifier.height(LargeSpacing))
                    Content {
                        drawingState.frame = it
                        onCreateWork(label, it)
                        onDismissRequest()
                    }
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = LargeSpacing)
    ) {
        Text(
            text = stringResource(Res.string.new_paint),
            style = AppTheme.typography.titleLarge,
            color = AppTheme.colorScheme.text
        )
    }
}

@Composable
private fun Content(
    onSetFrame: (Frame) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = LargeSpacing),
        horizontalArrangement = Arrangement.spacedBy(SmallSpacing)
    ) {
        FrameTool.defaultTemplates.forEach { template ->
            SuggestionChip(
                onClick = {
                    onSetFrame(template.frame)
                },
                label = {
                    Text(
                        text = template.name,
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colorScheme.onSurface
                    )
                }
            )
        }
    }
}
