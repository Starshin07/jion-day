package kr.jion.day

import android.content.Context
import android.content.SharedPreferences

object Store {
    private const val NAME = "jion"
    fun p(c: Context): SharedPreferences = c.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun saveConfig(c: Context, projectId: String, apiKey: String, family: String) {
        p(c).edit()
            .putString("projectId", projectId)
            .putString("apiKey", apiKey)
            .putString("family", family)
            .apply()
    }

    fun hasConfig(c: Context): Boolean =
        p(c).getString("projectId", "").isNullOrEmpty().not() &&
        p(c).getString("apiKey", "").isNullOrEmpty().not() &&
        p(c).getString("family", "").isNullOrEmpty().not()

    /** 위젯이 보여 줄 내용 */
    fun saveWidget(c: Context, msg: String, tasks: List<Pair<String, Boolean>>, sched: List<Pair<String, String>>, prep: List<String>) {
        val e = p(c).edit()
        e.putString("w_msg", msg)
        e.putInt("w_tn", tasks.size)
        tasks.forEachIndexed { i, t ->
            e.putString("w_t$i", t.first)
            e.putBoolean("w_td$i", t.second)
        }
        e.putInt("w_sn", sched.size)
        sched.forEachIndexed { i, s ->
            e.putString("w_sh$i", s.second)
            e.putString("w_st$i", s.first)
        }
        e.putString("w_prep", prep.joinToString(", "))
        e.putLong("w_at", System.currentTimeMillis())
        e.apply()
    }

    /** 새 소식 저장. 알림을 보낼지 판단하는 데 쓴다. */
    class News(
        val msgAt: Long, val praiseN: Long, val praiseLast: String,
        val wordN: Long, val wordLast: String, val msg: String
    )

    private var pendingNews: News? = null

    fun saveNews(
        c: Context, msgAt: Long, praiseN: Long, praiseLast: String,
        wordN: Long, wordLast: String, msg: String
    ) {
        pendingNews = News(msgAt, praiseN, praiseLast, wordN, wordLast, msg)
    }

    fun takeNews(): News? { val n = pendingNews; pendingNews = null; return n }

    fun seenMsgAt(c: Context): Long = p(c).getLong("seen_msgAt", -1L)
    fun seenPraise(c: Context): Long = p(c).getLong("seen_praiseN", -1L)
    fun seenWord(c: Context): Long = p(c).getLong("seen_wordN", -1L)
    fun markSeen(c: Context, msgAt: Long, praiseN: Long, wordN: Long) {
        p(c).edit().putLong("seen_msgAt", msgAt).putLong("seen_praiseN", praiseN)
            .putLong("seen_wordN", wordN).apply()
    }

    fun widgetMsg(c: Context): String = p(c).getString("w_msg", "") ?: ""
    fun widgetTasks(c: Context): List<Pair<String, Boolean>> {
        val sp = p(c); val n = sp.getInt("w_tn", 0)
        return (0 until n).map { (sp.getString("w_t$it", "") ?: "") to sp.getBoolean("w_td$it", false) }
            .filter { it.first.isNotEmpty() }
    }
    fun widgetSched(c: Context): List<Pair<String, String>> {
        val sp = p(c); val n = sp.getInt("w_sn", 0)
        return (0 until n).map { (sp.getString("w_st$it", "") ?: "") to (sp.getString("w_sh$it", "") ?: "") }
            .filter { it.first.isNotEmpty() }
    }
    fun widgetPrep(c: Context): String = p(c).getString("w_prep", "") ?: ""
    fun widgetAt(c: Context): Long = p(c).getLong("w_at", 0L)
}
