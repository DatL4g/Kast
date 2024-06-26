package dev.datlag.kast.test

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import dev.datlag.kast.Kast

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.e("Example", "Android app started")

        Kast.setup(this) {
            Log.e("KAST", "CastContextUnavailable")
        }

        setContent {
            val allDevices by Kast.allAvailableDevices.collectAsState()

            MaterialTheme {
                var activeScan by remember { mutableStateOf(false) }

                LaunchedEffect(allDevices) {
                    if (allDevices.isNotEmpty()) {
                        Log.e("KAST", "Selected: ${allDevices.firstOrNull { it.isSelected }?.name}")
                        Log.e("KAST", "Available: ${allDevices.map { it.name }}")
                    }
                }
                Column {
                    Text("Casting ready")
                    Button(
                        onClick = {
                            activeScan = !activeScan
                        }
                    ) {
                        Text("Toggle active scan")
                    }
                }
                LaunchedEffect(activeScan) {
                    if (activeScan) {
                        Kast.Router.activeDiscovery()
                    } else {
                        Kast.Router.passiveDiscovery()
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        Kast.dispose()
                    }
                }
            }
        }
    }
}