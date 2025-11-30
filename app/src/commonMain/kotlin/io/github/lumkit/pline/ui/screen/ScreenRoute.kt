package io.github.lumkit.pline.ui.screen

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import io.github.lumkit.pline.ui.screen.home.homeScreen
import io.github.lumkit.pline.ui.screen.paint.paintScreen
import kotlinx.serialization.Serializable

sealed class ScreenRoute {

    @Serializable
    data object Home : ScreenRoute()

    @Serializable
    data class Paint(
        val id: Long? = null,
    ) : ScreenRoute()

    companion object {
        /**
         * 路由注册
         */
        internal val routeRegister: NavGraphBuilder.() -> Unit = {
            homeScreen()
            paintScreen()
        }
    }
}

fun NavController.navigateToPaint(id: Long?) {
    navigate(ScreenRoute.Paint(id)) {
        restoreState = true
        launchSingleTop = true
    }
}
