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

- Ejecutar una prueba visual en dispositivo Android.
- Ejecutar la prueba instrumental de PIN/Keystore y arranque en dispositivo.
- Probar Device Owner y Lock Task en un dispositivo restablecido.
- Validar fabricantes y configuraciones Android físicas.
- Agregar recuperación cuando el proceso WebView renderer muere.
- Implementar descargas seguras y comportamiento configurable del botón Atrás.
- Completar manejo de errores, accesibilidad y pruebas de persistencia/conectividad en dispositivo.

Verificado en esta máquina: `testDebugUnitTest`, `assembleDebug`, `assembleRelease` y `assembleDebugAndroidTest` terminaron con éxito. El APK debug pasó `apksigner verify`; release está generado sin firma por diseño. La prueba instrumental de AndroidKeyStore compila, pero está pendiente ejecutarla en un dispositivo/emulador.

## Intento de demo en emulador

Se encontró el AVD `Medium_Phone_API_36` (Android 36, x86_64), pero no arrancó con aceleración porque el Android Emulator Hypervisor Driver no está instalado. Se instaló Android Emulator 37.2.13 y la imagen API 36 ARM64; QEMU2 informa que no admite ARM64 sobre el host x86_64. El AVD x86_64 también termina con un fallo nativo de Windows en TCG (`ANDROID_I_WANT_MY_TCG=yes`, `-accel off`, salida `-1073741819`). `adb devices` quedó vacío y no se instaló/abrió el APK. Para correr la demo hace falta conectar un dispositivo Android con depuración USB o usar un host x86_64 con WHPX/hipervisor disponible. El APK debug está listo en `dist/`.
