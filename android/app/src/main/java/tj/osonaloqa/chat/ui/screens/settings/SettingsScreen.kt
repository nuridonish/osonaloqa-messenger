package tj.osonaloqa.chat.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tj.osonaloqa.chat.BuildConfig
import tj.osonaloqa.chat.Dependencies
import tj.osonaloqa.chat.OsonAloqaApp
import tj.osonaloqa.chat.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val container = Dependencies.get()
    val session by container.sessionManager.session.collectAsState(initial = null)
    var currentTag by remember { mutableStateOf("tg") }

    LaunchedEffect(Unit) {
        val loc = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags()
        currentTag = loc.ifBlank { "tg" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        (session?.name?.take(1) ?: "?").uppercase(),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(session?.name?.ifBlank { session?.phone ?: "" } ?: "", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Text(session?.phone ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider()
            SectionTitle(stringResource(R.string.language))
            LanguageRow("tg", "тоҷикӣ", currentTag) { t ->
                currentTag = t
                OsonAloqaApp.setLanguage(t)
            }
            LanguageRow("uz", "oʻzbekcha", currentTag) { t ->
                currentTag = t
                OsonAloqaApp.setLanguage(t)
            }
            LanguageRow("ru", "Русский", currentTag) { t ->
                currentTag = t
                OsonAloqaApp.setLanguage(t)
            }
            LanguageRow("fa", "فارسی", currentTag) { t ->
                currentTag = t
                OsonAloqaApp.setLanguage(t)
            }
            LanguageRow("en", "English", currentTag) { t ->
                currentTag = t
                OsonAloqaApp.setLanguage(t)
            }
            LanguageRow("ar", "العربية", currentTag) { t ->
                currentTag = t
                OsonAloqaApp.setLanguage(t)
            }
            HorizontalDivider()
            SectionTitle(stringResource(R.string.about_app))
            InfoRow(stringResource(R.string.version), BuildConfig.VERSION_NAME)
            InfoRow(stringResource(R.string.server_url), BuildConfig.BASE_URL)
            InfoRow(stringResource(R.string.target_android), "Android 8.0+ (14–16)")
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp),
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
    )
}

@Composable
private fun LanguageRow(tag: String, label: String, current: String, onPick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPick(tag) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 16.sp)
        if (current == tag) {
            Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp)
    }
}
