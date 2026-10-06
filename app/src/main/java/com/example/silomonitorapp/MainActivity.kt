package com.example.silomonitorapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.silomonitorapp.ui.navigation.AppNavHost
import com.example.silomonitorapp.ui.theme.SiloMonitorAppTheme
import com.example.silomonitorapp.ui.viewmodel.SiloViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SiloViewModel by viewModels {
        SiloViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}
