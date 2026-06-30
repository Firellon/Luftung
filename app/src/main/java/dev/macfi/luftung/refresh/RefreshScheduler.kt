package dev.macfi.luftung.refresh

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object RefreshScheduler {
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<VentilationRefreshWorker>(
            repeatInterval = 2,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "ventilation-refresh",
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
