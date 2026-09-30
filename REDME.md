# SiloMonitorApp 🌾🚜

**SiloMonitorApp** es un Producto Mínimo Viable (MVP) desarrollado en Android nativo para la monitorización en tiempo real de niveles de alimento en silos agrícolas, diseñado para optimizar la logística de recarga y prevenir desabastecimientos en granjas avícolas.

---

## 📌 Características Principales

- **Monitoreo en Tiempo Real:** Visualización del porcentaje disponible y estado operativo de cada silo (`NORMAL`, `ADVERTENCIA`, `CRITICO`).
- **Sincronización Cloud y Offline:** Integración con **Cloud Firestore** habilitando persistencia local en disco para operación sin cobertura de red en terreno.
- **Trazabilidad de Movimientos:** Registro de cargas, descargas y consumos asociados al operario de turno.
- **Sistema de Alertas:** Detección visual automática cuando el nivel desciende por debajo de umbrales críticos ($\le 20\%$).

---

## 🛠️ Stack Tecnológico

- **Lenguaje:** Kotlin
- **UI Toolkit:** Jetpack Compose & Material Design 3
- **Arquitectura:** MVVM (Model-View-ViewModel) + Repository Pattern
- **Base de Datos / Backend:** Firebase Cloud Firestore (Standard Edition)
- **Asincronía & Flujos:** Kotlin Coroutines & StateFlow
- **Compatibilidad Mínima:** Android 7.0 (API nivel 24 / Nougat)

---

## 📂 Estructura del Proyecto

```text
com.example.silomonitorapp/
├── data / model/           # Modelos de dominio y Repositorio Firestore
│   ├── SiloModels.kt       # Entidades Silo, Movimiento y Enums de estado
│   └── SiloRepository.kt   # Acceso a datos, persistencia offline y snapshot listeners
├── ui/
│   ├── screens/            # Pantallas e interfaces en Jetpack Compose
│   ├── theme/              # Esquema de color, tipografía y tema Material3
│   └── viewmodel/          # SiloViewModel y gestión de estados reactivos
└── MainActivity.kt         # Punto de entrada de la aplicación