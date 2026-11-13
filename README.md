# Generic Kiosk Browser

Aplicación Android nativa en Kotlin para mostrar un sitio web a pantalla completa en equipos Android 8.0 o posterior. El modo de pantalla completa sirve en un dispositivo convencional. El bloqueo dedicado de Android solo se activa después de provisionar el equipo como **Device Owner**.

## Estado de las funciones

- WebView con JavaScript, cookies y almacenamiento DOM; solo admite navegación HTTP/HTTPS y bloquea `file:`, `javascript:`, `intent:` y otros esquemas.
- URL inicial predeterminada: `https://nuevoshopping.com/screens/link`. Las opciones se guardan con Preferences DataStore.
- El botón visible **Ajustes** abre la administración. El PIN inicial `1234` se muestra en la pantalla de acceso si todavía no se cambió; también se puede mostrar/ocultar el PIN que se escribe.
- MainActivity está declarada como actividad `HOME`, por lo que Android puede ofrecer **NuevoShopping Kiosk** para elegirla como launcher predeterminado, además de mostrarla en el lanzador normal.
- Pantalla inmersiva, orientación, pantalla encendida, carga al arranque y recuperación de conectividad.
- Lock Task con lista permitida solo si Android informa que esta app es Device Owner.
- No hay telemetría, anuncios ni servicios externos.

La aplicación no instala APK por sí sola y las descargas quedan bloqueadas en esta versión. El timeout detiene una carga que supera el límite configurado; se maneja la reconexión de red. Aún falta recuperación explícita si el proceso renderer de WebView se cierra, y configurar el comportamiento del botón Atrás. El arranque desde segundo plano depende de fabricante/versión y puede bloquearse. No se ha validado en dispositivos físicos.

## Entorno y compilación

Requisitos: Android Studio (JBR 17 o 21), Android SDK Platform 35, Build Tools 35, Gradle Wrapper incluido. `minSdk 26`; `targetSdk 35`.

En Android Studio: abrir la carpeta del repo, esperar la sincronización de Gradle y elegir **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

En Windows PowerShell:

```powershell
.\scripts\build.ps1
```

Cada `assembleDebug` o `assembleRelease` —también desde Android Studio— copia el APK a `dist` con fecha, por ejemplo `generic-kiosk-browser-debug-2026-11-13.apk`, y reemplaza las copias fechadas anteriores de esa variante. También mantiene nombres estables para las guías e instalaciones. El script `scripts\build.ps1` compila ambas variantes y limpia además las copias fechadas anteriores de las dos. Gradle conserva sus salidas normales dentro de `app\build\outputs\apk\`.

Las pruebas unitarias se ejecutan con `.\gradlew.bat testDebugUnitTest`; la prueba instrumental requiere un emulador/dispositivo y `.\gradlew.bat connectedDebugAndroidTest`.

## Instalar y operar por ADB

```powershell
.\scripts\install.ps1
.\scripts\status.ps1
```

O manualmente: `adb install -r dist\generic-kiosk-browser-debug.apk`, luego abrir **NuevoShopping Kiosk**. En Android, se puede elegir en Ajustes del sistema como aplicación de inicio predeterminada.

## Provisionamiento Dedicated Device / Device Owner

Device Owner normalmente solo se asigna en un equipo nuevo o restablecido de fábrica, sin cuentas/perfiles ya configurados. Este comando requiere confirmar que aceptás borrar el equipo; no lo ejecutes sobre un dispositivo con datos que quieras conservar:

```powershell
adb shell dpm set-device-owner com.generic.kiosk/.AdminReceiver
```

Para el APK debug, el ID instalado incluye `.debug`; en ese caso usá `adb shell dpm set-device-owner com.generic.kiosk.debug/com.generic.kiosk.AdminReceiver`. Si Android rechaza el comando, revisá el estado de aprovisionamiento y la salida de `adb shell dpm help`. Algunos fabricantes exigen NFC/QR/Zero-touch o políticas empresariales. La app no se eleva privilegios y no puede saltar esas condiciones.

Después del provisionamiento, abrir app, entrar al panel y activar “Modo dedicado”; el lock task aplicará la allowlist de un solo paquete y quitará funciones de Lock Task. Para salir de forma segura, activar el gesto administrativo, PIN y usar “Estado y salir del modo quiosco”. Si el panel estuviera bloqueado, un administrador Device Owner puede usar `adb shell dpm remove-active-admin com.generic.kiosk/.AdminReceiver` solo donde Android lo permita; de otro modo, seguir el procedimiento de recuperación del fabricante/restablecimiento.

## Firma release

Generá y guardá una clave propia fuera del repositorio, con contraseña robusta y copia de seguridad protegida:

```powershell
keytool -genkeypair -v -keystore "$env:USERPROFILE\kiosk-release.jks" -alias kiosk -keyalg RSA -keysize 4096 -validity 10000
```

La versión release actual se produce sin firmar. Firmala localmente con Android Studio **Build > Generate Signed Bundle / APK** o `apksigner` del SDK Build Tools. Nunca agregues el keystore o sus contraseñas al repo. Conservá la clave: las actualizaciones deben firmarse con el mismo certificado.

## Recuperación y límites

- **Modo fullscreen convencional:** salir con el gesto del sistema o navegación Android. El ocultamiento inmersivo no es un bloqueo de seguridad y algunos fabricantes pueden mostrar controles del sistema.
- **Lock Task:** solo Device Owner controla la allowlist; el comando `startLockTask()` en un equipo normal no promete un quiosco administrado.
- **Inicio al arranque:** `BOOT_COMPLETED` requiere que la app haya sido iniciada al menos una vez y que el fabricante permita el inicio. Android moderno limita inicios de Activity desde background; también se puede elegir esta app como launcher predeterminado desde los ajustes del sistema.
- **Corte eléctrico:** el encendido automático es una función del firmware, no de la app.
- **HTTP:** se permite por compatibilidad, pero puede transmitir datos sin cifrar. Preferí HTTPS.
- **Panel:** botón visible **Ajustes** y PIN. El PIN inicial `1234` no es adecuado para producción. La app guarda un HMAC del PIN y mantiene la clave HMAC en Android Keystore; un dispositivo rooteado/comprometido sigue fuera del modelo de protección.
- **Compatibilidad fabricante:** Xiaomi/MIUI, Samsung y otros pueden requerir permitir autoinicio o excluir optimización de batería en ajustes del sistema.

## Lista de prueba manual

1. Instalar en Android 8+ y abrir una URL HTTPS; verificar navegación, formularios, cookies y sesión tras reiniciar la app.
2. Probar URL HTTP local, URL mal formada y esquemas `file:`, `intent:` y `javascript:`; solo HTTP(S) debe cargar.
3. Desconectar/reconectar red y comprobar pantalla de error y recarga.
4. Probar inmersivo con gestos y botones de 3 teclas, rotación automática, orientación fijada y pantalla encendida.
5. Probar gesto administrativo, PIN correcto/incorrecto, guardar/cerrar/volver, cambio de PIN.
6. Probar arranque tras reinicio en cada fabricante objetivo; registrar si el sistema bloquea `startActivity`.
7. En equipo de prueba recién restablecido, aprovisionar Device Owner, activar Lock Task, verificar navegación del sistema bloqueada y usar salida administrativa.
8. Verificar con descarga/enlaces externos desactivados y documentar los límites de esta versión.

## Permisos

Solo `INTERNET`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED` y el binding obligatorio del receiver Device Admin. No se solicita ubicación, almacenamiento, contactos, cámara ni servicio en primer plano.
