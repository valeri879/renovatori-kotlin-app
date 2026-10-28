package ge.renovatori.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ge.renovatori.app.ui.RenovatoriApp
import ge.renovatori.app.ui.theme.RenovatoriTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RenovatoriTheme {
                RenovatoriApp()
            }
        }
    }
}
