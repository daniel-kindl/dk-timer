package dev.danielkindl.ocho

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.danielkindl.ocho.ui.navigation.AppNavigation
import dev.danielkindl.ocho.ui.theme.OchoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OchoTheme {
                AppNavigation()
            }
        }
    }
}
