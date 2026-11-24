package kr.jion.day

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notify {

    private const val CH = "jion-news"

    private fun ensureChannel(c: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = c.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(CH) != null) return
        val ch = NotificationChannel(CH, "지온이 소식", NotificationManager.IMPORTANCE_DEFAULT)
        ch.description = "엄마·아빠가 보낸 메시지, 칭찬 도장, 단어 뜻 알림"
        mgr.createNotificationChannel(ch)
    }

    private fun show(c: Context, id: Int, title: String, text: String) {
        ensureChannel(c)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val open = PendingIntent.getActivity(c, id, Intent(c, MainActivity::class.java), flags)
        val n: Notification = NotificationCompat.Builder(c, CH)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try {
            NotificationManagerCompat.from(c).notify(id, n)
        } catch (e: SecurityException) {
            // 알림 권한이 없으면 조용히 넘어간다
        }
    }

    /** 새 소식이 있으면 알림을 띄운다. 처음 한 번은 기준만 잡고 알리지 않는다. */
    fun checkAndNotify(c: Context) {
        val n = Store.takeNews() ?: return
        val firstRun = Store.seenMsgAt(c) < 0L

        if (!firstRun) {
            if (n.msgAt > Store.seenMsgAt(c) && n.msg.isNotEmpty()) {
                show(c, 1001, "엄마·아빠가 메시지를 보냈어요", n.msg)
            }
            if (n.praiseN > Store.seenPraise(c)) {
                val body = if (n.praiseLast.isNotEmpty()) n.praiseLast else "오늘도 잘했어요!"
                show(c, 1002, "칭찬 도장을 받았어요", body)
            }
            if (n.wordN > Store.seenWord(c)) {
                val body = if (n.wordLast.isNotEmpty())
                    n.wordLast + " 의 뜻이 도착했어요" else "단어 뜻이 도착했어요"
                show(c, 1003, "사전에 새 뜻이 왔어요", body)
            }
        }
        Store.markSeen(c, n.msgAt, n.praiseN, n.wordN)
    }
}
