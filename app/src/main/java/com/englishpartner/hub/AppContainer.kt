package com.englishpartner.hub

import android.content.Context
import com.englishpartner.hub.data.LearningRepository
import com.englishpartner.hub.data.backup.BackupManager
import com.englishpartner.hub.data.db.AppDatabase
import com.englishpartner.hub.data.settings.SettingsRepository
import com.englishpartner.hub.data.storage.MediaStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Simple manual dependency injection: one instance of each service for the whole app. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: AppDatabase = AppDatabase.build(appContext)
    val storage = MediaStorage(appContext)
    val repository = LearningRepository(database, storage)
    val settings = SettingsRepository(appContext)
    val backup = BackupManager(appContext, database, storage)
    val shareInbox = ShareInbox()

    /** Lives as long as the app; used for saves that must finish after a screen closes. */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
