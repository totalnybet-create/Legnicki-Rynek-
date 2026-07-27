package pl.legnickirynek.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pl.legnickirynek.app.ui.LegnickiRynekApp
import pl.legnickirynek.app.ui.theme.LegnickiRynekTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LegnickiRynekTheme {
                LegnickiRynekApp()
            }
        }
    }
}
