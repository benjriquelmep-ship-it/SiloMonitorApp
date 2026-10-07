# SiloMonitorApp 🌾🚜

**SiloMonitorApp** es una solución móvil nativa para Android desarrollada en Kotlin y Jetpack Compose, diseñada para el monitoreo georreferenciado, control de inventario y trazabilidad de alimento en tolvas y silos agroindustriales (caso de estudio Ariztía). Su objetivo es mitigar riesgos de desabastecimiento en galpones y optimizar la cadena logística de reposición en terreno mediante una arquitectura robusta *offline-first*.

---

## 👥 Información del Proyecto y Equipo

- **Institución:** Duoc UC
- **Asignatura:** Desarrollo de Aplicaciones Móviles (DSY1105)
- **Integrantes:**
    - Benjamín Alexis Riquelme Pozo
    - [Nombre y Apellido de tu compañero/a]
- **Docente:** [Nombre del Docente]
- **Planificación del Proyecto:** [Enlace al tablero de Trello]

---

## 📌 Características Principales

- **Monitoreo en Tiempo Real y Semáforo de Capacidad:**
    - Semáforo visual automatizado por umbrales de stock:
        - 🟢 **Normal (> 40%):** Abastecimiento óptimo (`#2E7D32`).
        - 🟡 **Advertencia (20% – 40%):** Reposición recomendada para prevenir quiebres (`#F57F17`).
        - 🔴 **Crítico (< 20%):** Alerta prioritaria de desabastecimiento inminente (`#C62828`).
- **Estimación Predictiva de Autonomía:**
    - Proyección en tiempo real de las horas de alimento restantes basada en la media móvil de consumo de los últimos 3 a 7 días en el galpón asociado:
      > **Horas de Autonomía = (Stock Actual en kg / Consumo Promedio Diario en kg) × 24 horas**
- **Acceso Dual a Silos (QR + Ingreso Manual):**
    - Identificación instantánea mediante escáner de códigos QR usando ML Kit.
    - Flujo de contingencia (*fallback*) para búsqueda, selección y registro manual en caso de etiquetas dañadas o fallas de lectura en terreno.
- **Validaciones Anti-Error en Terreno (Desacopladas en Dominio/ViewModel):**
    - *Bloqueo de sobrellenado:* Impide registrar cargas si **(Stock Actual + Carga) > Capacidad Máxima** del silo.
    - *Prevención de saldo negativo:* Bloquea transacciones de consumo superiores al stock real disponible (**Consumo ≤ Stock Actual**).
    - Retroalimentación visual inmediata con mensajes de error tipificados e íconos en cada campo de formulario.
- **Captura de Evidencia Fotográfica:**
    - Registro de anomalías físicas, daños estructurales o auditorías de carga capturando fotografías en faena con la cámara nativa mediante `FileProvider` y renderizado inmediato en la interfaz.
- **Cartografía y Georreferenciación Interactiva:**
    - Visualización espacial de silos y sectores con **Google Maps SDK**, con pines dinámicos coloreados según el semáforo y tarjetas desplegables (*ModalBottomSheet*) para consultar la ficha técnica y coordinar camiones.
- **Exportación de Reportes Nativos (Auditoría):**
    - Generación de planillas de inventario en formato **CSV** delimitado por punto y coma (compatible con Microsoft Excel).
    - Creación de informes ejecutivos en formato **PDF** con membrete oficial institucional y estados coloreados mediante `PdfDocument` nativo de Android y `Android Sharesheet`.
- **Control de Acceso Basado en Roles (RBAC):**
    - **Operario:** Registro de transacciones locales, escaneo QR/manual y visualización de su granja asignada.
    - **Supervisor:** Auditoría de movimientos, visualización de incidencias zonales y solicitud de camiones de reposición.
    - **Jefatura:** Mapa macro de todas las plantas, planificación logística y dashboard de analítica histórica.
    - **Administrador (ADMIN):** Alta manual de silos con captura GPS, generación de QR, CRUD de infraestructura y usuarios.
