package com.example.silomonitorapp.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// SERVICIO RETROFIT del microservicio ms-silos.
// Cada función es un endpoint: Retrofit genera el código HTTP a partir de las anotaciones.
// 'suspend' = se llama desde una corrutina y no bloquea la pantalla.
interface SiloApiService {

    @GET("api/silos")
    suspend fun listarSilos(): List<SiloDto>

    @GET("api/silos/{id}")
    suspend fun obtenerSilo(@Path("id") id: String): SiloDto

    @POST("api/silos")
    suspend fun crearSilo(@Body silo: SiloDto): SiloDto

    @PUT("api/silos/{id}")
    suspend fun editarSilo(@Path("id") id: String, @Body silo: SiloDto): SiloDto

    // DELETE responde 204 sin cuerpo: se usa Response<Unit> para leer solo el código HTTP.
    @DELETE("api/silos/{id}")
    suspend fun eliminarSilo(@Path("id") id: String): Response<Unit>

    @GET("api/silos/{id}/movimientos")
    suspend fun listarMovimientos(@Path("id") siloId: String): List<MovimientoDto>

    // Devuelve el silo con el stock ya actualizado por el servidor.
    @POST("api/silos/{id}/movimientos")
    suspend fun registrarMovimiento(@Path("id") siloId: String, @Body movimiento: MovimientoRequestDto): SiloDto
}