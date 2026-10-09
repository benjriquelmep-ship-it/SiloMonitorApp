package com.example.silomonitorapp.domain

/**
 * Validador desacoplado de formularios de la aplicación.
 * Evalúa reglas de entrada sin acoplamiento a componentes de interfaz de usuario (IE 2.2.1).
 * Cada función devuelve el mensaje de error del campo, o null si el valor es válido.
 */
object ValidadorFormularios {

    /** Convierte texto a número aceptando coma o punto decimal (ej: "2500,5" o "2500.5"). */
    fun aNumero(texto: String): Double? = texto.trim().replace(",", ".").toDoubleOrNull()

    fun validarUsuario(usuario: String): String? {
        return if (usuario.isBlank()) "El usuario es obligatorio" else null
    }

    fun validarPassword(password: String): String? {
        return if (password.isBlank()) "La contraseña es obligatoria" else null
    }

    fun validarCodigoSilo(codigo: String): String? {
        return when {
            codigo.isBlank() -> "El código de silo es obligatorio"
            // Sin espacios ni símbolos: el mismo código va impreso en el QR del silo
            !Regex("^[A-Za-z0-9-]{1,20}$").matches(codigo.trim()) -> "Solo letras, números y guiones (máx. 20)"
            else -> null
        }
    }

    /** Para campos de texto obligatorios como la granja; [mensajeError] se muestra bajo el campo. */
    fun validarObligatorio(valor: String, mensajeError: String): String? {
        return if (valor.isBlank()) mensajeError else null
    }

    fun validarCapacidad(capacidadTexto: String): String? {
        val capacidad = aNumero(capacidadTexto)
        return when {
            capacidadTexto.isBlank() -> "La capacidad es obligatoria"
            capacidad == null -> "Debe ser un número válido"
            capacidad <= 0.0 -> "La capacidad debe ser mayor a 0 kg"
            else -> null
        }
    }

    /** El stock no puede ser negativo ni superar la capacidad máxima del silo. */
    fun validarStock(stockTexto: String, capacidadTexto: String): String? {
        val stock = aNumero(stockTexto)
        val capacidad = aNumero(capacidadTexto)
        return when {
            stockTexto.isBlank() -> "Ingrese el stock inicial"
            stock == null -> "Debe ser un número válido"
            stock < 0.0 -> "El stock no puede ser negativo"
            capacidad != null && stock > capacidad -> "No puede superar la capacidad (${capacidad.toLong()} kg)"
            else -> null
        }
    }

    fun validarConsumo(consumoTexto: String): String? {
        val consumo = aNumero(consumoTexto)
        return when {
            consumoTexto.isBlank() -> "El consumo diario es obligatorio"
            consumo == null -> "Debe ser un número válido"
            consumo < 0.0 -> "El consumo no puede ser negativo"
            else -> null
        }
    }

    fun validarCoordenada(valorTexto: String, esLatitud: Boolean): String? {
        val coord = aNumero(valorTexto) ?: return "Coordenada numérica inválida"
        return if (esLatitud && (coord < -90.0 || coord > 90.0)) {
            "Latitud fuera de rango (-90 a 90)"
        } else if (!esLatitud && (coord < -180.0 || coord > 180.0)) {
            "Longitud fuera de rango (-180 a 180)"
        } else {
            null
        }
    }

    fun validarKilos(kilosTexto: String, capacidadMaxima: Double? = null): String? {
        val kilos = aNumero(kilosTexto)
        return when {
            kilosTexto.isBlank() -> "Ingrese la cantidad de kilos"
            kilos == null -> "Formato numérico inválido"
            kilos <= 0.0 -> "La cantidad debe ser superior a 0 kg"
            capacidadMaxima != null && kilos > capacidadMaxima -> "Supera la capacidad disponible (${capacidadMaxima.toLong()} kg)"
            else -> null
        }
    }
}
