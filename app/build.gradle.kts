import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    // Lee app/google-services.json para que Firebase (Firestore) se inicialice al abrir la app
    id("com.google.gms.google-services")
    id("de.mannodermaus.android-junit") version "2.0.2"
    // JaCoCo: mide qué porcentaje del código ejecutan las pruebas unitarias (cobertura)
    jacoco
}

// La API key de Google Maps se lee desde local.properties (MAPS_API_KEY=...), nunca se sube al repo
val localProperties = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

android {
    namespace = "com.example.silomonitorapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.silomonitorapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY", "")

        // URL del microservicio ms-silos. Por defecto 10.0.2.2 (el PC visto desde el emulador).
        // Con celular físico: agregar API_SILOS_URL=http://IP_DEL_PC:8080/ en local.properties
        buildConfigField(
            "String",
            "API_SILOS_URL",
            "\"${localProperties.getProperty("API_SILOS_URL", "http://10.0.2.2:8080/")}\""
        )
        // URL del microservicio ms-solicitudes (puerto 8081). Celular físico: API_SOLICITUDES_URL en local.properties
        buildConfigField(
            "String",
            "API_SOLICITUDES_URL",
            "\"${localProperties.getProperty("API_SOLICITUDES_URL", "http://10.0.2.2:8081/")}\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Genera el reporte de cobertura de las pruebas unitarias (tarea createDebugUnitTestCoverageReport)
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        // Genera la clase BuildConfig (con API_SILOS_URL y API_SOLICITUDES_URL)
        buildConfig = true
    }
    // Versión de JaCoCo compatible con Java 25 (el JDK que usa Gradle en este proyecto)
    testCoverage {
        jacocoVersion = "0.8.14"
    }
}

// ---------------------------------------------------------------------------
// COBERTURA DE PRUEBAS UNITARIAS (JaCoCo)
//   ./gradlew reporteCobertura    -> reporte HTML en app/build/reports/cobertura/html/index.html
//   ./gradlew verificarCobertura  -> falla si la cobertura de líneas de la LÓGICA es menor a 80 %
// Se excluye lo que no es lógica de negocio: código generado (R, BuildConfig, Room *_Impl),
// pantallas y componentes de Compose (se prueban con androidTest) y clases que solo
// envuelven APIs de Android (Activity, notificaciones, PDF/CSV, Firestore, base de datos).
// ---------------------------------------------------------------------------
jacoco {
    toolVersion = "0.8.14"
}

val exclusionesCobertura = listOf(
    // Código generado
    "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*", "**/*_Impl*.*",
    // Interfaz Compose (pantallas, componentes, navegación y tema)
    "**/ui/screens/**", "**/ui/components/**", "**/ui/navigation/**", "**/ui/theme/**",
    "**/*ComposableSingletons*.*", "**/ui/model/SilosDemo*.*",
    // Envoltorios de Android / servicios externos sin lógica propia
    "**/MainActivity*.*", "**/SiloApp*.*", "**/data/local/AppDatabase*.*",
    "**/data/remote/SyncRepository*.*", "**/data/remote/RetrofitClient*.*",
    "**/notificaciones/**", "**/data/reports/**",
)

// Clases compiladas de la variante debug (Kotlin y Java), sin las exclusiones.
val clasesCobertura = fileTree(layout.buildDirectory) {
    include(
        "intermediates/built_in_kotlinc/debug/**/classes/**/*.class",
        "tmp/kotlin-classes/debug/**/*.class",
        "intermediates/javac/debug/**/classes/**/*.class",
    )
    exclude(exclusionesCobertura)
}

// Archivos .exec que genera la tarea testDebugUnitTest con la cobertura registrada.
val datosCobertura = fileTree(layout.buildDirectory) {
    include("outputs/unit_test_code_coverage/debugUnitTest/*.exec", "jacoco/testDebugUnitTest.exec")
}

tasks.register<JacocoReport>("reporteCobertura") {
    group = "verification"
    description = "Ejecuta las pruebas unitarias y genera el reporte de cobertura (HTML y XML)."
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(clasesCobertura)
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(datosCobertura)
    reports {
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/cobertura/html"))
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/cobertura/cobertura.xml"))
    }
}

tasks.register<JacocoCoverageVerification>("verificarCobertura") {
    group = "verification"
    description = "Falla si la cobertura de líneas de la lógica es menor a 80 %."
    dependsOn("reporteCobertura")
    classDirectories.setFrom(clasesCobertura)
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(datosCobertura)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Firebase BoM y Firestore
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Navegación en Jetpack Compose e Íconos
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.compose.material:material-icons-extended:1.7.0")

    // Persistencia Local: Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // CameraX (Cámara para escaneo QR)
    val cameraxVersion = "1.6.2"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // Google ML Kit Barcode Scanning (Lector de QR)
    // Versión que usa el modelo de Google Play Services: no incluye la librería nativa
    // libbarhopper en el APK (compatible con páginas de 16 KB y APK más liviano)
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.1")

    // Librería nativa alineada a 16 KB (la versión que traía Compose no lo estaba)
    implementation("androidx.graphics:graphics-path:1.1.0")

    // Google Maps + GPS
    implementation("com.google.maps.android:maps-compose:6.4.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Retrofit: consumo del microservicio ms-silos (JSON con Gson) + log de peticiones
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    //TEST
    // --- Pruebas Unitarias (JVM: carpeta test) ---
    testImplementation(libs.junit) // Compatibilidad con tests JUnit 4 existentes
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")

    // Kotest (aserciones expresivas con shouldBe)
    testImplementation("io.kotest:kotest-assertions-core:6.2.7")

    // MockK y Coroutines Test
    testImplementation("io.mockk:mockk:1.14.12")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")

    // --- Pruebas Instrumentadas / Compose UI Test (JUnit 4: carpeta androidTest) ---
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}