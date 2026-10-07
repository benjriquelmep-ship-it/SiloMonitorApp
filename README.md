# SiloMonitorApp 🌾🚜

**SiloMonitorApp** es una aplicación nativa Android para el monitoreo georreferenciado, el control de inventario y la trazabilidad del alimento en tolvas y silos agroindustriales (caso de estudio **Ariztía**). Busca reducir el riesgo de desabastecimiento en los galpones y agilizar la coordinación de la reposición en terreno, con una arquitectura *offline-first*: todo se registra primero en el teléfono, aunque no haya señal.

> Proyecto de la asignatura **Desarrollo de Aplicaciones Móviles (DSY1105)** — Duoc UC.

---

## 📌 Funcionalidades

### Monitoreo y semáforo de capacidad
- Lista de silos ordenada del más crítico al más lleno, con barra de nivel animada.
- Semáforo automático por porcentaje de llenado:
  - 🟢 **Normal (> 40%)** — `#2E7D32`
  - 🟠 **Advertencia (20% – 40%)** — `#F57F17`
  - 🔴 **Crítico (< 20%)** — `#C62828`
- Filtros por estado y buscador por nombre, código, granja o galpón.

### Autonomía estimada
> **Horas de autonomía = (Stock actual en kg ÷ Consumo promedio diario en kg) × 24**

Se muestra en cada tarjeta y en la ficha técnica del silo.

### Registro de movimientos (carga y consumo)
- Formulario con tipo de movimiento, kilos, observación y **foto de evidencia** tomada con la cámara.
- **Bloqueo de sobrellenado:** no se puede cargar si *stock + carga > capacidad máxima*.
- **Prevención de saldo negativo:** no se puede consumir más que el stock disponible.
- **Historial** de los últimos movimientos en la ficha del silo, con foto y estado de sincronización.

### Acceso dual a los silos
- **Escáner QR** con CameraX + ML Kit (modelo de Google Play Services).
- **Respaldo manual:** buscador en la lista y alta manual de silos cuando falla la lectura del QR.

### Mapa georreferenciado
- Silos en **Google Maps** con marcadores teñidos según el semáforo.
- Filtro de solo críticos, centrado en la ubicación GPS y enfoque de un silo desde su ficha.

### Alertas
- **Notificación local** cuando un movimiento deja un silo bajo el 20% de capacidad. Al tocarla se abre la app.

### Gestión de silos (Administrador)
- Alta y edición de silos con código libre (letras, números y guiones, sin duplicados).
- Botón **"Usar mi ubicación actual"** para registrar las coordenadas con el GPS.
- Al editar, el stock no se modifica: solo cambia con cargas y consumos, para que el historial cuadre.

### Coordinación de reposición
- El **Supervisor** solicita un camión desde la ficha del silo. Se sugieren los kilos que faltan para llenarlo y se marca urgente si el silo está crítico.
- La **Jefatura** aprueba o rechaza las solicitudes desde la bandeja de *Reposición*.
- No se puede pedir más de lo que cabe ni duplicar una solicitud pendiente para el mismo silo.

### Dashboard (Jefatura y Administrador)
- Indicadores: llenado global, silos críticos, menor autonomía y camiones pendientes.
- Gráfico del consumo de los últimos 7 días y nivel de cada silo.
- **Exportación de reportes** del inventario desde la barra superior del dashboard:
  - **CSV** (separado por `;`, se abre directo en Excel): código, nombre, granja, galpón, capacidad, stock, porcentaje, estado y autonomía de cada silo.
  - **PDF** en tamaño A4 con el mismo inventario.
  - Ambos se comparten con el menú de Android (correo, WhatsApp, Drive, etc.).

### Sincronización con la nube
- Botón **⟳** en la barra superior para enviar a **Cloud Firestore** los movimientos y solicitudes pendientes, con indicador de progreso.

---

## 🔐 Roles y permisos (RBAC)

