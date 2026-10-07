package com.example.silomonitorapp.domain

/**
 * Roles de la matriz RBAC del documento técnico y lo que cada uno puede hacer.
 * Las pantallas consultan estos permisos en vez de preguntar por el rol directamente.
 */
enum class Rol(val etiqueta: String) {
    OPERARIO("Operario"),
    SUPERVISOR("Supervisor"),
    JEFATURA("Jefatura"),
    ADMIN("Administrador");

    /** Operario: total con persistencia local. Admin: registro, modificación y anulación. */
    val puedeRegistrarMovimientos: Boolean
        get() = this == OPERARIO || this == ADMIN

    /** Solo el Administrador da de alta y edita silos (gestión de maestros). */
    val puedeGestionarSilos: Boolean
        get() = this == ADMIN

    /** Supervisor solicita camión de reposición; Admin tiene control total. */
    val puedeSolicitarCamion: Boolean
        get() = this == SUPERVISOR || this == ADMIN

    /** Jefatura aprueba y planifica las reposiciones; Admin tiene control total. */
    val puedeAprobarCamion: Boolean
        get() = this == JEFATURA || this == ADMIN

    /** El Operario no ve la coordinación de reposición. */
    val puedeVerSolicitudes: Boolean
        get() = this != OPERARIO

    /** Dashboard analítico con gráficos: Jefatura y Admin. */
    val puedeVerDashboard: Boolean
        get() = this == JEFATURA || this == ADMIN

    /** Operario: el QR abre directo el formulario. El resto abre la ficha técnica. */
    val qrAbreFormulario: Boolean
        get() = this == OPERARIO
}

/**
 * Usuario autenticado.
 * [granjasPermitidas] = null significa que ve todas las granjas (Jefatura y Admin).
 */
data class Usuario(
    val usuario: String,
    val nombre: String,
    val rol: Rol,
    val granjasPermitidas: Set<String>? = null,
) {
    fun puedeVerGranja(granja: String): Boolean =
        granjasPermitidas == null || granja in granjasPermitidas

    /** Texto corto para mostrar el alcance del usuario en pantalla. */
    val alcance: String
        get() = granjasPermitidas?.sorted()?.joinToString(", ") ?: "Todas las granjas"
}
