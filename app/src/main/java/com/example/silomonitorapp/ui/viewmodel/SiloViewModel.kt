package com.example.silomonitorapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.silomonitorapp.data.local.AppDatabase
import com.example.silomonitorapp.data.local.SiloEntity
import com.example.silomonitorapp.data.local.SiloRepository
import com.example.silomonitorapp.ui.model.SiloUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SiloViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SiloRepository
    val silos: StateFlow<List<SiloEntity>>

    // Mapeo reactivo directo de Room a SiloUi
    val silosUi: StateFlow<List<SiloUi>>

    private val _mensajeOperacion = MutableStateFlow<String?>(null)
    val mensajeOperacion: StateFlow<String?> = _mensajeOperacion.asStateFlow()

    private val _siloSeleccionado = MutableStateFlow<SiloEntity?>(null)
    val siloSeleccionado: StateFlow<SiloEntity?> = _siloSeleccionado.asStateFlow()

    init {
        val db = AppDatabase.obtenerBaseDatos(application)
        repository = SiloRepository(db.siloDao())

        silos = repository.silosFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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

        // Precarga en segundo plano seguro usando Dispatchers.IO
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (repository.obtenerSiloPorId("SIL-001") == null) {
                    repository.precargarSilosSiEstaVacio()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun seleccionarSilo(silo: SiloEntity?) {
        _siloSeleccionado.value = silo
    }

    fun seleccionarSiloPorId(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val silo = repository.obtenerSiloPorId(id)
            if (silo != null) {
                _siloSeleccionado.value = silo
            } else {
                _mensajeOperacion.value = "Código inválido o silo no registrado ($id)"
            }
        }
    }

    fun registrarMovimiento(idSilo: String, cantidadKg: Double, esCarga: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val resultado = repository.registrarMovimiento(idSilo, cantidadKg, esCarga)
            resultado.onSuccess {
                _mensajeOperacion.value = if (esCarga) "Carga registrada con éxito" else "Consumo registrado con éxito"
                _siloSeleccionado.value = null
            }.onFailure { error ->
                _mensajeOperacion.value = error.message
            }
        }
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