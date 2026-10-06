package com.example.silomonitorapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.silomonitorapp.notificaciones.NotificadorAlertas
import com.example.silomonitorapp.ui.navigation.AppNavHost
import com.example.silomonitorapp.ui.theme.SiloMonitorAppTheme
import com.example.silomonitorapp.ui.viewmodel.SiloViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SiloViewModel by viewModels {
        SiloViewModel.Factory(application)
    }

    // Si el usuario rechaza el permiso, la app sigue funcionando sin notificaciones
    private val solicitarPermisoNotificaciones =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificadorAlertas.crearCanal(this)
        pedirPermisoNotificaciones()

        setContent {
            SiloMonitorAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(viewModel = viewModel)
                }
            }
        }
    }

    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            solicitarPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
