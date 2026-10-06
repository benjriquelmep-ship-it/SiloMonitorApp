package com.example.silomonitorapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.silomonitorapp.data.local.AppDatabase
import com.example.silomonitorapp.data.local.SiloRepository
import com.example.silomonitorapp.ui.model.SiloUi
import com.example.silomonitorapp.ui.model.TipoMovimiento
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SiloViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SiloRepository

    // Mapeo reactivo directo de Room a SiloUi
    val silosUi: StateFlow<List<SiloUi>>

    private val _mensajeOperacion = MutableStateFlow<String?>(null)
    val mensajeOperacion: StateFlow<String?> = _mensajeOperacion.asStateFlow()

    init {
        val db = AppDatabase.obtenerBaseDatos(application)
        repository = SiloRepository(db.siloDao())

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

    /** Indica si el código leído (QR o manual) corresponde a un silo registrado. */
    suspend fun existeSilo(codigo: String): Boolean {
        val existe = repository.obtenerSiloPorId(codigo) != null
        if (!existe) _mensajeOperacion.value = "Código inválido o silo no registrado ($codigo)"
        return existe
    }

    /**
     * Aplica las reglas de terreno (sobrellenado y saldo negativo) y guarda el movimiento.
     * Devuelve el mensaje de error para mostrarlo en el formulario, o null si se registró.
     */
    suspend fun registrarMovimiento(idSilo: String, tipo: TipoMovimiento, cantidadKg: Double): String? {
        val esCarga = tipo == TipoMovimiento.CARGA
        val resultado = repository.registrarMovimiento(idSilo, cantidadKg, esCarga)
        return resultado.fold(
            onSuccess = {
                _mensajeOperacion.value = if (esCarga) "Carga registrada con éxito" else "Consumo registrado con éxito"
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
