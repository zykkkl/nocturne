package com.nocturne.learn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nocturne.learn.data.AppSettings
import com.nocturne.learn.data.ConnectionResult
import com.nocturne.learn.data.ProviderConfig
import com.nocturne.learn.data.learning.LearningState
import com.nocturne.learn.data.update.UpdateState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as NocturneApp).container
    private val repository = container.settingsRepository
    private val learning = container.learningRepository

    /** null 表示 DataStore 尚未读出第一帧。 */
    val settings: StateFlow<AppSettings?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val learningState: StateFlow<LearningState> = learning.state
    val subjects: StateFlow<List<LearningState>> = learning.subjects
    val activeSubjectId: StateFlow<String?> = learning.activeSubjectId
    val updateState: StateFlow<UpdateState> = container.updateManager.state
    val updateDialogVisible: StateFlow<Boolean> = container.updateManager.dialogVisible
    val appVersion: String = container.updateManager.currentVersion

    fun completeOnboarding(goal: String, dailyMinutes: Int) {
        viewModelScope.launch {
            repository.completeOnboarding(goal, dailyMinutes)
            learning.createSubject(goal, dailyMinutes)
        }
    }

    fun updatePlan(goal: String, dailyMinutes: Int) {
        viewModelScope.launch {
            repository.updatePlan(goal, dailyMinutes)
            learning.updateActivePlan(goal, dailyMinutes)
        }
    }

    fun createSubject(goal: String, dailyMinutes: Int) {
        viewModelScope.launch {
            learning.createSubject(goal, dailyMinutes)
            repository.updatePlan(goal, dailyMinutes)
        }
    }

    fun switchSubject(subjectId: String) {
        viewModelScope.launch {
            learning.switchSubject(subjectId)
            val state = learning.state.value
            repository.updatePlan(
                goal = state.goal,
                dailyMinutes = state.dailyMinutes,
            )
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            learning.deleteSubject(subjectId)
            val state = learning.state.value
            repository.updatePlan(
                goal = state.goal,
                dailyMinutes = state.dailyMinutes,
            )
        }
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

    fun checkForUpdates(manual: Boolean = false) {
        viewModelScope.launch {
            if (manual) {
                container.updateManager.check(manual = true)
            } else {
                container.updateManager.autoCheck()
            }
        }
    }

    fun downloadUpdate() {
        viewModelScope.launch { container.updateManager.download() }
    }

    fun installUpdate() = container.updateManager.install()

    fun dismissUpdate() = container.updateManager.dismiss()
}
