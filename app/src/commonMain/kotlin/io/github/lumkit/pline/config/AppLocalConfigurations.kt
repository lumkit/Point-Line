package io.github.lumkit.pline.config

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavHostController

/**
 * 应用设置
 */
val LocalAppSettings = compositionLocalOf<AppSettings> {
    error("No app settings provided")
}

/**
 * 屏幕导航控制器
 */
val LocalScreenNavController = compositionLocalOf<NavHostController> {
    error("No nav controller provided")
}
