# Avance de implementación

## Versión inicial en curso

Objetivo inmediato cumplido: versión mínima compilable que abre una URL configurable en WebView, recuerda la configuración, oculta barras del sistema y permite cambiar la configuración con un PIN.

## Implementado

- Proyecto Kotlin/Android Studio, `applicationId com.generic.kiosk`, Android 8+ y SDK 35.
- Carga de URL HTTP(S), cookies y DOM storage; filtros de esquemas no web.
- Gesto de cinco toques en esquina superior izquierda, panel PIN y opciones persistentes con DataStore; HMAC del PIN respaldado por Android Keystore.
- Fullscreen inmersivo, orientación y mantener pantalla encendida.
- Manejo básico de error de red/HTTP, reintento al recuperar conectividad.
- Receptor BOOT_COMPLETED y Lock Task/Device Owner condicionados a autorización Android.
- Scripts PowerShell y guía inicial de instalación, provisión y recuperación.

## Pendiente de esta primera entrega

- Instalar y probar visualmente en un dispositivo Android; no había emulador/dispositivo conectado (`adb devices` vacío).
- Ejecutar la prueba instrumental de PIN/Keystore y arranque en dispositivo.
- Probar Device Owner y Lock Task en un dispositivo restablecido.
- Validar fabricantes y configuraciones Android físicas.
- Agregar recuperación cuando el proceso WebView renderer muere.
- Implementar descargas seguras y comportamiento configurable del botón Atrás.
- Completar manejo de errores, accesibilidad y pruebas de persistencia/conectividad en dispositivo.
- Implementar o retirar controles que todavía no están operativos (descargas, timeout y comportamiento configurable de Atrás).
- Prueba manual en hardware Android y comprobar aprovisionamiento Device Owner: no se puede afirmar hasta ejecutarla.

Verificado en esta máquina: `testDebugUnitTest`, `assembleDebug`, `assembleRelease` y `assembleDebugAndroidTest` terminaron con éxito. El APK debug pasó `apksigner verify`; release está generado sin firma por diseño. No hay Android conectado, por lo que no se afirma ejecución en dispositivo. El primer intento unitario usaba Robolectric y falló porque faltaba su runtime local; se eliminó esa dependencia para que las validaciones JVM no dependan de ella. La prueba instrumental de AndroidKeyStore compila, pero está pendiente ejecutarla en un dispositivo/emulador.
