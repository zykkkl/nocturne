package com.nocturne.learn.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.nocturneDataStore by preferencesDataStore(name = "nocturne_settings")

class SettingsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    val settings: Flow<AppSettings> = context.nocturneDataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs -> prefs.toSettings() }

    suspend fun completeOnboarding(goal: String, dailyMinutes: Int) {
        context.nocturneDataStore.edit { prefs ->
            prefs[Keys.Onboarding] = true
            prefs[Keys.Goal] = goal.trim()
            prefs[Keys.DailyMinutes] = dailyMinutes
        }
    }

    suspend fun updatePlan(goal: String, dailyMinutes: Int) {
        context.nocturneDataStore.edit { prefs ->
            prefs[Keys.Goal] = goal.trim()
            prefs[Keys.DailyMinutes] = dailyMinutes
        }
    }

    suspend fun saveProvider(provider: ProviderConfig) {
        context.nocturneDataStore.edit { prefs ->
            val current = decodeProviders(prefs[Keys.Providers])
            val updated = current.filterNot { it.id == provider.id } + provider
            prefs[Keys.Providers] = json.encodeToString(updated)
            if (prefs[Keys.ActiveProvider].isNullOrBlank()) {
                prefs[Keys.ActiveProvider] = provider.id
            }
        }
    }

    suspend fun removeProvider(providerId: String) {
        context.nocturneDataStore.edit { prefs ->
            val remaining = decodeProviders(prefs[Keys.Providers]).filterNot { it.id == providerId }
            prefs[Keys.Providers] = json.encodeToString(remaining)
            if (prefs[Keys.ActiveProvider] == providerId) {
                prefs.remove(Keys.ActiveProvider)
                remaining.firstOrNull()?.let { prefs[Keys.ActiveProvider] = it.id }
            }
        }
    }

    suspend fun setActiveProvider(providerId: String) {
        context.nocturneDataStore.edit { prefs -> prefs[Keys.ActiveProvider] = providerId }
    }

    private fun Preferences.toSettings(): AppSettings = AppSettings(
        onboardingDone = this[Keys.Onboarding] ?: false,
        goal = this[Keys.Goal].orEmpty(),
        dailyMinutes = this[Keys.DailyMinutes] ?: 30,
        providers = decodeProviders(this[Keys.Providers]),
        activeProviderId = this[Keys.ActiveProvider],
    )

    private fun decodeProviders(raw: String?): List<ProviderConfig> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<ProviderConfig>>(raw) }.getOrDefault(emptyList())
    }

    private object Keys {
        val Onboarding = booleanPreferencesKey("onboarding_done")
        val Goal = stringPreferencesKey("goal")
        val DailyMinutes = intPreferencesKey("daily_minutes")
        val Providers = stringPreferencesKey("providers")
        val ActiveProvider = stringPreferencesKey("active_provider")
    }
}
