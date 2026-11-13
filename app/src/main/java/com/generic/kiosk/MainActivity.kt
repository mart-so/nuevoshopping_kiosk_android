package com.generic.kiosk

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
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
    private val handler = android.os.Handler(mainLooper)
    private val loadTimeout = Runnable { browser.stopLoading(); browser.showMessage("Carga interrumpida", "Se superó el tiempo máximo configurado. Comprobá la red e intentá nuevamente.") }
    private var taps = 0
    private var lastTap = 0L

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
        val adminGesture = View(this).apply {
            contentDescription = null
            setOnClickListener {
                val now = android.os.SystemClock.elapsedRealtime()
                taps = if (now - lastTap < 800) taps + 1 else 1
                lastTap = now
                if (taps >= 5) { taps = 0; openAdmin() }
            }
        }
        root.addView(adminGesture, FrameLayout.LayoutParams(72, 72, Gravity.TOP or Gravity.START))
        setContentView(root)
        lifecycleScope.launch {
            settings = runCatching { repo.load() }.getOrDefault(KioskSettings())
            applySettings()
            browser.reloadHome(settings.startUrl)
        }
        monitor = NetworkMonitor(this) { connected ->
            if (connected && wasOffline && ::browser.isInitialized) browser.reloadHome(settings.startUrl)
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
    override fun onDestroy() { reloadTask?.let(handler::removeCallbacks); handler.removeCallbacks(loadTimeout); browser.destroy(); super.onDestroy() }
}
