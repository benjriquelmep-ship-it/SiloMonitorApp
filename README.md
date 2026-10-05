# SiloMonitorApp 🌾🚜

**SiloMonitorApp** es una solución móvil nativa para Android diseñada para el monitoreo georreferenciado, control de inventario y trazabilidad de alimento en tolvas y silos agroindustriales (caso de estudio Ariztía). Su objetivo es mitigar riesgos de desabastecimiento en galpones y optimizar la cadena logística de reposición en terreno mediante una arquitectura robusta *offline-first*.

---

## 📌 Características Principales

- **Monitoreo en Tiempo Real y Semáforo de Capacidad:**
    - Semáforo visual automatizado por umbrales de stock:
        - 🟢 **Normal (> 40%):** Abastecimiento óptimo (`#2E7D32`).
        - 🟡 **Advertencia (20% – 40%):** Reposición recomendada para prevenir quiebres (`#F57F17`).
        - 🔴 **Crítico (< 20%):** Alerta prioritaria de desabastecimiento inminente (`#C62828`).
- **Estimación Predictiva de Autonomía:**
    - Proyección en tiempo real en el dispositivo de las horas de alimento restantes basada en la media móvil de consumo de los últimos 3 a 7 días en el galpón asociado:
      > **Horas de Autonomía = (Stock Actual en kg / Consumo Promedio Diario en kg) × 24 horas**
- **Acceso Dual a Silos (QR + Ingreso Manual):**
    - Identificación instantánea mediante escáner de códigos QR.
    - Flujo de contingencia (*fallback*) para búsqueda, selección y registro manual en caso de etiquetas dañadas o fallas de lectura en terreno.
- **Validaciones Anti-Error en Terreno:**
    - *Bloqueo de sobrellenado:* Impide registrar cargas si **(Stock Actual + Carga) > Capacidad Máxima** del silo.
    - *Prevención de saldo negativo:* Bloquea transacciones de consumo superiores al stock real disponible (**Consumo ≤ Stock Actual**).
- **Captura de Evidencia Fotográfica:**
    - Registro de anomalías físicas, daños estructurales o alimento deteriorado adjuntando fotografías capturadas en faena con CameraX.
- **Cartografía y Georreferenciación Interactiva:**
    - Visualización espacial de silos y sectores con **Google Maps SDK**, con pines dinámicos teñidos según el semáforo y tarjetas desplegables (*ModalBottomSheet*) para consultar la ficha técnica y coordinar camiones.
- **Control de Acceso Basado en Roles (RBAC):**
    - **Operario:** Registro de transacciones locales, escaneo QR/manual y visualización de su granja asignada.
    - **Supervisor:** Auditoría de movimientos, visualización de incidencias zonales y solicitud de camiones de reposición.
    - **Jefatura:** Mapa macro de todas las plantas, planificación logística y dashboard de analítica histórica.
    - **Administrador (ADMIN):** Alta manual de silos con captura GPS, generación de QR, CRUD de infraestructura y usuarios.
- **Alertas en Segundo Plano:**
    - Auditoría periódica con **WorkManager** para detectar niveles críticos (< 20%) y disparar notificaciones nativas con acciones rápidas de reposición.

---

## 🏗 Arquitectura Híbrida Offline-First

Para asegurar operatividad continua en faenas agrícolas sin cobertura de red:
1. **Persistencia Local (Room SQLite):** Almacenamiento local e instantáneo de entidades (`UsuarioEntity`, `GranjaEntity`, `SiloEntity`, `MovimientoEntity`, `AlertaEntity`).
2. **Cola de Sincronización Diferida (Sync Queue):** Cada registro sin red se marca con `sincronizado = false`. Al detectar conectividad, el sistema transmite las transacciones a la nube.
3. **Sincronización Cloud (Firebase Cloud Firestore):** Consolidación de datos multi-planta en tiempo real para supervisores y administradores.
4. **Almacenamiento Cloud (Firebase Storage):** Almacenamiento optimizado de las fotografías de auditoría e incidencias capturadas en terreno.

---

## 🛠️ Stack Tecnológico

- **Lenguaje:** Kotlin
- **UI Toolkit:** Jetpack Compose & Material Design 3
- **Patrón de Diseño:** MVVM (Model-View-ViewModel) + Repository Pattern
- **Base de Datos Local:** Android Room Database (SQLite)
- **Base de Datos Cloud:** Firebase Cloud Firestore
- **Almacenamiento Multimedia:** Firebase Storage
- **Visión Computacional & Hardware:** CameraX + Google ML Kit (Barcode Scanning)
- **Cartografía & GPS:** Google Maps Compose SDK & Location Services
- **Background Tasks:** Android Jetpack WorkManager
- **Asincronía & Flujos:** Kotlin Coroutines & StateFlow
- **Compatibilidad Mínima:** Android 7.0 (API nivel 24 / Nougat)

---

## 📂 Estructura del Proyecto

```text
com.example.silomonitorapp/
├── data/
│   ├── local/                     # Capa de datos local (Room Database)
│   │   ├── dao/                   # SiloDao, MovimientoDao, UsuarioDao
│   │   ├── entities/              # SiloEntity, MovimientoEntity, etc.
│   │   └── AppDatabase.kt         # Instancia y configuración de Room
│   ├── remote/                    # Capa Cloud (Firebase Firestore & Storage)
│   │   ├── FirestoreService.kt
│   │   └── StorageService.kt
│   └── repository/                # Implementación híbrida de repositorios
├── domain/
│   ├── model/                     # Modelos de dominio y enums de negocio
│   └── usecase/                   # Lógica de autonomía y validaciones anti-error
├── ui/
│   ├── components/                # Pines de mapas, modales y barras de progreso
│   ├── navigation/                # Control de rutas y navegación con RBAC
│   ├── screens/                   # Vistas Compose (Home, Ficha, Formulario, Mapa, Admin)
│   └── theme/                     # Identidad corporativa Ariztía (Colores, Tipografía)
├── utils/                         # Formateadores, gestor de cámara y permisos
├── workers/                       # SiloAlertWorker para auditoría periódica
└── MainActivity.kt                # Entrada principal de la aplicación