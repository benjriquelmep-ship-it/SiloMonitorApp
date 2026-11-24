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

### 9. Clima en la Ubicación del Silo (API externa)
* La ficha técnica muestra el clima actual en las coordenadas GPS del silo (temperatura, humedad, viento y precipitación), consultado a la API pública **Open-Meteo**, con una recomendación para la descarga del camión (por ejemplo, evitar descargar con lluvia).

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

La app sigue los patrones **MVVM (Model-View-ViewModel)** y **Repository** con enfoque **Offline-First**. El backend está formado por **dos microservicios Spring Boot**, cada uno con su propia base de datos MySQL, y la app consume además una **API externa** (Open-Meteo):

```text
┌──────────────────────────────────────────────────────────────────────┐
│                     CAPA DE PRESENTACIÓN (UI)                        │
│          Jetpack Compose | Navigation | Componentes Material 3       │
└──────────────────────────────────▲───────────────────────────────────┘
                                   │ StateFlow / eventos
┌──────────────────────────────────┴───────────────────────────────────┐
│                           CAPA VIEWMODEL                             │
│        SiloViewModel  |  SesionViewModel  |  ClimaViewModel          │
└──────────────┬──────────────────────────────────────┬────────────────┘
               │ reglas puras                         │ datos
┌──────────────▼─────────────────┐   ┌────────────────▼─────────────────┐
│        CAPA DE DOMINIO         │   │          CAPA DE DATOS           │
│ ReglasTerreno · ReglasClima    │   │  SiloRepository · ClimaRepository│
│ ValidadorFormularios · Roles   │   └───┬─────────────┬────────────┬───┘
└────────────────────────────────┘       │ Retrofit    │ Room       │ Retrofit
            ┌────────────────────────────┤             │            │
   ┌────────▼────────┐  ┌────────────────▼┐  ┌─────────▼──────┐  ┌──▼──────────────┐
   │ ms-silos :8080  │  │ ms-solicitudes  │  │  Room (SQLite) │  │ Open-Meteo      │
   │ silos_db (MySQL)│  │ :8081           │  │  copia local / │  │ (API externa,   │
   │ silos y         │  │ solicitudes_db  │  │  modo offline  │  │  clima del silo)│
   │ movimientos     │  │ (MySQL)         │  └────────────────┘  └─────────────────┘
   └─────────────────┘  └─────────────────┘
   Firebase Firestore: respaldo en la nube de silos y movimientos.
```

### Paquetes de la app (`app/src/main/java/com/example/silomonitorapp`)

| Paquete | Contenido |
|---|---|
| `ui/screens`, `ui/components`, `ui/navigation`, `ui/theme` | Pantallas y componentes Compose, rutas de navegación y tema |
| `ui/viewmodel` | `SiloViewModel`, `SesionViewModel`, `ClimaViewModel` (estado con `StateFlow`) |
| `ui/model` | Modelos de UI (`SiloUi`, `MovimientoUi`, `SolicitudUi`…) |
| `domain` | Reglas puras y testeables: `ReglasTerreno`, `ReglasClima`, `ValidadorFormularios`, `Roles` |
| `data/local` | Room: entidades, DAOs, `AppDatabase` y `SiloRepository` |
| `data/remote` | Retrofit: `RetrofitClient`, servicios `SiloApiService`, `SolicitudApiService`, `ClimaApiService`, DTOs, `ClimaRepository` |

### Paquetes de cada microservicio (`backend/ms-silos`, `backend/ms-solicitudes`)

`model` (entidades JPA) · `repository` (Spring Data JPA) · `service` (reglas de negocio) · `controller` (endpoints REST) · `dto` (cuerpos de petición con validaciones) · `exception` (errores → `{"error": "mensaje"}`) · `config` (datos iniciales).

---

## 🔌 Endpoints utilizados

### Microservicio `ms-silos` — `http://localhost:8080`

| Método | Endpoint | Uso en la app |
|---|---|---|
| GET | `/api/silos` | Descarga los silos al abrir la app y al sincronizar |
| GET | `/api/silos/{id}` | Consulta un silo |
| POST | `/api/silos` | Crear silo (Administrador) |
| PUT | `/api/silos/{id}` | Editar silo (Administrador) |
| DELETE | `/api/silos/{id}` | Eliminar silo (Administrador) |
| GET | `/api/silos/{id}/movimientos` | Historial de cargas/consumos |
| POST | `/api/silos/{id}/movimientos` | Registrar carga o consumo (valida sobrellenado y saldo negativo) |

### Microservicio `ms-solicitudes` — `http://localhost:8081`

| Método | Endpoint | Uso en la app |
|---|---|---|
| GET | `/api/solicitudes` (`?siloId=SIL-001` opcional) | Bandeja de reposición (pendientes primero) |
| GET | `/api/solicitudes/{id}` | Consulta una solicitud |
| POST | `/api/solicitudes` | Solicitar camión (Supervisor/Admin). 409 si el silo ya tiene un camión pendiente |
| PATCH | `/api/solicitudes/{id}/revision` | Aprobar o rechazar (Jefatura/Admin). 409 si ya fue revisada |
| DELETE | `/api/solicitudes/{id}` | Eliminar una solicitud |

