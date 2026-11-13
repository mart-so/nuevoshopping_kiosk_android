package com.generic.kiosk

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    private lateinit var repo: SettingsRepository
    private lateinit var pin: EditText
    private lateinit var settingsRoot: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = SettingsRepository(this)
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 24, 24, 24) }
        settingsRoot = container
        val scroll = ScrollView(this).apply { addView(container) }
        setContentView(scroll)
        pin = EditText(this).apply {
            hint = "PIN de administrador"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            transformationMethod = PasswordTransformationMethod.getInstance()
        }
        container.addView(TextView(this).apply { text = "Acceso de administrador"; textSize = 24f })
        container.addView(TextView(this).apply {
            text = "PIN predeterminado: 1234 (si no lo cambiaste)"
            textSize = 16f
            setPadding(0, 12, 0, 12)
        })
        container.addView(pin)
        container.addView(CheckBox(this).apply {
            text = "Mostrar PIN escrito"
            setOnCheckedChangeListener { _, shown ->
                pin.transformationMethod = if (shown) null else PasswordTransformationMethod.getInstance()
                pin.setSelection(pin.text.length)
            }
        })
        val enter = Button(this).apply { text = "Desbloquear" }
        container.addView(enter)
        enter.setOnClickListener {
            lifecycleScope.launch {
                val s = repo.load()
                if (AdminAuthenticator.verify(pin.text.toString(), s.adminPinHash)) showSettings(s)
                else { pin.setText(""); pin.error = "PIN incorrecto" }
            }
        }
    }

    private fun showSettings(s: KioskSettings) {
        val root = settingsRoot
        root.removeAllViews()
        root.addView(TextView(this).apply { text = "Configuración"; textSize = 24f })
        fun label(text: String) { root.addView(TextView(this).apply { this.text = text; textSize = 18f; setPadding(0, 18, 0, 4) }) }
        fun field(hint: String, value: String, numeric: Boolean = false): EditText = EditText(this).also { it.hint = hint; it.setText(value); if (numeric) it.inputType = 2; root.addView(it) }
        fun toggle(text: String, checked: Boolean) = CheckBox(this).also { it.text = text; it.isChecked = checked; root.addView(it) }
        label("General")
        val url = field("URL inicial", s.startUrl)
        val auto = toggle("Abrir al iniciar Android", s.autoStart)
        val full = toggle("Pantalla completa", s.fullscreen)
        val lock = toggle("Modo dedicado (requiere Device Owner)", s.lockTask)
        val keep = toggle("Mantener pantalla encendida", s.keepScreenOn)
        val orient = Spinner(this).apply { adapter = ArrayAdapter(this@SettingsActivity, android.R.layout.simple_spinner_dropdown_item, listOf("system", "portrait", "landscape")); setSelection(listOf("system", "portrait", "landscape").indexOf(s.orientation).coerceAtLeast(0)); root.addView(this) }
        label("Navegación y recuperación")
        val external = toggle("Permitir enlaces externos", s.allowExternalLinks)
        val reload = field("Recarga periódica en segundos (0 desactiva)", s.reloadSeconds.toString(), true)
        val timeout = field("Tiempo máximo de carga (segundos)", s.loadTimeoutSeconds.toString(), true)
        label("Administración")
        val newPin = field("Nuevo PIN (dejar vacío para conservar)", "", true)
        val save = Button(this).apply { text = "Guardar cambios"; root.addView(this) }
        val status = Button(this).apply { text = "Estado y salir del modo quiosco"; root.addView(this) }
        root.addView(TextView(this).apply { text = "Versión ${packageManager.getPackageInfo(packageName, 0).versionName}. PIN inicial: 1234. Cambialo al terminar."; setPadding(0, 16, 0, 0) })
        save.setOnClickListener {
            val candidate = UrlValidator.validate(url.text.toString())
            if (candidate == null) { url.error = "Ingresá una URL HTTP o HTTPS válida"; return@setOnClickListener }
            if (newPin.text.isNotBlank() && (newPin.text.length !in 4..12 || !newPin.text.all(Char::isDigit))) { newPin.error = "Usá entre 4 y 12 dígitos"; return@setOnClickListener }
            val updated = s.copy(startUrl = candidate, autoStart = auto.isChecked, fullscreen = full.isChecked,
                lockTask = lock.isChecked, keepScreenOn = keep.isChecked, allowExternalLinks = external.isChecked,
                allowDownloads = false, orientation = orient.selectedItem.toString(),
                reloadSeconds = reload.text.toString().toIntOrNull()?.coerceIn(0, 86400) ?: 0,
                loadTimeoutSeconds = timeout.text.toString().toIntOrNull()?.coerceIn(10, 300) ?: 45,
                adminPinHash = if (newPin.text.isNullOrBlank()) s.adminPinHash else AdminAuthenticator.hash(newPin.text.toString()))
            lifecycleScope.launch {
                runCatching { repo.save(updated) }.onSuccess { Toast.makeText(this@SettingsActivity, "Configuración guardada. Volvé a la página", Toast.LENGTH_LONG).show(); finish() }
                    .onFailure { Toast.makeText(this@SettingsActivity, it.message ?: "No se pudo guardar", Toast.LENGTH_LONG).show() }
            }
        }
        status.setOnClickListener {
            val manager = KioskManager(this)
            if (manager.isDeviceOwner) manager.stop(this@SettingsActivity)
            val network = getSystemService(android.net.ConnectivityManager::class.java).activeNetwork != null
            Toast.makeText(this, "Conectividad: ${if (network) "conectado" else "sin red"}. Device Owner: ${manager.isDeviceOwner}. Lock Task permitido: ${manager.isLockTaskPermitted}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
