package com.example.silomonitorapp.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.silomonitorapp.domain.ValidadorFormularios
import com.example.silomonitorapp.ui.model.DatosSilo

/**
 * Formulario de alta y edición de Silo adaptado al modelo DatosSilo original,
 * incorporando validación por campo individual con íconos y textos de soporte.
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
    var codigo by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var granja by remember { mutableStateOf("") }
    var galpon by remember { mutableStateOf("") }
    var tipoAlimento by remember { mutableStateOf("") }
    var capacidadMax by remember { mutableStateOf("") }
    var stockActual by remember { mutableStateOf("") }
    var consumoPromedio by remember { mutableStateOf("500.0") }
    var latitud by remember { mutableStateOf("-33.6850") }
    var longitud by remember { mutableStateOf("-71.2150") }

    LaunchedEffect(datosIniciales) {
        datosIniciales?.let {
            codigo = it.codigo
            nombre = it.nombre
            granja = it.granja
            galpon = it.galpon
            tipoAlimento = it.tipoAlimento
            capacidadMax = it.capacidadMaxKg.toString()
            stockActual = it.stockActualKg.toString()
            consumoPromedio = it.consumoPromedioDiarioKg.toString()
            latitud = it.latitud.toString()
            longitud = it.longitud.toString()
        }
    }

    var errorCodigo by remember { mutableStateOf<String?>(null) }
    var errorCapacidad by remember { mutableStateOf<String?>(null) }
    var errorStockActual by remember { mutableStateOf<String?>(null) }
    var errorLatitud by remember { mutableStateOf<String?>(null) }
    var errorLongitud by remember { mutableStateOf<String?>(null) }

    fun validarFormulario(): Boolean {
        errorCodigo = ValidadorFormularios.validarCodigoSilo(codigo)
        errorCapacidad = ValidadorFormularios.validarCapacidad(capacidadMax)
        errorStockActual = if (stockActual.isBlank()) "Ingrese stock inicial" else null
        errorLatitud = ValidadorFormularios.validarCoordenada(latitud, esLatitud = true)
        errorLongitud = ValidadorFormularios.validarCoordenada(longitud, esLatitud = false)

        return errorCodigo == null &&
                errorCapacidad == null &&
                errorStockActual == null &&
                errorLatitud == null &&
                errorLongitud == null
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
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Identificación",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = codigo,
                        onValueChange = {
                            codigo = it
                            errorCodigo = ValidadorFormularios.validarCodigoSilo(it)
                        },
                        label = { Text("Código de Silo (ej. SILO-01)") },
                        enabled = esNuevo,
                        isError = errorCodigo != null,
                        trailingIcon = {
                            if (errorCodigo != null) {
                                Icon(Icons.Filled.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        supportingText = {
                            if (errorCodigo != null) {
                                Text(errorCodigo.orEmpty(), color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre descriptivo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = granja,
                        onValueChange = { granja = it },
                        label = { Text("Granja o Sector") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = galpon,
                        onValueChange = { galpon = it },
                        label = { Text("Galpón") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tipoAlimento,
                        onValueChange = { tipoAlimento = it },
                        label = { Text("Tipo de Alimento") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Capacidades y Ubicación",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = capacidadMax,
                        onValueChange = {
                            capacidadMax = it
                            errorCapacidad = ValidadorFormularios.validarCapacidad(it)
                        },
                        label = { Text("Capacidad Máxima (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = errorCapacidad != null,
                        trailingIcon = {
                            if (errorCapacidad != null) {
                                Icon(Icons.Filled.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        supportingText = {
                            if (errorCapacidad != null) {
                                Text(errorCapacidad.orEmpty(), color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = stockActual,
                        onValueChange = {
                            stockActual = it
                            errorStockActual = if (it.isBlank()) "Ingrese stock inicial" else null
                        },
                        label = { Text("Stock Actual Inicial (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = errorStockActual != null,
                        trailingIcon = {
                            if (errorStockActual != null) {
                                Icon(Icons.Filled.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        supportingText = {
                            if (errorStockActual != null) {
                                Text(errorStockActual.orEmpty(), color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = consumoPromedio,
                        onValueChange = { consumoPromedio = it },
                        label = { Text("Consumo Promedio Diario (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = latitud,
                            onValueChange = {
                                latitud = it
                                errorLatitud = ValidadorFormularios.validarCoordenada(it, esLatitud = true)
                            },
                            label = { Text("Latitud") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = errorLatitud != null,
                            trailingIcon = {
                                if (errorLatitud != null) {
                                    Icon(Icons.Filled.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                                }
                            },
                            supportingText = {
                                if (errorLatitud != null) {
                                    Text(errorLatitud.orEmpty(), color = MaterialTheme.colorScheme.error)
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = longitud,
                            onValueChange = {
                                longitud = it
                                errorLongitud = ValidadorFormularios.validarCoordenada(it, esLatitud = false)
                            },
                            label = { Text("Longitud") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = errorLongitud != null,
                            trailingIcon = {
                                if (errorLongitud != null) {
                                    Icon(Icons.Filled.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                                }
                            },
                            supportingText = {
                                if (errorLongitud != null) {
                                    Text(errorLongitud.orEmpty(), color = MaterialTheme.colorScheme.error)
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
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
                            capacidadMaxKg = capacidadMax.toDoubleOrNull() ?: 0.0,
                            stockActualKg = stockActual.toDoubleOrNull() ?: 0.0,
                            consumoPromedioDiarioKg = consumoPromedio.toDoubleOrNull() ?: 500.0,
                            latitud = latitud.toDoubleOrNull() ?: 0.0,
                            longitud = longitud.toDoubleOrNull() ?: 0.0,
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