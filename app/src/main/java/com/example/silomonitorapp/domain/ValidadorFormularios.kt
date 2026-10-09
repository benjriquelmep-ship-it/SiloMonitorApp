package com.example.silomonitorapp.domain

/**
 * Validador desacoplado de formularios de la aplicación.
 * Evalúa reglas de entrada sin acoplamiento a componentes de interfaz de usuario (IE 2.2.1).
 */
object ValidadorFormularios {

    fun validarUsuario(usuario: String): String? {
        return if (usuario.isBlank()) "El usuario es obligatorio" else null
    }

    fun validarPassword(password: String): String? {
        return if (password.isBlank()) "La contraseña es obligatoria" else null
    }

    fun validarCodigoSilo(codigo: String): String? {
        return if (codigo.isBlank()) "El código de silo es obligatorio" else null
    }

    fun validarCapacidad(capacidadTexto: String): String? {
        val capacidad = capacidadTexto.toDoubleOrNull()
        return when {
            capacidadTexto.isBlank() -> "La capacidad es obligatoria"
            capacidad == null -> "Debe ser un número válido"
            capacidad <= 0.0 -> "La capacidad debe ser mayor a 0 kg"
            else -> null
        }
    }

    fun validarCoordenada(valorTexto: String, esLatitud: Boolean): String? {
        val coord = valorTexto.toDoubleOrNull() ?: return "Coordenada numérica inválida"
        return if (esLatitud && (coord < -90.0 || coord > 90.0)) {
            "Latitud fuera de rango (-90 a 90)"
        } else if (!esLatitud && (coord < -180.0 || coord > 180.0)) {
            "Longitud fuera de rango (-180 a 180)"
        } else {
            null
        }
    }

    fun validarKilos(kilosTexto: String, capacidadMaxima: Double? = null): String? {
        val kilos = kilosTexto.toDoubleOrNull()
        return when {
            kilosTexto.isBlank() -> "Ingrese la cantidad de kilos"
            kilos == null -> "Formato numérico inválido"
            kilos <= 0.0 -> "La cantidad debe ser superior a 0 kg"
            capacidadMaxima != null && kilos > capacidadMaxima -> "Supera la capacidad total del silo ($capacidadMaxima kg)"
            else -> null
        }
    }
}