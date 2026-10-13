package com.example.silomonitorapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.silomonitorapp.data.local.AppDatabase
import com.example.silomonitorapp.data.local.SiloEntity
import com.example.silomonitorapp.data.local.SiloRepository
import com.example.silomonitorapp.data.remote.SyncRepository
import com.example.silomonitorapp.domain.ReglasTerreno
import com.example.silomonitorapp.notificaciones.NotificadorAlertas
import com.example.silomonitorapp.ui.model.ConsumoDia
import com.example.silomonitorapp.ui.model.DatosSilo
import com.example.silomonitorapp.ui.model.EstadoSilo
import com.example.silomonitorapp.ui.model.EstadoSolicitud
import com.example.silomonitorapp.ui.model.MovimientoUi
import com.example.silomonitorapp.ui.model.SiloUi
import com.example.silomonitorapp.ui.model.SolicitudUi
import com.example.silomonitorapp.ui.model.TipoMovimiento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// VIEWMODEL principal (patrón MVVM).
// Conecta las pantallas con el Repository: transforma los datos de Room en modelos de UI
// (SiloUi, MovimientoUi...) y los expone como StateFlow. Las pantallas solo observan
// estos flujos y llaman a sus funciones; nunca tocan la base de datos.
class SiloViewModel(application: Application) : AndroidViewModel(application) {

    // Acceso a los datos locales (Room) y a la sincronización con la nube (Firestore).
    private val repository: SiloRepository
    private val syncRepository: SyncRepository = SyncRepository(application)

    // Estado reactivo de la sincronización remota con Firestore
    private val _sincronizando = MutableStateFlow(false)
    val sincronizando: StateFlow<Boolean> = _sincronizando.asStateFlow()

    // Mapeo reactivo directo de Room a SiloUi
    val silosUi: StateFlow<List<SiloUi>>

    // Patrón '_privado / público': el ViewModel modifica _mensajeOperacion (Mutable)
    // y las pantallas solo pueden leer mensajeOperacion. Se muestra como Toast.
    private val _mensajeOperacion = MutableStateFlow<String?>(null)
    val mensajeOperacion: StateFlow<String?> = _mensajeOperacion.asStateFlow()

