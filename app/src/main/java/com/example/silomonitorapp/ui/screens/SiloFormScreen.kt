package com.example.silomonitorapp.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.silomonitorapp.domain.ValidadorFormularios
import com.example.silomonitorapp.ui.model.DatosSilo
import com.google.android.gms.location.LocationServices
import java.util.Locale

/**
 * Formulario de alta y edición de Silo con validación por campo individual
 * (ícono de advertencia + texto de soporte). Las reglas viven en [ValidadorFormularios].
 * Al editar, el código y el stock no se modifican: el stock solo cambia con movimientos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiloFormScreen(
    datosIniciales: DatosSilo?,
    esNuevo: Boolean,
    error: String?,
    onVolver: () -> Unit,
    onGuardar: (DatosSilo) -> Unit,
) {
    val context = LocalContext.current

    // rememberSaveable: los datos escritos se conservan al girar el teléfono
    var codigo by rememberSaveable { mutableStateOf("") }
    var nombre by rememberSaveable { mutableStateOf("") }
    var granja by rememberSaveable { mutableStateOf("") }
    var galpon by rememberSaveable { mutableStateOf("") }
    var tipoAlimento by rememberSaveable { mutableStateOf("") }
    var capacidadMax by rememberSaveable { mutableStateOf("") }
    var stockActual by rememberSaveable { mutableStateOf("") }
    var consumoPromedio by rememberSaveable { mutableStateOf("500") }
    var latitud by rememberSaveable { mutableStateOf("-33.6850") }
    var longitud by rememberSaveable { mutableStateOf("-71.2150") }
    var precargado by rememberSaveable { mutableStateOf(false) }

    var errorCodigo by rememberSaveable { mutableStateOf<String?>(null) }
    var errorGranja by rememberSaveable { mutableStateOf<String?>(null) }
    var errorCapacidad by rememberSaveable { mutableStateOf<String?>(null) }
    var errorStockActual by rememberSaveable { mutableStateOf<String?>(null) }
    var errorConsumo by rememberSaveable { mutableStateOf<String?>(null) }
    var errorLatitud by rememberSaveable { mutableStateOf<String?>(null) }
    var errorLongitud by rememberSaveable { mutableStateOf<String?>(null) }

    var obteniendoUbicacion by rememberSaveable { mutableStateOf(false) }
    var avisoUbicacion by rememberSaveable { mutableStateOf<String?>(null) }

    // Al editar, precarga una sola vez los datos actuales del silo
    LaunchedEffect(datosIniciales) {
        if (datosIniciales != null && !precargado) {
            codigo = datosIniciales.codigo
            nombre = datosIniciales.nombre
            granja = datosIniciales.granja
            galpon = datosIniciales.galpon
            tipoAlimento = datosIniciales.tipoAlimento
            capacidadMax = sinDecimales(datosIniciales.capacidadMaxKg)
            stockActual = sinDecimales(datosIniciales.stockActualKg)
            consumoPromedio = sinDecimales(datosIniciales.consumoPromedioDiarioKg)
            latitud = datosIniciales.latitud.toString()
            longitud = datosIniciales.longitud.toString()
            precargado = true
        }
    }

    fun validarFormulario(): Boolean {
        errorCodigo = ValidadorFormularios.validarCodigoSilo(codigo)
        errorGranja = ValidadorFormularios.validarObligatorio(granja, "La granja")
        errorCapacidad = ValidadorFormularios.validarCapacidad(capacidadMax)
        errorStockActual = ValidadorFormularios.validarStock(stockActual, capacidadMax)
        errorConsumo = ValidadorFormularios.validarConsumo(consumoPromedio)
        errorLatitud = ValidadorFormularios.validarCoordenada(latitud, esLatitud = true)
        errorLongitud = ValidadorFormularios.validarCoordenada(longitud, esLatitud = false)

        return listOf(errorCodigo, errorGranja, errorCapacidad, errorStockActual, errorConsumo, errorLatitud, errorLongitud)
            .all { it == null }
    }

    // --- GPS: completa latitud y longitud con la ubicación actual del teléfono ---
    @SuppressLint("MissingPermission")
    fun leerUbicacionActual() {
        obteniendoUbicacion = true
        avisoUbicacion = null
        LocationServices.getFusedLocationProviderClient(context).lastLocation
            .addOnSuccessListener { ubicacion ->
                obteniendoUbicacion = false
                if (ubicacion != null) {
                    latitud = "%.6f".format(Locale.US, ubicacion.latitude)
                    longitud = "%.6f".format(Locale.US, ubicacion.longitude)
                    errorLatitud = null
                    errorLongitud = null
                } else {
                    avisoUbicacion = "No se pudo obtener la ubicación. Activa el GPS e inténtalo de nuevo."
                }
            }
            .addOnFailureListener {
                obteniendoUbicacion = false
                avisoUbicacion = "No se pudo obtener la ubicación."
            }
    }

    val pedirPermisoUbicacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        if (resultado.values.any { it }) leerUbicacionActual()
        else avisoUbicacion = "Se necesita permiso de ubicación para usar el GPS."
    }

    fun usarMiUbicacion() {
        val tienePermiso = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (tienePermiso) leerUbicacionActual()
        else pedirPermisoUbicacion.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (esNuevo) "Registrar Nuevo Silo" else "Editar Silo") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SeccionFormulario("Identificación") {
                CampoValidado(
                    valor = codigo,
                    onCambio = {
                        // Mayúsculas y sin espacios: es el mismo texto que lleva el QR del silo
                        codigo = it.uppercase().filter { c -> c.isLetterOrDigit() || c == '-' }.take(20)
                        errorCodigo = ValidadorFormularios.validarCodigoSilo(codigo)
                    },
                    etiqueta = "Código de Silo (ej. SIL-007)",
                    error = errorCodigo,
                    habilitado = esNuevo,
                    mayusculas = true,
                )
                CampoValidado(valor = nombre, onCambio = { nombre = it }, etiqueta = "Nombre descriptivo")
                CampoValidado(
                    valor = granja,
                    onCambio = {
                        granja = it
                        errorGranja = ValidadorFormularios.validarObligatorio(it, "La granja")
                    },
                    etiqueta = "Granja o Sector",
                    error = errorGranja,
                )
                CampoValidado(valor = galpon, onCambio = { galpon = it }, etiqueta = "Galpón")
                CampoValidado(valor = tipoAlimento, onCambio = { tipoAlimento = it }, etiqueta = "Tipo de Alimento")
            }

            SeccionFormulario("Capacidades y Consumo") {
                CampoValidado(
                    valor = capacidadMax,
                    onCambio = {
                        capacidadMax = it
                        errorCapacidad = ValidadorFormularios.validarCapacidad(it)
                        // Al cambiar la capacidad se revisa de nuevo que el stock quepa
                        if (stockActual.isNotBlank()) {
                            errorStockActual = ValidadorFormularios.validarStock(stockActual, it)
                        }
                    },
                    etiqueta = "Capacidad Máxima (kg)",
                    error = errorCapacidad,
                    numerico = true,
                )
                CampoValidado(
                    valor = stockActual,
                    onCambio = {
                        stockActual = it
                        errorStockActual = ValidadorFormularios.validarStock(it, capacidadMax)
                    },
                    etiqueta = if (esNuevo) "Stock Actual Inicial (kg)" else "Stock Actual (cambia con movimientos)",
                    error = errorStockActual,
                    habilitado = esNuevo,
                    numerico = true,
                )
                CampoValidado(
                    valor = consumoPromedio,
                    onCambio = {
                        consumoPromedio = it
                        errorConsumo = ValidadorFormularios.validarConsumo(it)
                    },
                    etiqueta = "Consumo Promedio Diario (kg)",
                    error = errorConsumo,
                    numerico = true,
                )
            }

            SeccionFormulario("Ubicación") {
                Row(modifier = Modifier.fillMaxWidth()) {
                    CampoValidado(
                        valor = latitud,
                        onCambio = {
                            latitud = it
                            errorLatitud = ValidadorFormularios.validarCoordenada(it, esLatitud = true)
                        },
                        etiqueta = "Latitud",
                        error = errorLatitud,
                        numerico = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    CampoValidado(
                        valor = longitud,
                        onCambio = {
                            longitud = it
                            errorLongitud = ValidadorFormularios.validarCoordenada(it, esLatitud = false)
                        },
                        etiqueta = "Longitud",
                        error = errorLongitud,
                        numerico = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedButton(
                    onClick = ::usarMiUbicacion,
                    enabled = !obteniendoUbicacion,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    if (obteniendoUbicacion) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.MyLocation, contentDescription = null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(if (obteniendoUbicacion) "Obteniendo ubicación..." else "Usar mi ubicación actual")
                }
                avisoUbicacion?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            AnimatedVisibility(visible = error != null) {
                Text(
                    text = error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = {
                    if (validarFormulario()) {
                        val datos = DatosSilo(
                            codigo = codigo.trim(),
                            nombre = if (nombre.isBlank()) "Silo ${codigo.trim()}" else nombre.trim(),
                            granja = granja.trim(),
                            galpon = galpon.trim(),
                            tipoAlimento = tipoAlimento.trim(),
                            capacidadMaxKg = ValidadorFormularios.aNumero(capacidadMax) ?: 0.0,
                            stockActualKg = ValidadorFormularios.aNumero(stockActual) ?: 0.0,
                            consumoPromedioDiarioKg = ValidadorFormularios.aNumero(consumoPromedio) ?: 0.0,
                            latitud = ValidadorFormularios.aNumero(latitud) ?: 0.0,
                            longitud = ValidadorFormularios.aNumero(longitud) ?: 0.0,
                        )
                        onGuardar(datos)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (esNuevo) "Guardar Silo" else "Actualizar Silo")
            }
        }
    }
}

/** Tarjeta blanca con título para agrupar campos relacionados. */
@Composable
private fun SeccionFormulario(titulo: String, contenido: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            contenido()
        }
    }
}

/**
 * Campo de texto con validación visual estándar: borde rojo, ícono de advertencia
 * y mensaje de error debajo cuando [error] no es null.
 */
@Composable
private fun CampoValidado(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    error: String? = null,
    habilitado: Boolean = true,
    numerico: Boolean = false,
    mayusculas: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        enabled = habilitado,
        isError = error != null,
        trailingIcon = {
            if (error != null) {
                Icon(Icons.Filled.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
            }
        },
        supportingText = {
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numerico) KeyboardType.Decimal else KeyboardType.Text,
            capitalization = if (mayusculas) KeyboardCapitalization.Characters else KeyboardCapitalization.Sentences,
        ),
        singleLine = true,
        modifier = modifier,
    )
}

private fun sinDecimales(valor: Double): String =
    if (valor % 1.0 == 0.0) valor.toLong().toString() else valor.toString()
