package com.sillyandroid.app

import android.app.Application
import com.sillyandroid.app.data.AppDatabase
import com.sillyandroid.app.data.SeedData
import com.sillyandroid.app.domain.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SillyApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.create(this)
        container = AppContainer(database)

        // Seed default data on first launch
        appScope.launch {
            SeedData.execute(database)
        }
    }

    companion object {
        lateinit var instance: SillyApp
            private set
    }
}
