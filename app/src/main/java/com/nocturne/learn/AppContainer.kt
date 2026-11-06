package com.nocturne.learn

import android.content.Context
import com.nocturne.learn.ai.AskEngine
import com.nocturne.learn.ai.ModelClient
import com.nocturne.learn.ai.TextbookEngine
import com.nocturne.learn.ai.TutorEngine
import com.nocturne.learn.data.ConnectionTester
import com.nocturne.learn.data.SecretStore
import com.nocturne.learn.data.SettingsRepository
import com.nocturne.learn.data.ask.AskHistoryRepository
import com.nocturne.learn.data.learning.LearningRepository
import com.nocturne.learn.data.search.WebSearchClient
import com.nocturne.learn.data.textbook.TextbookParser
import com.nocturne.learn.data.update.UpdateManager

class AppContainer(context: Context) {
    val settingsRepository = SettingsRepository(context)
    val secretStore = SecretStore(context)
    val connectionTester = ConnectionTester()
    val learningRepository = LearningRepository(context)
    val askHistoryRepository = AskHistoryRepository(context)
    val modelClient = ModelClient()
    val webSearchClient = WebSearchClient()
    val tutorEngine = TutorEngine(modelClient, learningRepository, webSearchClient)
    val askEngine = AskEngine(modelClient, webSearchClient)
    val textbookParser = TextbookParser(context)
    val textbookEngine = TextbookEngine(modelClient)
    val updateManager = UpdateManager(context)
}
