# Contexto de `generic-kiosk-browser-debug.apk`

Este archivo describe únicamente el APK `dist/generic-kiosk-browser-debug.apk`. Su propósito es dar a otro agente información suficiente para identificarlo, instalarlo, usarlo y entender sus límites sin el historial de esta conversación.

## Identidad del APK

- Ruta: `dist/generic-kiosk-browser-debug.apk`
- Nombre de paquete: `com.generic.kiosk.debug`
- Versión: `1.0.1-debug` (`versionCode` 2)
- Android mínimo: 8.0 / API 26
- Android objetivo: API 35; compilado con SDK 35
- Tamaño: 3,216,895 bytes
- SHA-256: `cf65d71a7827f409139e9acabad77eef3b1c765ec4a0f6610408bee4d738a8b5`
- Firma: firmado automáticamente por Gradle con la clave de desarrollo local. Se puede instalar para pruebas sin firmarlo de nuevo. Para actualizar una instalación existente, el certificado debe coincidir.

Para regenerarlo desde la raíz del repo:

```powershell
.\gradlew.bat :app:assembleDebug
```

Gradle también coloca el resultado en `app/build/outputs/apk/debug/app-debug.apk`.

## Propósito y uso inicial

**NuevoShopping Kiosk** es una app Android nativa en Kotlin que abre una página web dentro de Android WebView. Está pensada para pantallas de información, tablets de atención y navegación dedicada. No requiere cuenta ni servicios cloud; no tiene publicidad ni telemetría.

Instalación y apertura:

```powershell
adb install -r dist\generic-kiosk-browser-debug.apk
adb shell monkey -p com.generic.kiosk.debug 1
```

Al iniciar por primera vez abre `https://nuevoshopping.com/screens/link`. Para configurar otra página:

1. Tocar el botón visible **Ajustes**.
2. Ingresar el PIN inicial `1234` (se indica en la pantalla si no se cambió).
3. Cambiar el PIN y configurar la URL HTTP/HTTPS.
4. Guardar y volver a la pantalla principal.

El PIN inicial debe cambiarse antes de un uso real. Los PIN admitidos son de 4 a 12 dígitos. La verificación usa HMAC-SHA256 con una clave de Android Keystore; el PIN en texto claro no se guarda. Las preferencias se guardan localmente con Preferences DataStore.

La aplicación se ejecutó previamente en un Redmi Note 8 con Android 11 y MIUI V125. Se corrigió un cierre al arrancar causado por acceder al `mainLooper` del contexto de la Activity durante su construcción. La versión 1.0.1 agrega la URL solicitada, acceso visible a Ajustes y elegibilidad como launcher Android. MIUI bloquea `adb install` sin SIM; la actualización fechada `generic-kiosk-browser-debug-2026-11-13.apk` está en Descargas del teléfono para instalarla manualmente.

## Funciones incluidas

### Navegador WebView

- JavaScript, cookies y almacenamiento DOM habilitados para sitios y aplicaciones web modernas.
- Acepta direcciones HTTP y HTTPS válidas; bloquea esquemas como `file:`, `javascript:` e `intent:`.
- Desactiva acceso a archivos/contenido local, ventanas múltiples y contenido mixto.
- Por defecto permite navegar dentro del mismo host inicial. El ajuste “Permitir enlaces externos” permite otros hosts HTTP(S) en el mismo WebView.
- Presenta mensajes propios ante errores de red y HTTP; al detectar reconexión vuelve a cargar la URL inicial.
- Se puede configurar el timeout de carga y la recarga periódica.
- Las descargas están bloqueadas y la app no instala archivos APK.

### Pantalla y operación quiosco

- Modo inmersivo para ocultar las barras del sistema y mantener la pantalla encendida.
- Orientación del sistema, vertical u horizontal.
- “Abrir al iniciar Android” intenta lanzar la app al recibir `BOOT_COMPLETED`. Android y algunos fabricantes pueden impedir que una app inicie una Activity desde segundo plano; no garantiza autoinicio en todos los dispositivos.
- “Modo dedicado” llama a Lock Task únicamente cuando Android reconoce el paquete como Device Owner. La app no obtiene ese privilegio por sí sola.
- Fullscreen en un dispositivo convencional solo oculta controles del sistema: no impide que alguien salga de la app. Encendido tras un corte de energía depende del firmware.

### Panel administrativo

Se accede mediante el botón visible **Ajustes** y el PIN. El panel permite cambiar URL inicial, autoinicio, pantalla completa, modo dedicado, pantalla encendida, orientación, enlaces a otros hosts, intervalo de recarga, timeout y PIN. La entrada del PIN tiene una opción para mostrar u ocultar los dígitos.

La actividad principal declara las categorías `HOME` y `DEFAULT`, además de `LAUNCHER`. Android y el launcher del fabricante pueden ofrecer **NuevoShopping Kiosk** como aplicación de inicio predeterminada.

El botón “Estado y salir del modo quiosco” informa conectividad, Device Owner y permiso Lock Task. Puede solicitar salida de Lock Task si el paquete es Device Owner. El botón Atrás vuelve por el historial web y luego usa el comportamiento normal de Android; aún no es configurable.

## Límites importantes

- HTTP se acepta por compatibilidad, pero transmite datos sin cifrado; se recomienda HTTPS.
- No hay recuperación explícita si Android cierra el proceso renderer del WebView.
- El timeout detiene la carga que excede el límite; no garantiza que un servidor inaccesible vuelva a funcionar.
- En MIUI/Xiaomi, Samsung y otros fabricantes, autoinicio y batería pueden requerir permisos en los ajustes del sistema.
- Device Owner normalmente requiere un dispositivo nuevo/restablecido o un flujo MDM. Para este APK, el componente es `com.generic.kiosk.debug/com.generic.kiosk.AdminReceiver`. No ejecutar `dpm set-device-owner` en un equipo con datos que se quieran conservar; seguir `README.md` para aprovisionamiento y recuperación.

## Código correspondiente

- `app/src/main/java/com/generic/kiosk/MainActivity.kt`: apertura de URL, fullscreen, gesto administrativo, red y recarga.
- `app/src/main/java/com/generic/kiosk/KioskWebView.kt`: configuración y navegación WebView.
- `app/src/main/java/com/generic/kiosk/SettingsActivity.kt`: panel administrativo.
- `app/src/main/java/com/generic/kiosk/SettingsRepository.kt`: DataStore y autenticación PIN/Keystore.
- `app/src/main/java/com/generic/kiosk/KioskManager.kt`: Device Owner y Lock Task.
- `app/src/main/java/com/generic/kiosk/BootReceiver.kt` y `NetworkMonitor.kt`: arranque y conectividad.
- `app/src/main/AndroidManifest.xml`: permisos y componentes Android.

Permisos declarados: `INTERNET`, `ACCESS_NETWORK_STATE` y `RECEIVE_BOOT_COMPLETED`.

## Verificación realizada

El APK compila y su firma pasó `apksigner verify`. Las pruebas JVM pasaron; la prueba instrumental de Android Keystore no se ha ejecutado en un dispositivo. Aún no se ha instalado ni validado en hardware real. El AVD x86_64 se volvió a lanzar con ventana visible, emulador 37.2.13 y TCG (`ANDROID_I_WANT_MY_TCG=yes`, `-accel off`), pero se cerró antes de aparecer en ADB. No afirmar que la app se haya instalado o ejecutado en emulador.
