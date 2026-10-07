package com.example.silomonitorapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.silomonitorapp.data.local.AppDatabase
import com.example.silomonitorapp.data.local.SiloEntity
import com.example.silomonitorapp.data.local.SiloRepository
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

class SiloViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SiloRepository

    // Mapeo reactivo directo de Room a SiloUi
    val silosUi: StateFlow<List<SiloUi>>

    private val _mensajeOperacion = MutableStateFlow<String?>(null)
    val mensajeOperacion: StateFlow<String?> = _mensajeOperacion.asStateFlow()

    init {
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

    /** Bandeja de solicitudes de camión, con el nombre y la granja de cada silo. */
    val solicitudesUi: StateFlow<List<SolicitudUi>> =
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
        val inicio = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -(dias - 1))
        }
        val formato = SimpleDateFormat("EEE d", Locale.forLanguageTag("es-CL"))
        return repository.movimientosDesdeFlow(inicio.timeInMillis).map { movimientos ->
            val consumos = movimientos.filter { it.tipo == "CONSUMO" }
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
        val codigo = datos.codigo.trim().uppercase()
        ReglasTerreno.validarSilo(
            codigo, datos.nombre, datos.granja, datos.capacidadMaxKg,
            datos.stockActualKg, datos.consumoPromedioDiarioKg, datos.latitud, datos.longitud
        )?.let { return it }

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
        val esCarga = tipo == TipoMovimiento.CARGA
        val resultado = repository.registrarMovimiento(idSilo, cantidadKg, esCarga, observacion, fotoUri)
        return resultado.fold(
            onSuccess = { siloActualizado ->
                _mensajeOperacion.value = if (esCarga) "Carga registrada con éxito" else "Consumo registrado con éxito"
                val porcentaje = (siloActualizado.nivelActual / siloActualizado.capacidadMaxima * 100).toFloat()
                if (EstadoSilo.desdePorcentaje(porcentaje) == EstadoSilo.CRITICO) {
                    NotificadorAlertas.notificarNivelCritico(getApplication(), siloActualizado)
                }
                null
            },
            onFailure = { error -> error.message ?: "No se pudo registrar el movimiento" }
        )
    }

    fun limpiarMensaje() {
        _mensajeOperacion.value = null
    }

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