- **Alertas y Auditoría Periódica:**
    - Monitoreo en segundo plano con **WorkManager** para evaluar niveles críticos (< 20%) y emitir notificaciones locales al dispositivo.

---

## 🔐 Usuarios de Demostración (RBAC)

Credenciales preconfiguradas para pruebas locales (definidas en `data/sesion/UsuariosDemo.kt` con hashing SHA-256):

| Usuario | Contraseña | Rol | Alcance Asignado |
|---|---|---|---|
| `operario` | `Operario2026!` | Operario | Granja El Paico |
| `supervisor` | `Supervisor2026!` | Supervisor | Granja El Paico y Granja Pomaire |
| `jefatura` | `Jefatura2026!` | Jefatura | Todas las granjas |
| `admin` | `Admin2026!` | Administrador | Todas las granjas |

---

## 🏗 Arquitectura del Software

El proyecto sigue una arquitectura **MVVM (Model-View-ViewModel)** bajo un enfoque **Offline-First**, separando estrictamente responsabilidades:

1. **Capa de Presentación (UI):** Desarrollada 100% en Jetpack Compose, basada en componentes reutilizables, animaciones reactivas (`animateFloatAsState`, `animateItem`) y consumo de estado unidireccional vía `StateFlow`.
2. **Capa de Dominio (Domain):** Encapsula modelos de negocio, reglas de validación en terreno desacopladas de la interfaz y casos de uso de estimación de consumo.
3. **Capa de Datos (Data):**
    - **Persistencia Local (Room SQLite):** Base de datos `AppDatabase` con DAOs y entidades (`SiloEntity`, `MovimientoEntity`, `SolicitudCamionEntity`) como fuente única de verdad en el dispositivo.
    - **Capa Remota (Firebase Cloud Firestore):** Sincronización asíncrona de movimientos y solicitudes mediante `SyncRepository` cuando el dispositivo detecta conexión a internet.
    - **Módulo de Reportes:** `ReporteHelper` para la generación y distribución de archivos en caché segura con `FileProvider`.

---

## 📱 Recursos Nativos del Dispositivo y Permisos

Para cumplir con las exigencias de integración de hardware nativo de Android, la aplicación implementa y gestiona los siguientes permisos:

1. **Cámara Nativa (`android.permission.CAMERA`):**
    - Utilizada para la captura de fotos de evidencia y el escaneo de códigos QR mediante Google ML Kit.
2. **Almacenamiento Seguro (`androidx.core.content.FileProvider`):**
    - Manejo de URIs seguras (`content://`) para almacenar fotos de auditoría en la memoria privada de la aplicación y compartir reportes CSV/PDF sin exponer rutas absolutas del sistema.
3. **Geolocalización (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`):**
    - Captura precisa de coordenadas geográficas en faena para la asignación de silos y renderizado en Google Maps.
4. **Notificaciones (`android.permission.POST_NOTIFICATIONS`):**
    - Emisión de alertas locales al operario ante quiebres de stock inminentes.

---

## 🛠️ Stack Tecnológico

- **Lenguaje:** Kotlin
- **UI Toolkit:** Jetpack Compose & Material 3
- **Arquitectura:** MVVM + Clean Architecture Principles
- **Persistencia Local:** Android Jetpack Room (SQLite)
- **Persistencia Cloud:** Firebase Cloud Firestore
- **Procesamiento de Imágenes / QR:** Google ML Kit Barcode Scanning
- **Mapas:** Google Maps Compose SDK & Play Services Location
- **Concurrencia:** Kotlin Coroutines & StateFlow
- **Generación Documental:** Android `PdfDocument` & Java I/O (CSV)
- **Min SDK:** 24 (Android 7.0 Nougat) | **Target SDK:** 34 o superior

---

## 🚀 Pasos para Clonar y Ejecutar el Proyecto

1. **Clonar el repositorio:**
   ```bash
   git clone [https://github.com/benjriquelmep-ship-it/SiloMonitorApp.git](https://github.com/benjriquelmep-ship-it/SiloMonitorApp.git)