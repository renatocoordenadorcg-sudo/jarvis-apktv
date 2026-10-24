package com.jarvis.tv

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast

/**
 * JARVIS TV: mostra as câmeras do go2rtc (grade, câmera em tela cheia e ronda) com a interface embutida no app (assets/tv.html).
 * A página é "servida" pelo próprio app no endereço do go2rtc (http://IP:1984/jarvis-tv.html), então fica na mesma origem
 * do servidor: sem CORS e sem configurar nada no go2rtc.
 * Controle remoto: setas navegam · OK seleciona · Voltar fecha · Menu abre as opções · Play/Pause e CH+/CH- na ronda.
 */
class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var root: FrameLayout
    private var dialogOpen = false
    private val prefs by lazy { getSharedPreferences("jarvistv", Context.MODE_PRIVATE) }
    private val bg = Color.parseColor("#02060D")
    private val page = "/jarvis-tv.html"
    private val starts = arrayOf("", "#ronda")
    private val startNames = arrayOf("Grade de câmeras", "Ronda automática")

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        root = FrameLayout(this).apply { setBackgroundColor(bg) }
        web = WebView(this).apply {
            setBackgroundColor(bg)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isFocusable = true
            isFocusableInTouchMode = true
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                setSupportZoom(false)
                builtInZoomControls = false
                allowFileAccess = false
                allowContentAccess = false
                setGeolocationEnabled(false)
                setSupportMultipleWindows(false)
            }
            webViewClient = object : WebViewClient() {
                // a página da interface vem do próprio app, mas com o endereço (origem) do go2rtc
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    if (request.url.path == page) {
                        return WebResourceResponse(
                            "text/html", "utf-8", 200, "OK",
                            mapOf("Cache-Control" to "no-store"), assets.open("tv.html")
                        )
                    }
                    return null
                }
                override fun onReceivedError(v: WebView, r: WebResourceRequest, e: WebResourceError) {
                    if (r.isForMainFrame) web.postDelayed({ web.reload() }, 8000)
                }
                override fun onRenderProcessGone(v: WebView, d: RenderProcessGoneDetail): Boolean {
                    runOnUiThread { recreate() }
                    return true
                }
            }
            webChromeClient = WebChromeClient()
        }
        root.addView(web, ViewGroup.LayoutParams(-1, -1))
        setContentView(root)
        web.requestFocus()
        val u = target()
        if (u == null) askServer(first = true) else web.loadUrl(u)
    }

    private fun target(): String? {
        val s = prefs.getString("server", null)
        if (s.isNullOrBlank()) return null
        return s + page + starts[prefs.getInt("start", 0).coerceIn(0, starts.size - 1)]
    }

    private fun askServer(first: Boolean = false) {
        if (dialogOpen) return
        dialogOpen = true
        val input = EditText(this).apply {
            hint = "192.168.3.45:1984"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine()
            setText((prefs.getString("server", "") ?: "").removePrefix("http://"))
        }
        AlertDialog.Builder(this)
            .setTitle("Endereço do go2rtc")
            .setMessage("IP e porta do go2rtc. Ex.: 192.168.3.45:1984 (se não digitar a porta, usa 1984)")
            .setView(input)
            .setCancelable(!first)
            .setPositiveButton("Conectar") { _, _ ->
                dialogOpen = false
                var s = input.text.toString().trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
                if (s.isEmpty()) { askServer(first); return@setPositiveButton }
                if (!s.contains(":")) s += ":1984"
                prefs.edit().putString("server", "http://$s").apply()
                target()?.let { web.loadUrl(it) }
            }
            .setOnCancelListener { dialogOpen = false }
            .show()
    }

    private fun showMenu() {
        if (dialogOpen) return
        dialogOpen = true
        val start = startNames[prefs.getInt("start", 0).coerceIn(0, startNames.size - 1)]
        AlertDialog.Builder(this)
            .setTitle("JARVIS TV")
            .setItems(arrayOf("Recarregar", "Alterar servidor", "Abrir ao iniciar: $start", "Sair")) { _, i ->
                dialogOpen = false
                when (i) {
                    0 -> web.reload()
                    1 -> askServer()
                    2 -> pickStart()
                    3 -> finish()
                }
            }
            .setOnCancelListener { dialogOpen = false }
            .show()
    }

    private fun pickStart() {
        if (dialogOpen) return
        dialogOpen = true
        AlertDialog.Builder(this)
            .setTitle("Abrir ao iniciar")
            .setSingleChoiceItems(startNames, prefs.getInt("start", 0)) { d, i ->
                prefs.edit().putInt("start", i).apply()
                dialogOpen = false
                d.dismiss()
                Toast.makeText(this, "Vale a partir da próxima abertura do app", Toast.LENGTH_SHORT).show()
            }
            .setOnCancelListener { dialogOpen = false }
            .show()
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (dialogOpen) return
        // a página trata o Voltar primeiro (fecha janela, ronda ou câmera ampliada); se não tratar, abre o menu
        web.evaluateJavascript("(function(){try{return window.__tvBack?window.__tvBack():false}catch(e){return false}})()") { r ->
            if (r != "true") showMenu()
        }
    }

    private fun key(k: String) {
        web.evaluateJavascript("window.__tvKey&&window.__tvKey('$k')", null)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MENU -> { showMenu(); return true }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE -> { key("pause"); return true }
            KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> { key("next"); return true }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_MEDIA_REWIND -> { key("prev"); return true }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun immersive() {
        if (Build.VERSION.SDK_INT >= 28) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) { immersive(); web.requestFocus() }
    }
    override fun onPause() { web.onPause(); super.onPause() }
    override fun onResume() { super.onResume(); web.onResume() }
    override fun onDestroy() { root.removeAllViews(); web.destroy(); super.onDestroy() }
}
