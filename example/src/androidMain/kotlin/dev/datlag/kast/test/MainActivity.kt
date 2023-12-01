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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

var hasRouterListener = false

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.e("Example", "Android app started")

        Kast.setup(this) {
            Log.e("KAST", "CastContextUnavailable")
        }

        setContent {
            MaterialTheme {
                var available by remember { mutableStateOf(Kast.isSetupFinished) }
                var activeScan by remember { mutableStateOf(false) }

                if (Kast.isSetupFinished) {
                    SideEffect {
                        if (!hasRouterListener) {
                            Kast.Listener.setRouteListener { selected, available ->
                                Log.e("KAST", "Selected: ${selected?.name}")
                                Log.e("KAST", "Available: ${available.map { it.name }}")
                            }
                            hasRouterListener = true
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
                } else {
                    Text("Hello World")
                }

                LaunchedEffect(available) {
                    if (!available) {
                        withContext(Dispatchers.IO) {
                            delay(1000)
                            available = Kast.isSetupFinished
                        }
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