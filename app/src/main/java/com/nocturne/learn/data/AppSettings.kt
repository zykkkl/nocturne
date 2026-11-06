package com.nocturne.learn.data

import kotlinx.serialization.Serializable

@Serializable
data class ProviderConfig(
    val id: String,
    val templateId: String,
    val displayName: String,
    val baseUrl: String,
    val model: String,
    val streaming: Boolean = true,
    val toolCalling: Boolean = true,
)

@Serializable
data class AppSettings(
    val onboardingDone: Boolean = false,
    val goal: String = "",
    val dailyMinutes: Int = 30,
    val providers: List<ProviderConfig> = emptyList(),
    val activeProviderId: String? = null,
    val searchEnabled: Boolean = false,
    val searchProviderId: String = "bocha",
) {
    val activeProvider: ProviderConfig?
        get() = providers.firstOrNull { it.id == activeProviderId } ?: providers.firstOrNull()
}
