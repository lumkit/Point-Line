package io.github.lumkit.pline.config

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.datastore.preferences.core.booleanPreferencesKey
import io.github.lumkit.pline.core.dataStore
import kotlinx.coroutines.flow.map

@Stable
class AppSettings {

    /**
     * 是否为暗黑主题
     */
    val isDarkTheme: State<Boolean>
        @Composable
        get() = dataStore.data.map {
            it[APP_DARK_THEME] ?: false
        }.collectAsState(initial = false)

    /**
     * 是否自动保存作品
     */
    val autoSaveWork: State<Boolean>
        @Composable
        get() = dataStore.data.map {
            it[APP_AUTO_SAVE_WORK] ?: true
        }.collectAsState(initial = true)

    /**
     * 设置是否为暗黑主题
     */
    suspend fun setDarkTheme(darkTheme: Boolean) {
        dataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply {
                this[APP_DARK_THEME] = darkTheme
            }
        }
    }

    /**
     * 设置是否自动保存作品
     */
    suspend fun setAutoSaveWork(autoSaveWork: Boolean) {
        dataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply {
                this[APP_AUTO_SAVE_WORK] = autoSaveWork
            }
        }
    }

    companion object {
        private val APP_DARK_THEME = booleanPreferencesKey("app_dark_theme")
        private val APP_AUTO_SAVE_WORK = booleanPreferencesKey("app_auto_save_work")
    }

}
