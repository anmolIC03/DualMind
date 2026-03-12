package com.example.dualmind

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DualMindApp : Application(), Configuration.Provider {

    // 1. Inject Hilt's custom Worker Factory
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // 2. Pass it to WorkManager's configuration
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}