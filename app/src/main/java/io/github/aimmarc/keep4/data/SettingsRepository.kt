package io.github.aimmarc.keep4.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.keep4DataStore by preferencesDataStore(name = "keep4_settings")

data class AppSettings(
    val enabled: Boolean = false,
    val startOnBoot: Boolean = false,
    val hideFromRecents: Boolean = false,
)

class SettingsRepository(context: Context) {
    private val dataStore = context.applicationContext.keep4DataStore

    val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { preferences ->
            AppSettings(
                enabled = preferences[Keys.ENABLED] ?: false,
                startOnBoot = preferences[Keys.START_ON_BOOT] ?: false,
                hideFromRecents = preferences[Keys.HIDE_FROM_RECENTS] ?: false,
            )
        }

    suspend fun setEnabled(value: Boolean) = update(Keys.ENABLED, value)

    suspend fun setStartOnBoot(value: Boolean) = update(Keys.START_ON_BOOT, value)

    suspend fun setHideFromRecents(value: Boolean) = update(Keys.HIDE_FROM_RECENTS, value)

    private suspend fun update(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { preferences -> preferences[key] = value }
    }

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        val HIDE_FROM_RECENTS = booleanPreferencesKey("hide_from_recents")
    }
}
