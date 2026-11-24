package com.example.silomonitorapp.data.remote

import com.example.silomonitorapp.data.local.SolicitudCamionEntity
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

// SERVICIO RETROFIT del microservicio propio ms-solicitudes (solicitudes de camión de reposición).
// 'suspend' = se llama desde una corrutina y no bloquea la pantalla.
interface SolicitudApiService {

    // Bandeja completa: pendientes primero (urgentes arriba) y luego las revisadas.
    @GET("api/solicitudes")
    suspend fun listar(): List<SolicitudDto>

    // El Supervisor pide un camión. Responde 409 si el silo ya tiene uno pendiente.
    @POST("api/solicitudes")
    suspend fun crear(@Body solicitud: SolicitudRequestDto): SolicitudDto

    // La Jefatura aprueba o rechaza. Responde 409 si ya había sido revisada.
    @PATCH("api/solicitudes/{id}/revision")
    suspend fun revisar(@Path("id") id: Long, @Body revision: RevisionRequestDto): SolicitudDto
}

// DTOs: forma exacta del JSON que manda y recibe ms-solicitudes.

/** Solicitud tal como la devuelve el microservicio. */
data class SolicitudDto(
    val id: Long,
    val siloId: String,
    val kgSolicitados: Double,
    val urgente: Boolean,
    val observacion: String?,
    val solicitadoPor: String,
    val fechaSolicitud: Long,
    val estado: String, // PENDIENTE, APROBADA o RECHAZADA
    val revisadoPor: String?,
    val fechaRevision: Long?
)

/** Body de POST /api/solicitudes (se manda la fecha real por si se pidió sin internet). */
data class SolicitudRequestDto(
    val siloId: String,
    val kgSolicitados: Double,
    val urgente: Boolean,
    val observacion: String,
    val solicitadoPor: String,
    val fechaSolicitud: Long
)

/** Body de PATCH /api/solicitudes/{id}/revision. */
data class RevisionRequestDto(
    val aprobada: Boolean,
    val revisadoPor: String,
    val fechaRevision: Long?
)

// MAPPERS: convierten entre el DTO (red) y la entidad (Room).

// Lo que viene del servidor ya está sincronizado.
fun SolicitudDto.aEntidad() = SolicitudCamionEntity(
    id = id,
    siloId = siloId,
    kgSolicitados = kgSolicitados,
    urgente = urgente,
    observacion = observacion.orEmpty(),
    solicitadoPor = solicitadoPor,
    fechaSolicitud = fechaSolicitud,
    estado = estado,
    revisadoPor = revisadoPor,
    fechaRevision = fechaRevision,
    pendienteSincronizar = false
)

fun SolicitudCamionEntity.aRequest() = SolicitudRequestDto(
    siloId = siloId,
    kgSolicitados = kgSolicitados,
    urgente = urgente,
    observacion = observacion,
    solicitadoPor = solicitadoPor,
    fechaSolicitud = fechaSolicitud
)