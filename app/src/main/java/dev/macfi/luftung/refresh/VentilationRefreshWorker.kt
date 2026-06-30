package dev.macfi.luftung.refresh

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class VentilationRefreshWorker(
    context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        return runCatching {
            RefreshCoordinator(applicationContext).refresh()
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }
}
