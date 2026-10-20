package io.github.giamat13.turnfall.data

import android.content.Context
import kotlinx.serialization.json.Json

/**
 * Progress and settings in this phone's private storage. The keys and the JSON shape are the page's, so a backup of
 * either one reads the same way. A broken entry is ignored and the defaults are used, as the page does.
 */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("turnfall", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun loadProgress(): Progress = read(PROGRESS_KEY) ?: Progress()

    fun saveProgress(progress: Progress) = write(PROGRESS_KEY, json.encodeToString(Progress.serializer(), progress))

    fun loadSettings(): Settings = read(SETTINGS_KEY) ?: Settings()

    fun saveSettings(settings: Settings) = write(SETTINGS_KEY, json.encodeToString(Settings.serializer(), settings))

    private inline fun <reified T> read(key: String): T? {
        val text = prefs.getString(key, null) ?: return null
        return try {
            json.decodeFromString<T>(text)
        } catch (e: Exception) {
            null
        }
    }

    private fun write(key: String, text: String) {
        prefs.edit().putString(key, text).apply()
    }

    companion object {
        const val PROGRESS_KEY = "world-best-v2"
        const val SETTINGS_KEY = "world-settings-v2"
    }
}
