package com.englishpartner.hub

import android.app.Application
import kotlinx.coroutines.launch

class EnglishPartnerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.appScope.launch { container.repository.ensureSeeded() }
    }
}
