package io.github.lumkit.pline.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import io.github.lumkit.pline.ui.theme.AppTheme

object TextFieldDefaults {
    val cursorBrush: Brush
        @Composable get() = SolidColor(AppTheme.colorScheme.text)
}
