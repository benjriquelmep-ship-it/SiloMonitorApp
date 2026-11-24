package com.example.silomonitorapp.data.remote

import com.example.silomonitorapp.BuildConfig
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Configuración única de Retrofit (patrón Singleton con 'object').
// Hay una instancia de Retrofit por cada servidor (cada una con su URL base):
//   - ms-silos        (microservicio propio)  -> BuildConfig.API_SILOS_URL
//   - Open-Meteo      (API externa pública)   -> https://api.open-meteo.com/
// URLs de los microservicios (se definen en build.gradle.kts / local.properties):
//   - Emulador:          http://10.0.2.2:PUERTO/   (10.0.2.2 = el "localhost" del PC visto desde el emulador)
//   - Celular físico:    http://IP_DEL_PC:PUERTO/  (mismo Wi-Fi)
object RetrofitClient {

    // URL base de la API externa de clima (pública, sin API key).
    const val URL_OPEN_METEO = "https://api.open-meteo.com/"

    // Muestra en el Logcat cada petición y respuesta (útil para la defensa: filtrar por "okhttp").
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Si el servidor no responde en 10 s se lanza IOException y la app trabaja sin conexión.
    private val cliente = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Crea una instancia de Retrofit para una URL base.
    // GsonConverterFactory convierte JSON <-> data classes de Kotlin automáticamente.
    private fun crearRetrofit(urlBase: String): Retrofit = Retrofit.Builder()
        .baseUrl(urlBase)
        .client(cliente)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // 'by lazy': cada servicio se crea la primera vez que se usa.
    val siloApi: SiloApiService by lazy { crearRetrofit(BuildConfig.API_SILOS_URL).create(SiloApiService::class.java) }

    val climaApi: ClimaApiService by lazy { crearRetrofit(URL_OPEN_METEO).create(ClimaApiService::class.java) }
}