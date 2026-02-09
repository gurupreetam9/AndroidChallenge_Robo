package com.example.androidchallenge_robo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.androidchallenge_robo.task2.Task2Activity
import com.example.androidchallenge_robo.task3.Task3Activity
import com.example.androidchallenge_robo.task6.Task6Activity
import com.example.androidchallenge_robo.ui.theme.AndroidChallengeRoboTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidChallengeRoboTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainMenuScreen(
                        onTask2Click = { startActivity(Intent(this, Task2Activity::class.java)) },
                        onTask3Click = { startActivity(Intent(this, Task3Activity::class.java)) },
                        onTask6Click = { startActivity(Intent(this, Task6Activity::class.java)) }
                    )
                }
            }
        }
    }
}

@Composable
fun MainMenuScreen(
    onTask2Click: () -> Unit,
    onTask3Click: () -> Unit,
    onTask6Click: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Robo Challenge Tasks", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = onTask2Click, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Text(text = "Task 2: Robo Face (UI Only)")
        }
        
        Button(onClick = onTask3Click, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Text(text = "Task 3: Sensor Fusion")
        }
        
        Button(onClick = onTask6Click, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Text(text = "Task 6: TFLite Emotion")
        }
    }
}