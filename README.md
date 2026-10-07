# SiloMonitorApp 🌾🚜

**SiloMonitorApp** es una plataforma móvil nativa para Android desarrollada en Kotlin y Jetpack Compose, concebida para el monitoreo georreferenciado, la trazabilidad operacional y la optimización de la reposición de alimento en tolvas y silos agroindustriales (caso de estudio **Empresas Ariztía**).

El sistema resuelve la problemática de quiebres de inventario no advertidos y contingencias logísticas en galpones avícolas mediante una arquitectura reactiva **Offline-First**: las transacciones de faena se procesan, validan y almacenan de forma local en el dispositivo, sincronizándose con la nube una vez restablecida la conectividad.

> Proyecto desarrollado para la asignatura **Desarrollo de Aplicaciones Móviles (DSY1105)** — Escuela de Informática y Telecomunicaciones, **Duoc UC**.

---

## 👥 Equipo y Datos Académicos

* **Institución Educativa:** Duoc UC
* **Asignatura:** Desarrollo de Aplicaciones Móviles (`DSY1105`)
* **Integrantes:**
  * **Benjamín Alexis Riquelme Pozo**
  * **Aileen Oyaneder**

---

## 🎯 Problemática y Propuesta de Valor

En faenas de crianza y engorde avícola distribuidas geográficamente, el agotamiento de tolvas y silos compromete los ciclos productivos. Los registros en papel o dependientes de cobertura satelital/móvil fallan debido al aislamiento habitual de los planteles.

**SiloMonitorApp** entrega:
1. **Continuidad Operacional Total:** Registro ininterrumpido de cargas y consumos sin conexión a internet mediante base de datos SQLite/Room embebida.
2. **Defensa Anti-Error en Faena:** Reglas matemáticas de validación desacopladas que impiden registrar sobrellenados o saldos negativos en silos.
3. **Visibilidad Ejecutiva e Instantánea:** Semáforos visuales basados en niveles de capacidad, cálculo dinámico de autonomía en horas y exportación de auditorías en PDF y CSV.
4. **Trazabilidad con Evidencia:** Registro visual de guías de despacho, sellos o anomalías estructurales mediante la cámara nativa.

---

## 📌 Funcionalidades Principales

### 1. Monitoreo y Semáforo de Capacidad Automatizado
* **Cálculo reactivo de capacidad:** Actualización en tiempo real del porcentaje de ocupación a partir del stock en kilogramos y la capacidad máxima declarada.
* **Semáforo tricolor normado:**
  * 🟢 **Óptimo (> 40%):** Nivel suficiente de alimento (`#2E7D32`).
  * 🟠 **Advertencia (20% – 40%):** Nivel bajo; reposición sugerida para evitar contingencias (`#F57F17`).
  * 🔴 **Crítico (< 20%):** Alerta prioritaria; riesgo inminente de desabastecimiento (`#C62828`).
* **Buscador y filtros:** Filtrado simultáneo por estado de riesgo y coincidencia de texto (nombre de silo, código, granja o galpón).

### 2. Proyección Predictiva de Autonomía
* Algoritmo de cálculo en el dispositivo para estimar las horas operacionales restantes antes del quiebre:
  $$\text{Horas de Autonomía} = \left(\frac{\text{Stock Actual (kg)}}{\text{Consumo Promedio Diario (kg)}}\right) \times 24\,\text{horas}$$
* Visibilidad directa en las tarjetas resumen de la lista principal y en el encabezado de la ficha técnica.

### 3. Registro de Cargas/Consumos con Validaciones Centralizadas
* Formulario interactivo con retroalimentación visual, badges contextuales y soporte de errores por campo:
  * **Bloqueo estricto de sobrellenado:** Se rechaza la carga si $(\text{Stock Actual} + \text{Kilos Carga}) > \text{Capacidad Máxima}$.
  * **Prevención de saldo negativo:** Se rechaza el egreso si $\text{Kilos Consumo} > \text{Stock Actual}$.
  * **Obligatoriedad de campos:** Validación de rango numérico positivo y longitud mínima en observaciones.
* **Evidencia Fotográfica:** Activación de cámara fotográfica del sistema para adjuntar pruebas visuales a la transacción.

### 4. Acceso Dual: Escáner QR y Contingencia Manual
* **Identificación Rápida por QR:** Integración con Google ML Kit Barcode Scanning para leer etiquetas en la base del silo y saltar inmediatamente a la pantalla operativa según el perfil del usuario.
* **Flujo de Contingencia:** Selector manual optimizado con búsqueda rápida para faenas donde el código QR se encuentre desgastado, sucio o inaccesible.

### 5. Cartografía y Georreferenciación Interactiva
* Visualización en **Google Maps Compose SDK** con marcadores teñidos dinámicamente según el semáforo de capacidad.
* Centrado de mapa mediante proveedor de ubicación GPS nativo (`FusedLocationProviderClient`).
* Ficha técnica desplegable mediante componente *ModalBottomSheet* para consultar detalles y gestionar pedidos sin abandonar el mapa.

