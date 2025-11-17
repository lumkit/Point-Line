package io.github.lumkit.pline

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import io.github.lumkit.pline.config.AppSettings
import io.github.lumkit.pline.config.LocalAppSettings
import io.github.lumkit.pline.config.LocalScreenNavController
import io.github.lumkit.pline.ui.screen.ScreenRoute
import io.github.lumkit.pline.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    AppConfiguration()
}


@Composable
private fun AppConfiguration() {
    val appSettings = remember { AppSettings() }
    val navController = rememberNavController()

    CompositionLocalProvider(
        LocalAppSettings provides appSettings,
        LocalScreenNavController provides navController,
    ) {
        AppUi()
    }
}

@Composable
private fun AppUi() {
    val settings = LocalAppSettings.current
    val navController = LocalScreenNavController.current

    AppTheme(
        darkTheme = settings.isDarkTheme.value,
    ) {
        NavHost(
            modifier = Modifier.fillMaxSize(),
            navController = navController,
            startDestination = ScreenRoute.Home,
            builder = ScreenRoute.routeRegister,
        )
    }
}
