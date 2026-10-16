package tj.osonaloqa.chat

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import tj.osonaloqa.chat.ui.nav.OsonApp
import tj.osonaloqa.chat.ui.theme.OsonAloqaTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OsonAloqaTheme {
                OsonApp()
            }
        }
    }
}
