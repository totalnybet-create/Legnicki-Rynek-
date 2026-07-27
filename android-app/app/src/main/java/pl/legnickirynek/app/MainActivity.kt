package pl.legnickirynek.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import pl.legnickirynek.app.ui.LegnickiRynekApp
import pl.legnickirynek.app.ui.theme.LegnickiRynekTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LegnickiRynekTheme {
                LegnickiRynekApp()
            }
        }
    }
}
