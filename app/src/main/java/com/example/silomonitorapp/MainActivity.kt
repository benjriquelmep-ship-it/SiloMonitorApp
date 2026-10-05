package com.example.silomonitorapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.silomonitorapp.ui.navigation.AppNavHost
import com.example.silomonitorapp.ui.theme.SiloMonitorAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SiloMonitorAppTheme {
                AppNavHost()
            }
        }
    }
}
