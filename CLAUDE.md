# CLAUDE.md — Moto Intercom (nombre provisional)

Este archivo describe el proyecto para Claude Code. Léelo completo antes de escribir código.
Organización: Carmelita Softworks · Licencia: código abierto (GPL-3.0).

---

## 1. Qué es

App Android que funciona como **intercomunicador de moto universal**: cualquier casco con
Bluetooth (Sena, Cardo, Freedconn, genéricos, etc.) se conecta al teléfono como manos libres
y la app crea una **sala de voz grupal en tiempo real por internet** entre pilotos.

### Principio clave
La universalidad la da el teléfono, no el casco. **No** intentamos hablar los protocolos
propietarios (Sena Mesh, Cardo DMC): no tienen API pública. El casco solo es un auricular
Bluetooth (perfil HFP / LE Audio).

### No-objetivos (por ahora)
- iOS (fase posterior).
- Comunicación sin internet de largo alcance (imposible sin hardware de radio).
- Integración con apps oficiales de Sena/Cardo.

---

## 2. Stack

| Capa | Tecnología |
|---|---|
| App | Kotlin, Jetpack Compose, Material 3, minSdk 26, targetSdk 35 |
| Arquitectura | MVVM + Clean (ui / domain / data), Hilt para DI, Coroutines + Flow |
| Voz en tiempo real | WebRTC vía **LiveKit Android SDK** (`io.livekit:livekit-android`) |
| Codec | Opus, mono, 16–24 kbps, DTX activado, FEC activado |
| Servidor de medios | LiveKit Server (self-hosted en VPS o LiveKit Cloud para MVP) |
| Token / API | Cloudflare Worker (TypeScript) que firma JWT de LiveKit con `jose` |
| Datos de grupo | Cloudflare D1 (grupos, códigos de invitación) |
| Mapa (fase 3) | osmdroid o MapLibre (evitar dependencia de Google Maps) |
| Build | Gradle Kotlin DSL, version catalog (`libs.versions.toml`) |

---

## 3. Arquitectura

```
[Casco BT] ⇄ HFP/SCO ⇄ [Teléfono A: app] ⇄ WebRTC/Opus ⇄ [LiveKit SFU] ⇄ [Teléfono B..N] ⇄ [Casco BT]
                                  │
                                  └─ HTTPS ─ [Cloudflare Worker: auth + tokens + grupos (D1)]
```

### Estructura de módulos
```
app/                    → entrypoint, navegación, DI
core/audio/             → enrutamiento Bluetooth, AudioManager, foco de audio, procesamiento
core/rtc/               → wrapper de LiveKit (conectar, publicar mic, suscribir, reconexión)
core/service/           → Foreground Service de la sesión de intercom
core/ui/                → tema y componentes Compose compartidos por las features
feature/group/          → crear/unirse a grupo, código QR / código de 6 caracteres
feature/ride/           → pantalla de viaje (quién habla, mute, PTT, volumen)
feature/settings/       → modo de voz, sensibilidad, calidad, perfil
feature/map/            → (fase 3) posición del grupo
backend/worker/         → Cloudflare Worker (TypeScript)
build-logic/            → convention plugins de Gradle (config común de los módulos)
```

Paquete base: `com.carmelitasoftworks.motointercom`.

---

## 4. Requisitos funcionales

### MVP (Fase 1)
1. Crear grupo → genera código de 6 caracteres + QR.
2. Unirse a grupo con código o escaneando QR.
3. Conexión de voz grupal (2–8 personas) con audio por el casco Bluetooth.
4. Funciona con pantalla apagada y app en segundo plano.
5. Modos de transmisión:
   - **Voz abierta con VAD** (por defecto): solo transmite cuando detecta voz.
   - **Push-to-talk**: botón en pantalla o botón multimedia del casco (MediaSession).
6. Mute propio y silenciar a un participante.
7. Indicador de quién está hablando.
8. Reconexión automática al cambiar de red o salir de zona sin señal, con aviso por voz
   ("conexión perdida" / "reconectado") vía TextToSpeech.

### Fase 2
- Supresión de ruido de viento mejorada (RNNoise vía JNI) además del NS de WebRTC.
- Mezcla con música / GPS (ducking en lugar de cortar).
- Perfil de calidad: "Ahorro de datos" (12 kbps) / "Normal" (20 kbps) / "Alta" (32 kbps).
- Grupos de hasta 15.
- Estadísticas: consumo de datos, latencia, pérdida de paquetes.

### Fase 3
- Mapa con posición del grupo (data channel de LiveKit, cada 5 s).
- Detección de caída (acelerómetro) con alerta al grupo.
- Modo offline de corto alcance (Wi-Fi Direct / Nearby Connections).
- Versión iOS (CallKit + background audio).

---

## 5. Detalles técnicos críticos

### 5.1 Enrutamiento de audio Bluetooth (`core/audio`)
- API 31+: usar `AudioManager.setCommunicationDevice()` eligiendo el dispositivo
  `TYPE_BLUETOOTH_SCO` (o `TYPE_BLE_HEADSET` si existe → LE Audio).
