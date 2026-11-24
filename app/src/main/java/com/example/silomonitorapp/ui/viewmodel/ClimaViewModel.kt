package com.example.silomonitorapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.silomonitorapp.data.remote.ClimaRepository
import com.example.silomonitorapp.data.remote.ClimaSilo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estados posibles de la tarjeta de clima. */
sealed interface EstadoClima {
    data object Cargando : EstadoClima
    data class Exito(val clima: ClimaSilo) : EstadoClima
    data class Error(val mensaje: String) : EstadoClima
}

// VIEWMODEL de la API externa (clima del silo).
// Separado de SiloViewModel porque no depende de Room ni de los microservicios propios.
// El repositorio tiene valor por defecto: Android puede crear este ViewModel con viewModel()
// y las pruebas le pasan un mock.
class ClimaViewModel(
    private val repository: ClimaRepository = ClimaRepository()
) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoClima>(EstadoClima.Cargando)
    val estado: StateFlow<EstadoClima> = _estado.asStateFlow()

    // Coordenadas de la última consulta: evita repetir la llamada al volver a la pantalla.
    private var ultimaUbicacion: Pair<Double, Double>? = null

    /** Consulta el clima del silo. Con [forzar] = true vuelve a llamar a la API (botón reintentar). */
    fun cargar(latitud: Double, longitud: Double, forzar: Boolean = false) {
        val ubicacion = latitud to longitud
        if (!forzar && ubicacion == ultimaUbicacion && _estado.value is EstadoClima.Exito) return
        ultimaUbicacion = ubicacion
        _estado.value = EstadoClima.Cargando
        viewModelScope.launch {
            _estado.value = repository.obtenerClima(latitud, longitud).fold(
                onSuccess = { EstadoClima.Exito(it) },
                onFailure = { EstadoClima.Error(it.message ?: "No se pudo consultar el clima.") }
            )
        }
    }
}