    // init: se ejecuta una vez al crear el ViewModel.
    init {
        // Abre la base de datos y crea el repositorio.
        val db = AppDatabase.obtenerBaseDatos(application)
        repository = SiloRepository(db)

        // Convierte cada SiloEntity de Room a SiloUi en tiempo real
        silosUi = repository.silosFlow.map { lista ->
            lista.map { entity ->
                SiloUi(
                    id = entity.id,
                    codigo = entity.id,
                    nombre = entity.nombre,
                    granja = entity.granja,
                    galpon = entity.galpon,
                    capacidadMaxKg = entity.capacidadMaxima,
                    stockActualKg = entity.nivelActual,
                    consumoPromedioDiarioKg = entity.consumoPromedioDiario,
                    latitud = entity.latitud,
                    longitud = entity.longitud
                )
            }
        // stateIn convierte el Flow en StateFlow: guarda el último valor para la UI.
        // WhileSubscribed(5000): deja de escuchar Room 5 s después de que nadie lo observe.
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Precarga de datos semilla (el repositorio ya trabaja en Dispatchers.IO)
        viewModelScope.launch {
            try {
                repository.precargarSilosSiEstaVacio()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** Sincroniza lotes pendientes de Room hacia Cloud Firestore */
    fun sincronizarConFirestore() {
        // Evita sincronizar dos veces al mismo tiempo si se toca el botón repetidamente.
        if (_sincronizando.value) return
        viewModelScope.launch {
            // true muestra el indicador de carga en la barra superior; false lo oculta.
            _sincronizando.value = true
            val res = syncRepository.sincronizarTodo()
            _sincronizando.value = false
            // Arma el mensaje del Toast según el resultado.
            _mensajeOperacion.value = if (res.exitoso) {
                if (res.movimientosSincronizados == 0 && res.solicitudesSincronizadas == 0) {
                    "Todo está al día con la nube"
                } else {
                    "Sincronizados: ${res.movimientosSincronizados} movimientos y ${res.solicitudesSincronizadas} solicitudes"
                }
            } else {
                "Error al sincronizar: ${res.error ?: "desconocido"}"
            }
        }
    }

    /** Bandeja de solicitudes de camión, con el nombre y la granja de cada silo. */
    val solicitudesUi: StateFlow<List<SolicitudUi>> =
        // combine junta dos flujos: cada vez que cambian las solicitudes o los silos,
        // se vuelve a armar la lista con el nombre y la granja de cada silo.
        combine(repository.solicitudesFlow, repository.silosFlow) { solicitudes, silos ->
            val silosPorId = silos.associateBy { it.id }
            solicitudes.map { s ->
                val silo = silosPorId[s.siloId]
                SolicitudUi(
                    id = s.id,
                    siloId = s.siloId,
                    siloNombre = silo?.nombre ?: s.siloId,
                    granja = silo?.granja.orEmpty(),
                    kgSolicitados = s.kgSolicitados,
                    urgente = s.urgente,
                    observacion = s.observacion,
                    solicitadoPor = s.solicitadoPor,
                    fechaSolicitud = s.fechaSolicitud,
                    estado = EstadoSolicitud.valueOf(s.estado),
                    revisadoPor = s.revisadoPor
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Kilos consumidos por día en los últimos [dias] días (incluye días sin consumo). */
    fun consumoUltimosDias(dias: Int = 7): Flow<List<ConsumoDia>> {
        // Fecha de inicio: hoy a las 00:00 menos (dias - 1) días.
        val inicio = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -(dias - 1))
        }
        val formato = SimpleDateFormat("EEE d", Locale.forLanguageTag("es-CL"))
        return repository.movimientosDesdeFlow(inicio.timeInMillis).map { movimientos ->
            // Solo interesan los consumos (no las cargas).
            val consumos = movimientos.filter { it.tipo == "CONSUMO" }
            // Un valor por día: suma los kilos consumidos entre las 00:00 y las 24:00 de ese día.
            (0 until dias).map { i ->
                val desde = (inicio.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
                val hasta = (desde.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
                ConsumoDia(
                    etiqueta = formato.format(desde.time),
                    kg = consumos.filter { it.fecha >= desde.timeInMillis && it.fecha < hasta.timeInMillis }.sumOf { it.cantidadKg }
                )
            }
        }
    }

    /** Devuelve el mensaje de error para mostrar en el diálogo, o null si se envió. */
    suspend fun solicitarCamion(siloId: String, kg: Double, urgente: Boolean, observacion: String, usuario: String): String? =
        repository.solicitarCamion(siloId, kg, urgente, observacion.trim(), usuario).fold(
            onSuccess = {
                _mensajeOperacion.value = "Camión solicitado. Queda pendiente de aprobación por Jefatura."
                null
            },
            onFailure = { it.message ?: "No se pudo solicitar el camión" }
        )

    fun revisarSolicitud(id: Long, aprobada: Boolean, usuario: String) {
        viewModelScope.launch {
            repository.revisarSolicitud(id, aprobada, usuario)
                .onSuccess { _mensajeOperacion.value = if (aprobada) "Reposición aprobada" else "Solicitud rechazada" }
                .onFailure { _mensajeOperacion.value = it.message }
        }
    }

    /** Últimos movimientos del silo, del más reciente al más antiguo. */
    fun historial(siloId: String): Flow<List<MovimientoUi>> =
        repository.historialFlow(siloId).map { lista ->
            lista.map { m ->
                MovimientoUi(
                    id = m.id,
                    tipo = if (m.tipo == "CARGA") TipoMovimiento.CARGA else TipoMovimiento.CONSUMO,
                    cantidadKg = m.cantidadKg,
                    fecha = m.fecha,
                    observacion = m.observacion,
                    fotoUri = m.fotoUri,
                    pendienteSincronizar = m.pendienteSincronizar
                )
            }
        }

    /** Indica si el código leído (QR o manual) corresponde a un silo registrado. */
    suspend fun existeSilo(codigo: String): Boolean {
        // Si el código no existe, deja un mensaje para el Toast.
        val existe = repository.obtenerSiloPorId(codigo) != null
        if (!existe) _mensajeOperacion.value = "Código inválido o silo no registrado ($codigo)"
        return existe
    }

    /** Datos actuales del silo para precargar el formulario de edición. */
    suspend fun obtenerDatosSilo(id: String): DatosSilo? =
        repository.obtenerSiloPorId(id)?.let { e ->
            DatosSilo(
                codigo = e.id,
                nombre = e.nombre,
                granja = e.granja,
                galpon = e.galpon,
                tipoAlimento = e.tipoAlimento,
                capacidadMaxKg = e.capacidadMaxima,
                stockActualKg = e.nivelActual,
                consumoPromedioDiarioKg = e.consumoPromedioDiario,
                latitud = e.latitud,
                longitud = e.longitud
            )
        }

    /**
     * Valida y guarda un silo nuevo o editado.
     * Devuelve el mensaje de error para mostrarlo en el formulario, o null si se guardó.
     */
    suspend fun guardarSilo(datos: DatosSilo, esNuevo: Boolean): String? {
        // 1) Normaliza el código y valida los datos con las reglas de negocio.
        val codigo = datos.codigo.trim().uppercase()
        ReglasTerreno.validarSilo(
            codigo, datos.nombre, datos.granja, datos.capacidadMaxKg,
            datos.stockActualKg, datos.consumoPromedioDiarioKg, datos.latitud, datos.longitud
        )?.let { return it }

        // 2) Convierte los datos del formulario en una entidad de Room.
        val entidad = SiloEntity(
            id = codigo,
            nombre = datos.nombre.trim(),
            granja = datos.granja.trim(),
            galpon = datos.galpon.trim(),
            tipoAlimento = datos.tipoAlimento.trim(),
            capacidadMaxima = datos.capacidadMaxKg,
            nivelActual = datos.stockActualKg,
            consumoPromedioDiario = datos.consumoPromedioDiarioKg,
            latitud = datos.latitud,
            longitud = datos.longitud
        )
        // 3) Crea o edita según corresponda y devuelve el error (o null si salió bien).
        val resultado = if (esNuevo) repository.crearSilo(entidad) else repository.editarSilo(entidad)
        return resultado.fold(
            onSuccess = {
                _mensajeOperacion.value = if (esNuevo) "Silo $codigo creado" else "Silo $codigo actualizado"
                null
            },
            onFailure = { error -> error.message ?: "No se pudo guardar el silo" }
        )
    }

    /**
     * Aplica las reglas de terreno (sobrellenado y saldo negativo) y guarda el movimiento.
     * Devuelve el mensaje de error para mostrarlo en el formulario, o null si se registró.
     */
    suspend fun registrarMovimiento(
        idSilo: String,
        tipo: TipoMovimiento,
        cantidadKg: Double,
        observacion: String = "",
        fotoUri: String? = null
    ): String? {
        // 1) El repositorio aplica las reglas (sobrellenado / saldo negativo) y guarda.
        val esCarga = tipo == TipoMovimiento.CARGA
        val resultado = repository.registrarMovimiento(idSilo, cantidadKg, esCarga, observacion, fotoUri)
        return resultado.fold(
            onSuccess = { siloActualizado ->
                _mensajeOperacion.value = if (esCarga) "Carga registrada con éxito" else "Consumo registrado con éxito"
                // 2) Si el silo quedó bajo el 20%, se dispara la notificación de nivel crítico.
                val porcentaje = (siloActualizado.nivelActual / siloActualizado.capacidadMaxima * 100).toFloat()
                if (EstadoSilo.desdePorcentaje(porcentaje) == EstadoSilo.CRITICO) {
                    NotificadorAlertas.notificarNivelCritico(getApplication(), siloActualizado)
                }
                null
            },
            onFailure = { error -> error.message ?: "No se pudo registrar el movimiento" }
        )
    }

    // Se llama después de mostrar el Toast para no repetirlo.
    fun limpiarMensaje() {
        _mensajeOperacion.value = null
    }

    // Factory: le dice a Android cómo crear este ViewModel (necesita el Application).
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SiloViewModel::class.java)) {
                return SiloViewModel(application) as T
            }
            throw IllegalArgumentException("Clase ViewModel desconocida")
        }
    }
}