- API < 31: `startBluetoothSco()` + `isBluetoothScoOn = true`, escuchar
  `ACTION_SCO_AUDIO_STATE_UPDATED`.
- Modo de audio: `MODE_IN_COMMUNICATION` mientras la sesión esté activa.
- Detectar conexión/desconexión del casco y hacer fallback al altavoz del teléfono con aviso.
- Pedir foco de audio `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` para que la música baje en vez de pararse cuando sea posible.
- Encapsular todo detrás de una interfaz `AudioRouter` para poder testear y soportar
  diferencias entre fabricantes.

### 5.2 Servicio en segundo plano (`core/service`)
- `IntercomService` como Foreground Service con `foregroundServiceType="microphone"`.
- Permisos en el manifiesto: `RECORD_AUDIO`, `BLUETOOTH_CONNECT`, `FOREGROUND_SERVICE`,
  `FOREGROUND_SERVICE_MICROPHONE`,
  `POST_NOTIFICATIONS`, `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK`.
- Notificación persistente con acciones: Mute, PTT, Salir.
- La sesión de LiveKit vive en el servicio, no en el ViewModel.
- Pedir al usuario que excluya la app de la optimización de batería (con explicación clara).

### 5.3 RTC (`core/rtc`)
- Una sala de LiveKit por grupo; nombre de sala = id del grupo.
- Publicar solo audio. Opciones: `echoCancellation`, `noiseSuppression`,
  `autoGainControl` activados; `dtx = true`; `red = true` (redundancia).
- `adaptiveStream` y `dynacast` activados.
- Manejar `RoomEvent.Reconnecting` / `Reconnected` / `Disconnected`.
- PTT: alternar `setMicrophoneEnabled()`; en modo VAD dejar el mic publicado con DTX.

### 5.4 Backend (`backend/worker`)
Endpoints:
- `POST /groups` → crea grupo, devuelve `{ groupId, code }`.
- `POST /groups/join` `{ code, displayName }` → devuelve `{ groupId, livekitUrl, token }`.
- `POST /token` `{ groupId, displayName }` → token renovado.
- Tokens con TTL de 6 h, permisos: `roomJoin`, `canPublish`, `canSubscribe`, `canPublishData`.
- Secretos (`LIVEKIT_API_KEY`, `LIVEKIT_API_SECRET`) en `wrangler secret`, **nunca** en el repo.
- Identidad anónima en MVP (UUID guardado en DataStore); login opcional más adelante.

---

## 6. Requisitos no funcionales
- Latencia boca-oído objetivo: < 300 ms en 4G.
- Consumo de datos: ≤ 20 MB/h por usuario en modo Normal.
- Batería: ≤ 10 %/h con pantalla apagada.
- Reconexión tras pérdida de red: < 5 s una vez vuelve la señal.
- UI usable con guantes: botones de al menos 64 dp, alto contraste, modo oscuro por defecto.

---

## 7. Convenciones
- Código y nombres en inglés; textos de UI en español (`strings.xml`), preparado para i18n.
- Commits: Conventional Commits (`feat:`, `fix:`, `refactor:`...).
- ktlint + detekt; sin warnings en CI.
- Tests: unitarios en domain/data (JUnit5 + MockK + Turbine); `AudioRouter` con fakes.
- No agregar dependencias pesadas sin justificarlo en el PR.

## 8. Comandos
```bash
./gradlew assembleDebug         # build
./gradlew test                  # tests unitarios
./gradlew ktlintCheck detekt    # lint
cd backend/worker && npm run dev        # worker local
cd backend/worker && npx wrangler deploy
```

---

## 9. Orden de trabajo sugerido para Claude Code
1. ✅ Crear el proyecto Android multi-módulo con Compose, Hilt y el version catalog.
2. Crear `backend/worker` con los endpoints y el esquema D1 (`groups`: id, code, created_at, expires_at).
3. Implementar `core/rtc`: conectar a una sala fija con un token de prueba y oír audio.
4. Implementar `core/audio` (`AudioRouter`) y probar con un casco real.
5. Mover la sesión a `IntercomService` y verificar que sigue con pantalla apagada.
6. `feature/group` (crear/unirse, QR) conectado al Worker.
7. `feature/ride`: hablantes activos, mute, PTT, botón del casco.
8. Reconexión + avisos por voz.
9. Pruebas en ruta y ajuste de parámetros de Opus/VAD.

Antes de cada paso, proponer el plan brevemente; después, compilar y correr tests.

## 10. Riesgos conocidos
- Sin cobertura no hay intercom (limitación de diseño, comunicarlo en la UI).
- Comportamiento Bluetooth inconsistente entre fabricantes de cascos y de teléfonos.
- Algunos fabricantes de Android (Xiaomi, Huawei, Samsung) matan servicios en segundo plano.
- HFP reduce la calidad del audio y puede interrumpir la música.
