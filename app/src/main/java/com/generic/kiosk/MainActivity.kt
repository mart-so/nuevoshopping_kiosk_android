package com.generic.kiosk

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.util.TypedValue
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var repo: SettingsRepository
    private lateinit var browser: KioskWebView
    private lateinit var monitor: NetworkMonitor
    private lateinit var settings: KioskSettings
    private var wasOffline = false
    private var reloadTask: Runnable? = null
    // Activity context is not attached while Activity fields are constructed.
    private val handler = Handler(Looper.getMainLooper())
    private val loadTimeout = Runnable { browser.stopLoading(); browser.showMessage("Carga interrumpida", "Se superó el tiempo máximo configurado. Comprobá la red e intentá nuevamente.") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = SettingsRepository(this)
        browser = KioskWebView(this) { settings.allowExternalLinks }
        val root = FrameLayout(this)
        root.addView(browser)
        val progress = ProgressBar(this).apply { visibility = View.GONE; isIndeterminate = true }
        root.addView(progress, FrameLayout.LayoutParams(64, 64, Gravity.CENTER))
        browser.progress = { loading ->
            progress.visibility = if (loading) View.VISIBLE else View.GONE
            handler.removeCallbacks(loadTimeout)
            if (loading && ::settings.isInitialized) handler.postDelayed(loadTimeout, settings.loadTimeoutSeconds * 1000L)
        }
        val settingsButton = TextView(this).apply {
            text = "Ajustes"
            contentDescription = "Abrir configuración"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = GradientDrawable().apply {
                setColor(0xD9212121.toInt())
                cornerRadius = dp(10).toFloat()
            }
            elevation = dp(6).toFloat()
            isClickable = true
            isFocusable = true
            setOnClickListener { openAdmin() }
        }
        root.addView(settingsButton, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.END
        ).apply { setMargins(0, dp(16), dp(16), 0) })
        setContentView(root)
        lifecycleScope.launch {
            settings = runCatching { repo.load() }.getOrDefault(KioskSettings())
            applySettings()
            browser.reloadHome(settings.startUrl)
        }
        monitor = NetworkMonitor(this) { connected ->
            if (connected && wasOffline && ::browser.isInitialized && ::settings.isInitialized) browser.reloadHome(settings.startUrl)
            wasOffline = !connected
        }
    }

    private fun applySettings() {
        if (settings.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = when (settings.orientation) { "portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT; "landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE; else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        if (settings.fullscreen) hideSystemUi()
        KioskManager(this).configureAndStart(this, settings.lockTask)
        scheduleReload()
    }

    private fun hideSystemUi() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    private fun scheduleReload() {
        reloadTask?.let(handler::removeCallbacks)
        if (settings.reloadSeconds > 0) {
            reloadTask = Runnable { browser.reloadHome(settings.startUrl); scheduleReload() }
            handler.postDelayed(reloadTask!!, settings.reloadSeconds * 1000L)
        }
    }

    private fun openAdmin() {
        startActivity(android.content.Intent(this, SettingsActivity::class.java))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    override fun onResume() {
        super.onResume()
        if (::settings.isInitialized) {
            if (settings.fullscreen) hideSystemUi()
            lifecycleScope.launch {
                val updated = runCatching { repo.load() }.getOrNull() ?: return@launch
                val changed = updated != settings
                settings = updated
                applySettings()
                if (changed) browser.reloadHome(settings.startUrl)
            }
        }
        if (::monitor.isInitialized) monitor.start()
    }
    override fun onPause() { handler.removeCallbacks(loadTimeout); if (::monitor.isInitialized) monitor.stop(); super.onPause() }
    @Deprecated("Deprecated in Android")
    override fun onBackPressed() { if (browser.canGoBack()) browser.goBack() else super.onBackPressed() }
    override fun onWindowFocusChanged(hasFocus: Boolean) { super.onWindowFocusChanged(hasFocus); if (hasFocus && ::settings.isInitialized && settings.fullscreen) hideSystemUi() }
    override fun onDestroy() {
        reloadTask?.let(handler::removeCallbacks)
        handler.removeCallbacks(loadTimeout)
        if (::browser.isInitialized) browser.destroy()
        super.onDestroy()
    }
}