Errores de ambos microservicios: `400` (validación), `404` (no existe), `409` (regla de negocio), siempre con el cuerpo `{"error": "mensaje"}` que la app muestra al usuario.

### API externa — Open-Meteo (`https://api.open-meteo.com`)

| Método | Endpoint | Uso en la app |
|---|---|---|
| GET | `/v1/forecast?latitude={lat}&longitude={lon}&current=temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m,weather_code&timezone=auto` | Tarjeta **"Clima en el silo"** de la ficha técnica: temperatura, humedad, viento, precipitación y una recomendación para la descarga del camión |

Es un servicio público y gratuito (no requiere API key). Se consume con Retrofit (`ClimaApiService`) y **no** guarda nada en Room ni en los microservicios: solo se consulta al abrir la ficha.

---

## ▶️ Pasos para ejecutar

### 1. Base de datos
Iniciar **MySQL** (por ejemplo con XAMPP) en el puerto `3306`, usuario `root` sin contraseña. Las bases `silos_db` y `solicitudes_db` se crean solas al iniciar cada microservicio.

### 2. Microservicios (Java 17+)
Abrir cada carpeta en IntelliJ IDEA y ejecutar su clase principal, o desde una terminal:

```bash
cd backend/ms-silos        && mvn spring-boot:run   # http://localhost:8080
cd backend/ms-solicitudes  && mvn spring-boot:run   # http://localhost:8081
```

Prueba rápida: abrir `http://localhost:8080/api/silos` y `http://localhost:8081/api/solicitudes` en el navegador.

### 3. App Android
1. Abrir la carpeta raíz del proyecto en **Android Studio** y sincronizar Gradle.
2. Crear/editar `local.properties` (no se sube al repositorio):
   ```properties
   MAPS_API_KEY=tu_api_key_de_google_maps
   # Solo con celular físico (en el emulador no hace falta): IP del PC en la misma red Wi-Fi
   # API_SILOS_URL=http://192.168.1.50:8080/
   # API_SOLICITUDES_URL=http://192.168.1.50:8081/
   ```
3. Ejecutar en un emulador o dispositivo (▶ Run). En el emulador, `10.0.2.2` apunta al `localhost` del PC.
4. Iniciar sesión con una de las cuentas de demostración.

---

## 🧪 Pruebas unitarias y cobertura

| Módulo | Herramientas | Qué se prueba |
|---|---|---|
| App (`app/src/test`) | JUnit 5 + JUnit 4, MockK, Kotest, Coroutines Test | ViewModels (`SiloViewModel`, `ClimaViewModel`), repositorios (`SiloRepository`, solicitudes, `ClimaRepository`), modelos de UI y reglas de dominio |
| `ms-silos` / `ms-solicitudes` | JUnit 5 + Mockito | Reglas de negocio de cada `Service` |

Las APIs (Retrofit) y Room se reemplazan por **mocks**, por lo que las pruebas no necesitan internet, microservicios encendidos ni emulador.

```bash
./gradlew testDebugUnitTest      # ejecuta las pruebas de la app
./gradlew reporteCobertura       # reporte: app/build/reports/cobertura/html/index.html
./gradlew verificarCobertura     # falla si la cobertura de líneas de la lógica es < 80 %
cd backend/ms-solicitudes && mvn test
```

El reporte de cobertura mide la **lógica** (ViewModels, repositorios, dominio, DTOs) y excluye el código generado (R, BuildConfig, Room `*_Impl`), las pantallas Compose (se prueban con `androidTest`) y las clases que solo envuelven APIs de Android (Activity, notificaciones, PDF/CSV, Firestore).

<!-- Agregar aquí una captura del reporte de cobertura: ![Cobertura](docs/cobertura.png) -->

---

## 📦 APK firmado

<!-- Completar cuando se genere el APK firmado -->
1. Android Studio → **Build → Generate Signed App Bundle / APK… → APK**.
2. Crear la llave (`Create new…`): archivo `.jks`, contraseña, alias y datos del certificado.
3. Elegir la variante **release** y finalizar. El APK queda en `app/release/app-release.apk`.

Captura del APK firmado y del archivo `.jks`:

<!-- ![APK firmado](docs/apk-firmado.png) -->

---

## 📋 Planificación (Trello) y control de versiones

* **Tablero Trello:** <!-- pegar aquí el enlace al tablero -->
* **GitHub:** trabajo por ramas (`feature/*`, `fix/*`, `docs/*`) integradas a `master` mediante Pull Requests.

| Integrante | Responsabilidades principales |
|---|---|
| Benjamín Riquelme | <!-- completar --> |
| Aileen Oyaneder | <!-- completar --> |