### 6. Logística de Camiones y Flujo de Reposición
* **Solicitud (Supervisor/Admin):** Emisión de solicitudes de reposición desde la ficha del silo, sugiriendo automáticamente los kilos faltantes para el llenado y marcando prioridad urgente en silos críticos.
* **Aprobación (Jefatura/Admin):** Bandeja unificada con acciones para autorizar o denegar despachos, bloqueando solicitudes duplicadas o solicitudes con volumen mayor a la capacidad libre.

### 7. Dashboard Analítico y Reportabilidad Ejecutiva
* Tarjetas de resumen: llenado global consolidado, conteo de silos en condición crítica, silo con menor autonomía y solicitudes pendientes.
* Gráfico de barras interactivo con el consumo de los últimos 7 días.
* **Exportación de Documentos Formales:**
  * **Planilla CSV:** Datos tabulados delimitados por punto y coma (`;`), optimizados para análisis en Microsoft Excel.
  * **Informe Oficial PDF:** Generación en memoria en tamaño A4 con membrete institucional, tablas y colores semafóricos mediante `PdfDocument` de Android.
  * Distribución mediante el menú del sistema operativo (**Android Sharesheet**) a través de `FileProvider`.

### 8. Notificaciones de Emergencia en Terreno
* Disparo de notificaciones nativas locales del sistema al registrar una transacción que reduzca el stock de un silo a menos del 20%, con navegación directa hacia el detalle del silo.

---

## 🔐 Control de Acceso Basado en Roles (RBAC)

La aplicación implementa un esquema de control de accesos centralizado en `domain/Roles.kt`, donde las pantallas y acciones evalúan permisos específicos (`puedeRegistrarMovimientos`, `puedeSolicitarCamion`, `puedeAprobarCamion`, etc.):

| Funcionalidad / Módulo | Operario | Supervisor | Jefatura | Administrador |
|:---|:---:|:---:|:---:|:---:|
| **Visibilidad de Silos** | Granja asignada | Granjas de su zona | Todas las granjas | Todas las granjas |
| **Registrar Carga / Consumo** | ✅ | ❌ | ❌ | ✅ |
| **Acción tras Escanear QR** | Formulario directo | Ficha técnica | Ficha técnica | Ficha técnica |
| **Solicitar Camión de Alimento** | ❌ | ✅ | ❌ | ✅ |
| **Aprobar / Rechazar Camiones** | ❌ | ❌ | ✅ | ✅ |
| **Bandeja de Reposición** | ❌ | ✅ *(Lectura)* | ✅ *(Gestión)* | ✅ *(Gestión)* |
| **Dashboard y Reportes (CSV/PDF)** | ❌ | ❌ | ✅ | ✅ |
| **Crear / Editar Silos con GPS** | ❌ | ❌ | ❌ | ✅ |

### Cuentas de Acceso de Demostración (`data/sesion/UsuariosDemo.kt`)

Las credenciales locales están aseguradas con cifrado de contraseñas mediante hash SHA-256:

| Usuario | Contraseña | Rol Asignado | Alcance Geográfico |
|:---|:---|:---|:---|
| `operario` | `Operario2026!` | Operario | Granja El Paico |
| `supervisor` | `Supervisor2026!` | Supervisor | Granja El Paico y Granja Pomaire |
| `jefatura` | `Jefatura2026!` | Jefatura | Global (Todas las granjas) |
| `admin` | `Admin2026!` | Administrador | Global (Acceso total) |

---

## 🏗 Arquitectura del Software

El proyecto se fundamenta en los patrones **MVVM (Model-View-ViewModel)** y **Repository**, desacoplando la lógica de negocio pura del framework de Android y garantizando una arquitectura **Offline-First**:

```text
┌────────────────────────────────────────────────────────┐
│               CAPA DE PRESENTACIÓN (UI)                │
│    Jetpack Compose | Navigation | Componentes M3       │
└───────────────────────────▲────────────────────────────┘
                            │ Estado (StateFlow) / Eventos
┌───────────────────────────┴────────────────────────────┐
│                    CAPA VIEWMODEL                      │
│     SiloViewModel  |  SesionViewModel (Coroutines)     │
└─────────────┬────────────────────────────┬─────────────┘
              │ Consulta reglas puras      │ Lectura/Escritura
┌─────────────▼───────────────┐ ┌──────────▼─────────────┐
│       CAPA DE DOMINIO       │ │     CAPA DE DATOS      │
│   ReglasTerreno (Validación)│ │   SiloRepositoryImpl   │
│   Roles (Matriz permisos)   │ └──────────┬─────────────┘
└─────────────────────────────┘            │
                   ┌───────────────────────┴───────────────────────┐
                   │                                               │
       ┌───────────▼───────────┐                       ┌───────────▼───────────┐
       │   PERSISTENCIA ROOM   │                       │   FIREBASE FIRESTORE  │
       │    (Fuente de Verdad) │                       │ (Sincronización Cloud)│
       │  Silos, Movimientos,  │                       │  Colas y transacciones│
       │       Solicitudes     │                       │     asíncronas        │
       └───────────────────────┘                       └───────────────────────┘