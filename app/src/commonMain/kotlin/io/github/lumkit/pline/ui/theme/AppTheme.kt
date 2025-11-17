package io.github.lumkit.pline.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.lumkit.pline.ui.theme.color.AppColorScheme
import io.github.lumkit.pline.ui.theme.color.darkAppColorScheme
import io.github.lumkit.pline.ui.theme.color.lightAppColorScheme
import io.github.lumkit.pline.ui.theme.text.AppTypography

val LocalAppColorScheme = staticCompositionLocalOf { lightAppColorScheme() }
val LocalAppTypography = staticCompositionLocalOf { AppTypography }

object AppTheme {
    val colorScheme: AppColorScheme
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current
    val typography: Typography
        @Composable @ReadOnlyComposable get() = LocalAppTypography.current
}

@Composable
fun AppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val appColorScheme = when (darkTheme) {
        true -> darkAppColorScheme()
        false -> lightAppColorScheme()
    }

    CompositionLocalProvider(
        LocalAppColorScheme provides appColorScheme,
        LocalAppTypography provides AppTypography,
    ) {
        content()
    }
}