| Capacidad | Operario | Supervisor | Jefatura | Admin |
|---|:---:|:---:|:---:|:---:|
| Silos visibles | Su granja | Granjas de su zona | Todas | Todas |
| Registrar cargas y consumos | ✅ | — | — | ✅ |
| Al escanear un QR se abre | Formulario | Ficha | Ficha | Ficha |
| Solicitar camión | — | ✅ | — | ✅ |
| Aprobar o rechazar camiones | — | — | ✅ | ✅ |
| Ver bandeja de reposición | — | ✅ (consulta) | ✅ | ✅ |
| Dashboard y exportación de reportes (PDF / CSV) | — | — | ✅ | ✅ |
| Crear y editar silos | — | — | — | ✅ |

Los permisos están centralizados en `domain/Roles.kt`. Las pantallas consultan el permiso (por ejemplo `rol.puedeRegistrarMovimientos`), no el nombre del rol, y cada ruta protegida devuelve al usuario atrás si no tiene acceso.

### Usuarios de prueba

Mientras no exista autenticación en el backend, la app trae un usuario de prueba por rol, definidos en `data/sesion/UsuariosDemo.kt`. Las contraseñas se guardan como hash SHA-256:

| Usuario | Contraseña | Rol | Alcance |
|---|---|---|---|
| `operario` | `Operario2026!` | Operario | Granja El Paico |
| `supervisor` | `Supervisor2026!` | Supervisor | Granja El Paico y Granja Pomaire |
| `jefatura` | `Jefatura2026!` | Jefatura | Todas las granjas |
| `admin` | `Admin2026!` | Administrador | Todas las granjas |

> Son credenciales **solo de demostración**: se reemplazarán por usuarios reales al integrar el backend. La sesión queda guardada en el teléfono (solo el nombre de usuario, nunca la contraseña) hasta que se cierra con el botón de salir.

---

## 🏗 Arquitectura

**MVVM + Repository**, con Room como fuente de verdad en el dispositivo:

```text
Pantallas Compose ──▶ ViewModel (StateFlow) ──▶ Repository ──▶ Room (SQLite)
                                        │                    └──▶ Firestore (sincronización)
                                        └──▶ domain/ReglasTerreno (reglas puras, con tests)
```

- **Room (`AppDatabase`, versión 4):** tablas `silos`, `movimientos` y `solicitudes_camion`.
- **Offline-first:** cada movimiento y cada solicitud se guarda con `pendienteSincronizar = true` y se muestra con un ícono de nube tachada hasta que se envía.
- **Transacciones:** el nivel del silo y el registro del movimiento se guardan juntos (o quedan ambos, o ninguno).
- **Reglas de negocio aisladas** en `domain/ReglasTerreno.kt`, sin dependencias de Android, para probarlas con tests unitarios.
- **Sincronización con Cloud Firestore** (`data/remote/SyncRepository.kt`): envía los movimientos y solicitudes pendientes y los marca como sincronizados.

---

## 🛠️ Stack tecnológico

| Área | Tecnología |
|---|---|
| Lenguaje | Kotlin 2.2 |
| Interfaz | Jetpack Compose + Material 3 |
| Navegación | Navigation Compose (transiciones animadas) |
| Base de datos local | Room 2.6 (KSP) |
| Nube | Firebase Cloud Firestore |
| Cámara y QR | CameraX 1.6 + ML Kit Barcode Scanning (Play Services) |
| Mapas y GPS | Maps Compose + Fused Location Provider |
| Reportes | `PdfDocument` nativo de Android + CSV, compartidos con `FileProvider` |
| Asincronía | Coroutines + StateFlow |
| Tests | JUnit 4 |
| SDK | mínimo Android 8.0 (API 26), objetivo API 35, compilación API 37 |

Las librerías nativas del APK están alineadas a páginas de **16 KB**, como exigen los dispositivos Android nuevos.

---

## 📂 Estructura del proyecto

