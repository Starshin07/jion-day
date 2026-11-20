package kr.jion.day

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var web: WebView
    private var offline: View? = null
    private var pendingChooser: ValueCallback<Array<Uri>>? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        setContentView(root)

        web = WebView(this)
        root.addView(web, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT))

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            useWideViewPort = true
            loadWithOverviewMode = true
            allowFileAccess = true
            setSupportZoom(false)
        }
        WebView.setWebContentsDebuggingEnabled(false)

        web.addJavascriptInterface(Bridge(), "AndroidBridge")

        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView?, req: WebResourceRequest?): Boolean {
                val u = req?.url ?: return false
                val host = u.host ?: return false
                return if (host.contains(APP_HOST)) {
                    false
                } else {
                    try { startActivity(Intent(Intent.ACTION_VIEW, u)) } catch (e: Exception) {}
                    true
                }
            }
            override fun onReceivedError(v: WebView?, req: WebResourceRequest?, err: WebResourceError?) {
                if (req?.isForMainFrame == true) showOffline(root)
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                runOnUiThread {
                    try { request?.grant(request.resources) } catch (e: Exception) { request?.deny() }
                }
            }
            override fun onShowFileChooser(
                v: WebView?, cb: ValueCallback<Array<Uri>>?, params: FileChooserParams?
            ): Boolean {
                pendingChooser?.onReceiveValue(null)
                pendingChooser = cb
                return try {
                    val intent = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE)
                    }
                    startActivityForResult(intent, REQ_FILE)
                    true
                } catch (e: Exception) {
                    pendingChooser = null
                    cb?.onReceiveValue(null)
                    false
                }
            }
        }

        if (savedInstanceState != null) web.restoreState(savedInstanceState) else web.loadUrl(APP_URL)

        JionWidget.schedule(this)
    }

    private fun showOffline(root: LinearLayout) {
        if (offline != null) return
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(48, 64, 48, 48)
        val t = TextView(this)
        t.text = "인터넷에 연결되지 않았어요.\n연결한 뒤 다시 눌러 주세요."
        t.textSize = 18f
        box.addView(t)
        val b = Button(this)
        b.text = "다시 열기"
        b.setOnClickListener {
            root.removeView(box); offline = null; web.visibility = View.VISIBLE; web.loadUrl(APP_URL)
        }
        box.addView(b)
        web.visibility = View.GONE
        root.addView(box)
        offline = box
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_FILE) return
        val cb = pendingChooser ?: return
        pendingChooser = null
        val uris: Array<Uri>? = if (resultCode == RESULT_OK && data != null) {
            val clip = data.clipData
            when {
                clip != null -> Array(clip.itemCount) { clip.getItemAt(it).uri }
                data.data != null -> arrayOf(data.data!!)
                else -> null
            }
        } else null
        cb.onReceiveValue(uris)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onPause() {
        super.onPause()
        WidgetWorker.runOnce(this)
    }

    inner class Bridge {
        @JavascriptInterface
        fun saveConfig(projectId: String, apiKey: String, family: String) {
            Store.saveConfig(applicationContext, projectId, apiKey, family)
            WidgetWorker.runOnce(applicationContext)
        }

        @JavascriptInterface
        fun refreshWidget() {
            WidgetWorker.runOnce(applicationContext)
        }

        @JavascriptInterface
        fun isAndroidApp(): Boolean = true
    }

    companion object {
        // 배포 주소 (GitHub Pages)
        const val APP_URL = "https://REPLACE_OWNER.github.io/REPLACE_REPO/"
        const val APP_HOST = "github.io"
        private const val REQ_FILE = 7001
    }
}
