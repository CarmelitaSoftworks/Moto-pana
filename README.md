# Moto Intercom (nombre provisional)

Intercomunicador de moto universal para Android: cualquier casco con Bluetooth se conecta al
teléfono como manos libres y la app crea una sala de voz grupal en tiempo real por internet
entre pilotos.

Proyecto de [Carmelita Softworks](https://github.com/CarmelitaSoftworks). El diseño, los
requisitos y el orden de trabajo están en [CLAUDE.md](CLAUDE.md).

> Estado: esqueleto del proyecto. Todavía no hay voz ni grupos.

## Módulos

| Módulo | Contenido |
|---|---|
| `app` | Entrypoint, tema, navegación, DI |
| `core/audio` | `AudioRouter`: enrutamiento Bluetooth y foco de audio |
| `core/rtc` | `RtcClient`: wrapper de LiveKit |
| `core/service` | `IntercomService`: servicio en primer plano de la sesión |
| `feature/group` | Crear grupo o unirse a uno |
| `feature/ride` | Pantalla de viaje |
| `feature/settings` | Ajustes |
| `build-logic` | Convention plugins de Gradle compartidos por los módulos |

## Compilar

Requiere JDK 17 o superior y el Android SDK (plataforma 35).

```bash
./gradlew assembleDebug         # build
./gradlew test                  # tests unitarios
./gradlew ktlintCheck detekt    # lint
```

## Licencia

[GPL-3.0](LICENSE).
