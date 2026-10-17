package com.group_a1.isocam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.group_a1.isocam.ui.theme.IsocamTheme
import android.content.Intent
import com.group_a1.isocam.screens.CameraActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IsocamTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // We wrap things in a container or place the button directly here
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        androidx.compose.material3.Button(onClick = {
                            val intent = android.content.Intent(this@MainActivity, com.group_a1.isocam.screens.CameraActivity::class.java)
                            startActivity(intent)
                        }) {
                            Text("Open Camera")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    IsocamTheme {
        Greeting("Android")
    }
}