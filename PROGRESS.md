# Avance de implementación

## Versión inicial en curso

Objetivo inmediato cumplido: versión mínima compilable que abre una URL configurable en WebView, recuerda la configuración, oculta barras del sistema y permite cambiar la configuración con un PIN.

## Implementado

- Proyecto Kotlin/Android Studio, `applicationId com.generic.kiosk`, Android 8+ y SDK 35.
- Carga de URL HTTP(S), cookies y DOM storage; filtros de esquemas no web.
- Botón visible de Ajustes, PIN visible si se mantiene el valor inicial y opciones persistentes con DataStore; HMAC del PIN respaldado por Android Keystore.
- URL inicial `https://nuevoshopping.com/screens/link`; app elegible como launcher Android (`HOME`/`DEFAULT`).
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

Se encontró el AVD `Medium_Phone_API_36` (Android 36, x86_64), pero no arrancó con aceleración porque el Android Emulator Hypervisor Driver no está instalado. Se instaló Android Emulator 37.2.13 y la imagen API 36 ARM64; QEMU2 informa que no admite ARM64 sobre el host x86_64. El AVD x86_64 también termina con un fallo nativo de Windows en TCG (`ANDROID_I_WANT_MY_TCG=yes`, `-accel off`, salida `-1073741819`). Se reintentó iniciar con ventana visible y el emulador actualizado; se cerró antes de aparecer en ADB. `adb devices` quedó vacío y no se instaló/abrió el APK. Para correr la demo hace falta conectar un dispositivo Android con depuración USB o usar un host x86_64 con WHPX/hipervisor disponible. El APK debug está listo en `dist/`.

## Corrección tras primera ejecución en Redmi Note 8

- 2026-11-13: La app debug se instaló manualmente en un Redmi Note 8 (Android 11 / MIUI V125), pero se cerraba al abrir.
- El logcat identificó el fallo durante la construcción de `MainActivity`: `Handler(mainLooper)` consultaba el `Context` de la Activity antes de que Android lo adjuntara.
- Se cambió a `Handler(Looper.getMainLooper())`, que no depende del contexto de la Activity. También se protegieron la reconexión de red durante la inicialización y la destrucción de la vista.
- Corregido `Handler(mainLooper)` a `Handler(Looper.getMainLooper())`; el acceso al contexto de la Activity ocurría antes de que Android lo adjuntara y causaba el cierre inmediato.
- Implementadas las solicitudes siguientes: URL predeterminada `https://nuevoshopping.com/screens/link`, botón visible **Ajustes**, PIN inicial indicado en pantalla con control mostrar/ocultar y categorías `HOME`/`DEFAULT` para permitir elegir NuevoShopping Kiosk como launcher.
- Se subió `versionCode` a 2 (`1.0.1`) para que Android la reconozca como actualización. `:app:assembleDebug` terminó correctamente; `aapt` confirmó los filtros `MAIN`/`HOME`/`DEFAULT` y `MAIN`/`LAUNCHER`, y `apksigner verify` confirmó la firma.
- `scripts/build.ps1` genera APKs con fecha además de copias de nombre estable, y elimina copias fechadas anteriores dentro de `dist` antes de crear las nuevas. El APK de hoy es `dist/generic-kiosk-browser-debug-2026-11-13.apk` (SHA-256 `cf65d71a7827f409139e9acabad77eef3b1c765ec4a0f6610408bee4d738a8b5`). Está copiado en `/sdcard/Download/` del Redmi. MIUI bloquea instalación iniciada por ADB sin SIM, por lo que falta instalar manualmente y comprobar la pantalla y launcher en el teléfono.
