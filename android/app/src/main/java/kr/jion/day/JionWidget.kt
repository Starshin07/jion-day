package kr.jion.day

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class JionWidget : AppWidgetProvider() {

    override fun onUpdate(c: Context, mgr: AppWidgetManager, ids: IntArray) {
        schedule(c)
        WidgetWorker.runOnce(c)
        ids.forEach { render(c, mgr, it) }
    }

    override fun onEnabled(c: Context) { schedule(c) }

    override fun onDisabled(c: Context) {
        WorkManager.getInstance(c).cancelUniqueWork(WORK)
    }

    override fun onReceive(c: Context, intent: Intent) {
        super.onReceive(c, intent)
        if (intent.action == ACTION_REFRESH) {
            WidgetWorker.runOnce(c)
            refreshAll(c)
        }
    }

    companion object {
        const val ACTION_REFRESH = "kr.jion.day.REFRESH"
        private const val WORK = "jion-widget"
        private val TASK_IDS = intArrayOf(R.id.t0, R.id.t1, R.id.t2, R.id.t3, R.id.t4, R.id.t5)

        fun schedule(c: Context) {
            val req = PeriodicWorkRequestBuilder<WidgetWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(c).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, req)
        }

        fun refreshAll(c: Context) {
            val mgr = AppWidgetManager.getInstance(c)
            val ids = mgr.getAppWidgetIds(ComponentName(c, JionWidget::class.java))
            ids.forEach { render(c, mgr, it) }
        }

        fun render(c: Context, mgr: AppWidgetManager, id: Int) {
            val v = RemoteViews(c.packageName, R.layout.widget)

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), flags)
            v.setOnClickPendingIntent(R.id.root, open)
            val refresh = PendingIntent.getBroadcast(
                c, 1, Intent(c, JionWidget::class.java).setAction(ACTION_REFRESH), flags
            )
            v.setOnClickPendingIntent(R.id.refresh, refresh)

            if (!Store.hasConfig(c)) {
                v.setTextViewText(R.id.msg, "앱을 한 번 열어 주세요")
                v.setViewVisibility(R.id.msg, View.VISIBLE)
                TASK_IDS.forEach { v.setViewVisibility(it, View.GONE) }
                v.setViewVisibility(R.id.prep, View.GONE)
                v.setViewVisibility(R.id.empty, View.GONE)
                mgr.updateAppWidget(id, v)
                return
            }

            val msg = Store.widgetMsg(c)
            if (msg.isNotEmpty()) {
                v.setTextViewText(R.id.msg, "💌 " + msg)
                v.setViewVisibility(R.id.msg, View.VISIBLE)
            } else {
                v.setViewVisibility(R.id.msg, View.GONE)
            }

            val tasks = Store.widgetTasks(c)
            val sched = Store.widgetSched(c)
            val lines = ArrayList<String>()
            tasks.forEach { lines.add((if (it.second) "☑  " else "☐  ") + it.first) }
            sched.forEach { lines.add("🕘  " + (if (it.second.isNotEmpty()) it.second + "  " else "") + it.first) }

            TASK_IDS.forEachIndexed { i, rid ->
                if (i < lines.size) {
                    v.setTextViewText(rid, lines[i])
                    v.setViewVisibility(rid, View.VISIBLE)
                } else {
                    v.setViewVisibility(rid, View.GONE)
                }
            }

            val prep = Store.widgetPrep(c)
            if (prep.isNotEmpty()) {
                v.setTextViewText(R.id.prep, "🎒 " + prep)
                v.setViewVisibility(R.id.prep, View.VISIBLE)
            } else {
                v.setViewVisibility(R.id.prep, View.GONE)
            }

            val nothing = lines.isEmpty() && msg.isEmpty() && prep.isEmpty()
            v.setViewVisibility(R.id.empty, if (nothing) View.VISIBLE else View.GONE)

            mgr.updateAppWidget(id, v)
        }
    }
}