```text
com.example.silomonitorapp/
├── MainActivity.kt                 # Entrada: login o app según la sesión, permisos y canal de notificaciones
├── data/
│   ├── local/                      # Room: entidades, DAOs, AppDatabase y SiloRepository
│   ├── remote/                     # SyncRepository: sincronización con Firestore
│   ├── reports/                    # ReporteHelper: genera y comparte reportes CSV y PDF
│   └── sesion/                     # Sesión guardada y usuarios de prueba
├── domain/
│   ├── ReglasTerreno.kt            # Sobrellenado, saldo negativo, autonomía, validación de silos y camiones
│   └── Roles.kt                    # Roles, permisos y usuario autenticado
├── notificaciones/
│   └── NotificadorAlertas.kt       # Notificación de nivel crítico
└── ui/
    ├── components/                 # Barra Ariztía, tarjeta de silo, niveles animados, semáforo, foto, diálogo de camión
    ├── model/                      # Modelos de pantalla (SiloUi, MovimientoUi, SolicitudUi...)
    ├── navigation/AppNavHost.kt    # Rutas y aplicación de permisos por rol
    ├── screens/                    # Login, lista, ficha, formularios, mapa, escáner, reposición, dashboard
    ├── theme/                      # Identidad visual Ariztía (tema claro, fondo Gris Hielo #F8F9FA)
    └── viewmodel/                  # SiloViewModel y SesionViewModel
```

---

## ▶️ Cómo ejecutar el proyecto

1. Clonar el repositorio y abrirlo en **Android Studio**.
2. Esperar a que Gradle sincronice (**File → Sync Project with Gradle Files**).
3. Conectar un celular con **depuración USB** activada (o usar un emulador con Google Play).
4. Ejecutar la configuración **app** (▶).
5. Iniciar sesión con alguno de los [usuarios de prueba](#usuarios-de-prueba).

Desde la terminal también se puede compilar e instalar:

```bash
./gradlew installDebug
```

> **Nota:** la base de datos usa `fallbackToDestructiveMigration`. Cada vez que cambia su versión, los datos locales de prueba se borran y se vuelven a cargar los 6 silos iniciales. Antes de producción hay que reemplazarlo por migraciones reales.

---

## ✅ Tests

Las reglas de negocio y los permisos tienen tests unitarios:

```bash
./gradlew testDebugUnitTest
```

- `ReglasTerrenoTest`: sobrellenado, saldo negativo, autonomía, semáforo, validación de alta de silos y de solicitudes de camión.
- `RolesTest`: autenticación de usuarios de prueba y matriz de permisos por rol.

---

## 🗺️ Estado del proyecto

| Módulo | Estado |
|---|---|
| Lista, ficha, semáforo y autonomía | ✅ |
| Movimientos con validaciones, foto e historial | ✅ |
| Escáner QR y búsqueda manual | ✅ |
| Mapa con marcadores por estado y GPS | ✅ |
| Notificación de nivel crítico | ✅ |
| Alta y edición de silos | ✅ |
| Login, sesión y permisos por rol | ✅ |
| Solicitud y aprobación de camiones | ✅ |
| Dashboard de Jefatura | ✅ |
| Exportación de reportes PDF y CSV | ✅ |
| Sincronización manual con Firestore (botón ⟳) | ✅ |
| Subida de fotos a Firebase Storage | ⏳ Pendiente |
| Sincronización automática al recuperar conexión (WorkManager) | ⏳ Pendiente |
| Backend Spring Boot (`GET /api/silos`, `POST /api/movimientos`) + Retrofit | ⏳ Pendiente |
| Consumo diario calculado con media móvil de 3 a 7 días | ⏳ Pendiente |
| Despliegue del backend (Railway o Render) | ⏳ Pendiente |

---

## 👥 Equipo

| Integrante | Responsabilidad |
|---|---|
| Benjamín Riquelme | Datos, persistencia, sincronización y backend |
| Aileen Oyaneder | Interfaz, navegación, roles y funcionalidades de la app |
