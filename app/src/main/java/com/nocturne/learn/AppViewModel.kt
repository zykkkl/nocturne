package com.nocturne.learn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nocturne.learn.data.AppSettings
import com.nocturne.learn.data.ConnectionResult
import com.nocturne.learn.data.ProviderConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as NocturneApp).container
    private val repository = container.settingsRepository

    /** null 表示 DataStore 尚未读出第一帧。 */
    val settings: StateFlow<AppSettings?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun completeOnboarding(goal: String, dailyMinutes: Int) {
        viewModelScope.launch { repository.completeOnboarding(goal, dailyMinutes) }
    }

    fun updatePlan(goal: String, dailyMinutes: Int) {
        viewModelScope.launch { repository.updatePlan(goal, dailyMinutes) }
    }

    fun saveProvider(provider: ProviderConfig, apiKey: String?, makeActive: Boolean = false) {
        viewModelScope.launch {
            repository.saveProvider(provider)
            apiKey?.takeIf { it.isNotBlank() }?.let { container.secretStore.put(provider.id, it) }
            if (makeActive || settings.value?.activeProviderId.isNullOrBlank()) {
                repository.setActiveProvider(provider.id)
            }
        }
    }

    fun removeProvider(providerId: String) {
        viewModelScope.launch {
            container.secretStore.remove(providerId)
            repository.removeProvider(providerId)
        }
    }

    fun setActiveProvider(providerId: String) {
        viewModelScope.launch { repository.setActiveProvider(providerId) }
    }

    fun savedKey(providerId: String): String? = container.secretStore.get(providerId)

    fun hasKey(providerId: String): Boolean = container.secretStore.has(providerId)

    suspend fun testConnection(baseUrl: String, apiKey: String): ConnectionResult =
        container.connectionTester.test(baseUrl, apiKey)
}
