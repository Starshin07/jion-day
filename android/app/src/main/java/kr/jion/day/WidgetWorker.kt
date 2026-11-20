package kr.jion.day

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WidgetWorker(c: Context, p: WorkerParameters) : CoroutineWorker(c, p) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val ok = try { Fetch.refresh(applicationContext) } catch (e: Exception) { false }
        withContext(Dispatchers.Main) { JionWidget.refreshAll(applicationContext) }
        if (ok) Result.success() else Result.retry()
    }

    companion object {
        fun runOnce(c: Context) {
            val req = OneTimeWorkRequestBuilder<WidgetWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(c).enqueueUniqueWork("jion-widget-now", ExistingWorkPolicy.REPLACE, req)
        }
    }
}
