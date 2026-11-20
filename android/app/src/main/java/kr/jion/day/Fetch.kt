package kr.jion.day

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 파이어스토어에서 위젯용 문서 하나만 읽어 온다.
 * families/{fam}/app/widget 는 누구나 읽을 수 있게 규칙을 열어 둔다(쓰기는 로그인 필요).
 */
object Fetch {

    fun refresh(c: Context): Boolean {
        if (!Store.hasConfig(c)) return false
        val pid = Store.p(c).getString("projectId", "") ?: return false
        val key = Store.p(c).getString("apiKey", "") ?: return false
        val fam = Store.p(c).getString("family", "") ?: return false
        val url = "https://firestore.googleapis.com/v1/projects/$pid/databases/(default)/documents/" +
                "families/$fam/app/widget?key=$key"
        val body = get(url) ?: return false
        return try {
            parseInto(c, JSONObject(body))
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun get(url: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12000
                readTimeout = 12000
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        } finally {
            try { conn?.disconnect() } catch (e: Exception) {}
        }
    }

    private fun parseInto(c: Context, root: JSONObject) {
        val f = root.optJSONObject("fields") ?: return
        val msg = f.optJSONObject("msg")?.optString("stringValue", "") ?: ""

        val tasks = ArrayList<Pair<String, Boolean>>()
        f.optJSONObject("tasks")?.optJSONObject("arrayValue")?.optJSONArray("values")?.let { arr ->
            for (i in 0 until arr.length()) {
                val mv = arr.optJSONObject(i)?.optJSONObject("mapValue")?.optJSONObject("fields") ?: continue
                val title = mv.optJSONObject("t")?.optString("stringValue", "") ?: ""
                val doneRaw = mv.optJSONObject("d")
                val done = when {
                    doneRaw == null -> false
                    doneRaw.has("integerValue") -> doneRaw.optString("integerValue", "0") != "0"
                    doneRaw.has("doubleValue") -> doneRaw.optDouble("doubleValue", 0.0) != 0.0
                    doneRaw.has("booleanValue") -> doneRaw.optBoolean("booleanValue", false)
                    else -> false
                }
                if (title.isNotEmpty()) tasks.add(title to done)
            }
        }

        val sched = ArrayList<Pair<String, String>>()
        f.optJSONObject("sched")?.optJSONObject("arrayValue")?.optJSONArray("values")?.let { arr ->
            for (i in 0 until arr.length()) {
                val mv = arr.optJSONObject(i)?.optJSONObject("mapValue")?.optJSONObject("fields") ?: continue
                val t = mv.optJSONObject("t")?.optString("stringValue", "") ?: ""
                val h = mv.optJSONObject("h")?.optString("stringValue", "") ?: ""
                if (t.isNotEmpty()) sched.add(t to h)
            }
        }

        val prep = ArrayList<String>()
        f.optJSONObject("prep")?.optJSONObject("arrayValue")?.optJSONArray("values")?.let { arr ->
            for (i in 0 until arr.length()) {
                val v = arr.optJSONObject(i)?.optString("stringValue", "") ?: ""
                if (v.isNotEmpty()) prep.add(v)
            }
        }

        Store.saveWidget(c, msg, tasks, sched, prep)
    }
}
