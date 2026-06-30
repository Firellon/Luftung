package dev.macfi.luftung

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import dev.macfi.luftung.refresh.RefreshScheduler
import dev.macfi.luftung.ui.VentilationAdviceScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RefreshScheduler.schedule(this)
        setContent {
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) {
                // The screen refresh button will pick up the permission result.
            }

            MaterialTheme {
                VentilationAdviceScreen(
                    requestLocationPermission = {
                        permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                )
            }
        }
    }
}
