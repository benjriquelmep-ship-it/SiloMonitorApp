package com.example.silomonitorapp.data.remote

import com.example.silomonitorapp.BuildConfig
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Configuración única de Retrofit (patrón Singleton con 'object').
// La URL base sale de BuildConfig.API_SILOS_URL (se define en build.gradle.kts / local.properties):
//   - Emulador:          http://10.0.2.2:8080/   (10.0.2.2 = el "localhost" del PC visto desde el emulador)
//   - Celular físico:    http://IP_DEL_PC:8080/  (mismo Wi-Fi)
object RetrofitClient {

    // Muestra en el Logcat cada petición y respuesta (útil para la defensa: filtrar por "okhttp").
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Si el microservicio no responde en 10 s se lanza IOException y la app trabaja sin conexión.
    private val cliente = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // GsonConverterFactory convierte JSON <-> data classes de Kotlin automáticamente.
    private val retrofitSilos: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_SILOS_URL)
        .client(cliente)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // 'by lazy': se crea la primera vez que se usa.
    val siloApi: SiloApiService by lazy { retrofitSilos.create(SiloApiService::class.java) }
}