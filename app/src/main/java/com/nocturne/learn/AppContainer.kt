package com.nocturne.learn

import android.content.Context
import com.nocturne.learn.ai.AskEngine
import com.nocturne.learn.ai.ModelClient
import com.nocturne.learn.ai.TutorEngine
import com.nocturne.learn.data.ConnectionTester
import com.nocturne.learn.data.SecretStore
import com.nocturne.learn.data.SettingsRepository
import com.nocturne.learn.data.learning.LearningRepository
import com.nocturne.learn.data.update.UpdateManager

class AppContainer(context: Context) {
    val settingsRepository = SettingsRepository(context)
    val secretStore = SecretStore(context)
    val connectionTester = ConnectionTester()
    val learningRepository = LearningRepository(context)
    val modelClient = ModelClient()
    val tutorEngine = TutorEngine(modelClient, learningRepository)
    val askEngine = AskEngine(modelClient)
    val updateManager = UpdateManager(context)
}
