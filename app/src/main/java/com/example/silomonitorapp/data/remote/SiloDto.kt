package com.example.silomonitorapp.data.remote

import com.example.silomonitorapp.data.local.SiloEntity

// DTOs (Data Transfer Objects): la forma EXACTA del JSON que manda y recibe el microservicio ms-silos.
// Se separan de las entidades de Room para que un cambio en el backend no rompa la base local.
// Los campos que el servidor puede mandar en null se declaran con '?'.

/** Silo tal como lo devuelve/recibe GET, POST y PUT /api/silos. */
data class SiloDto(
    val id: String,
    val nombre: String,
    val granja: String,
    val galpon: String?,
    val tipoAlimento: String?,
    val capacidadMaxima: Double,
    val nivelActual: Double,
    val consumoPromedioDiario: Double?,
    val latitud: Double?,
    val longitud: Double?
)

/** Body de POST /api/silos/{id}/movimientos. */
data class MovimientoRequestDto(
    val tipo: String, // "CARGA" o "CONSUMO"
    val cantidadKg: Double,
    val observacion: String,
    val fecha: Long // se manda la fecha real (sirve para movimientos hechos sin internet)
)

/** Movimiento tal como lo devuelve GET /api/silos/{id}/movimientos. */
data class MovimientoDto(
    val id: Long,
    val siloId: String,
    val tipo: String,
    val cantidadKg: Double,
    val fecha: Long,
    val observacion: String?
)

/** Cuerpo de error que manda el GlobalExceptionHandler: { "error": "mensaje" }. */
data class ErrorDto(val error: String?)

// MAPPERS: convierten entre el DTO (red) y la entidad (Room).

fun SiloDto.aEntidad() = SiloEntity(
    id = id,
    nombre = nombre,
    granja = granja,
    galpon = galpon.orEmpty(),
    tipoAlimento = tipoAlimento.orEmpty(),
    capacidadMaxima = capacidadMaxima,
    nivelActual = nivelActual,
    consumoPromedioDiario = consumoPromedioDiario ?: 0.0,
    latitud = latitud ?: 0.0,
    longitud = longitud ?: 0.0
)

fun SiloEntity.aDto() = SiloDto(
    id = id,
    nombre = nombre,
    granja = granja,
    galpon = galpon,
    tipoAlimento = tipoAlimento,
    capacidadMaxima = capacidadMaxima,
    nivelActual = nivelActual,
    consumoPromedioDiario = consumoPromedioDiario,
    latitud = latitud,
    longitud = longitud
)