package com.example.androidchallenge_robo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp
import com.example.androidchallenge_robo.ui.theme.AndroidChallengeRoboTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val requestPermissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted
            } else {
                // Handle permission denied
            }
        }

        requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)

        setContent {
            AndroidChallengeRoboTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF050508)) {
                    RoboFaceScreen()
                }
            }
        }
    }
}