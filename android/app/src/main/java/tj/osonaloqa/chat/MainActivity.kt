package tj.osonaloqa.chat

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import tj.osonaloqa.chat.data.AppSettings
import tj.osonaloqa.chat.ui.nav.OsonApp
import tj.osonaloqa.chat.ui.theme.OsonAloqaTheme

class MainActivity : AppCompatActivity() {
    private val settings = MutableStateFlow(AppSettings())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            Dependencies.get().prefs.settings.collect { settings.value = it }
        }
        setContent {
            val s by settings.collectAsState()
            OsonAloqaTheme(
                darkTheme = when (s.theme) { "dark" -> true; "light" -> false; else -> androidx.compose.foundation.isSystemInDarkTheme() },
                fontScale = s.fontScale,
            ) {
                OsonApp()
            }
        }
    }
}
