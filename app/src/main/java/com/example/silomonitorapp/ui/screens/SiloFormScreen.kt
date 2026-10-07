package com.example.silomonitorapp.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.silomonitorapp.ui.components.AriztiaTopBar
import com.example.silomonitorapp.ui.model.DatosSilo
import com.example.silomonitorapp.ui.theme.SiloMonitorAppTheme
import com.google.android.gms.location.LocationServices

/**
 * Formulario de alta y edición manual de silos.
 * Al editar, el código y el stock no se modifican: el stock solo cambia con movimientos.
 */
@Composable
fun SiloFormScreen(
    datosIniciales: DatosSilo?,
    esNuevo: Boolean,
    error: String?,
    onVolver: () -> Unit,
    onGuardar: (DatosSilo) -> Unit,
) {
    val context = LocalContext.current
    var codigo by rememberSaveable { mutableStateOf("") }
    var nombre by rememberSaveable { mutableStateOf("") }
    var granja by rememberSaveable { mutableStateOf("") }
    var galpon by rememberSaveable { mutableStateOf("") }
    var tipoAlimento by rememberSaveable { mutableStateOf("") }
    var capacidad by rememberSaveable { mutableStateOf("") }
    var stock by rememberSaveable { mutableStateOf("") }
    var consumo by rememberSaveable { mutableStateOf("") }
    var latitud by rememberSaveable { mutableStateOf("") }
    var longitud by rememberSaveable { mutableStateOf("") }
    var avisoUbicacion by rememberSaveable { mutableStateOf<String?>(null) }
    var precargado by rememberSaveable { mutableStateOf(false) }

    // Al editar, precarga una sola vez los datos actuales del silo
    LaunchedEffect(datosIniciales) {
        if (datosIniciales != null && !precargado) {
            codigo = datosIniciales.codigo
            nombre = datosIniciales.nombre
            granja = datosIniciales.granja
            galpon = datosIniciales.galpon
            tipoAlimento = datosIniciales.tipoAlimento
            capacidad = sinDecimales(datosIniciales.capacidadMaxKg)
            stock = sinDecimales(datosIniciales.stockActualKg)
            consumo = sinDecimales(datosIniciales.consumoPromedioDiarioKg)
            latitud = datosIniciales.latitud.toString()
            longitud = datosIniciales.longitud.toString()
            precargado = true
        }
    }

    @SuppressLint("MissingPermission")
    fun leerUbicacionActual() {
        avisoUbicacion = "Obteniendo ubicación GPS..."
        LocationServices.getFusedLocationProviderClient(context).lastLocation
            .addOnSuccessListener { ubicacion ->
                if (ubicacion != null) {
                    latitud = "%.6f".format(java.util.Locale.US, ubicacion.latitude)
                    longitud = "%.6f".format(java.util.Locale.US, ubicacion.longitude)
                    avisoUbicacion = null
                } else {
                    avisoUbicacion = "No se pudo obtener la ubicación. Activa el GPS e inténtalo de nuevo."
                }
            }
            .addOnFailureListener { avisoUbicacion = "No se pudo obtener la ubicación." }
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

    val capacidadKg = aNumero(capacidad)
    val stockKg = aNumero(stock)
    val consumoKg = aNumero(consumo)
    val lat = aNumero(latitud)
    val lng = aNumero(longitud)
    val completo = codigo.isNotBlank() && nombre.isNotBlank() && granja.isNotBlank() &&
        capacidadKg != null && stockKg != null && consumoKg != null && lat != null && lng != null

    Scaffold(
        topBar = { AriztiaTopBar(titulo = if (esNuevo) "Nuevo silo" else "Editar silo", onVolver = onVolver) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Seccion("Identificación")
            CampoTexto(
                valor = codigo,
                onCambio = { codigo = it.uppercase() },
                etiqueta = "Código (ej: SIL-007)",
                habilitado = esNuevo,
                mayusculas = true,
            )
            CampoTexto(nombre, { nombre = it }, "Nombre del silo")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTexto(granja, { granja = it }, "Granja", Modifier.weight(1f))
                CampoTexto(galpon, { galpon = it }, "Galpón", Modifier.weight(1f))
            }
            CampoTexto(tipoAlimento, { tipoAlimento = it }, "Tipo de alimento")

            Seccion("Capacidad y consumo")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoNumero(capacidad, { capacidad = it }, "Capacidad máx. (kg)", Modifier.weight(1f))
                CampoNumero(
                    stock, { stock = it },
                    if (esNuevo) "Stock inicial (kg)" else "Stock actual (kg)",
                    Modifier.weight(1f),
                    habilitado = esNuevo,
                )
            }
            CampoNumero(consumo, { consumo = it }, "Consumo promedio diario (kg)")

            Seccion("Ubicación")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoNumero(latitud, { latitud = it }, "Latitud", Modifier.weight(1f), permiteNegativo = true)
                CampoNumero(longitud, { longitud = it }, "Longitud", Modifier.weight(1f), permiteNegativo = true)
            }
            OutlinedButton(
                onClick = ::usarMiUbicacion,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Usar mi ubicación actual")
            }
            avisoUbicacion?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            AnimatedVisibility(visible = error != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f))) {
                    Text(
                        error.orEmpty(),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Button(
                onClick = {
                    onGuardar(
                        DatosSilo(
                            codigo = codigo,
                            nombre = nombre,
                            granja = granja,
                            galpon = galpon,
                            tipoAlimento = tipoAlimento,
                            capacidadMaxKg = capacidadKg ?: 0.0,
                            stockActualKg = stockKg ?: 0.0,
                            consumoPromedioDiarioKg = consumoKg ?: 0.0,
                            latitud = lat ?: 0.0,
                            longitud = lng ?: 0.0,
                        )
                    )
                },
                enabled = completo,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (esNuevo) "Crear silo" else "Guardar cambios")
            }
        }
    }
}

@Composable
private fun Seccion(titulo: String) {
    Text(
        titulo,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    habilitado: Boolean = true,
    mayusculas: Boolean = false,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        modifier = modifier,
        enabled = habilitado,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = if (mayusculas) KeyboardCapitalization.Characters else KeyboardCapitalization.Sentences
        ),
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
private fun CampoNumero(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    habilitado: Boolean = true,
    permiteNegativo: Boolean = false,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { texto ->
            onCambio(texto.filter { it.isDigit() || it == '.' || it == ',' || (permiteNegativo && it == '-') })
        },
        label = { Text(etiqueta) },
        modifier = modifier,
        enabled = habilitado,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = MaterialTheme.shapes.medium,
    )
}

private fun aNumero(texto: String): Double? = texto.replace(",", ".").toDoubleOrNull()

private fun sinDecimales(valor: Double): String =
    if (valor % 1.0 == 0.0) valor.toLong().toString() else valor.toString()

@Preview(showBackground = true)
@Composable
private fun SiloFormScreenPreview() {
    SiloMonitorAppTheme {
        SiloFormScreen(datosIniciales = null, esNuevo = true, error = null, onVolver = {}, onGuardar = {})
    }
}
