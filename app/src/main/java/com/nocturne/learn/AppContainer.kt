package com.nocturne.learn

import android.content.Context
import com.nocturne.learn.data.ConnectionTester
import com.nocturne.learn.data.SecretStore
import com.nocturne.learn.data.SettingsRepository

class AppContainer(context: Context) {
    val settingsRepository = SettingsRepository(context)
    val secretStore = SecretStore(context)
    val connectionTester = ConnectionTester()
}
