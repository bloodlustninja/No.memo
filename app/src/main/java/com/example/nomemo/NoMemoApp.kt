package com.example.nomemo

import android.app.Application
import androidx.appfunctions.service.AppFunctionConfiguration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.nomemo.appfunctions.NoMemoAppFunctions
import com.example.nomemo.data.db.AppDatabase
import com.example.nomemo.glyph.BurnTimerWorker
import java.util.concurrent.TimeUnit

/**
 * Custom Application class for No Memo.
 *
 * Implements AppFunctionConfiguration.Provider to expose local Gemini on-device
 * capabilities via Android Jetpack AppFunctions, initializes AppDatabase,
 * and registers the periodic BurnTimerWorker.
 */
class NoMemoApp : Application(), AppFunctionConfiguration.Provider {

    override fun onCreate() {
        super.onCreate()
        AppDatabase.getDatabase(this)
        scheduleBurnTimerWorker()
    }

    override val appFunctionConfiguration: AppFunctionConfiguration
        get() = AppFunctionConfiguration.Builder()
            .addEnclosingClassFactory(NoMemoAppFunctions::class.java) {
                NoMemoAppFunctions(this)
            }
            .build()

    private fun scheduleBurnTimerWorker() {
        val workRequest = PeriodicWorkRequestBuilder<BurnTimerWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            BurnTimerWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest,
        )
    }
}
