package uz.shuhratdev.audiotransrec

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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
import uz.shuhratdev.AudioStreamService
import uz.shuhratdev.audiotransrec.ui.theme.AudioTransRecTheme
import kotlin.jvm.java

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        // Oddiy layout o'rniga dinamik tugma yaratamiz
        val button = Button(this).apply {
            text = "Audio Oqimni Boshlash"
        }
        setContentView(button);

        button.setOnClickListener {
            val intent = Intent(this, AudioStreamService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            button.text = "Oqim Faol (Fonda ishlamoqda)"
            button.isEnabled = false
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
    AudioTransRecTheme {
        Greeting("Android")
    }
}