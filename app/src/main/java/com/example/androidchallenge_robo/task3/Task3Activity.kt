package com.example.androidchallenge_robo.task3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Surface
import com.example.androidchallenge_robo.ui.theme.AndroidChallengeRoboTheme

class Task3Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidChallengeRoboTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF050508)) {
                    RoboFaceScreen()
                }
            }
        }
    }
}
