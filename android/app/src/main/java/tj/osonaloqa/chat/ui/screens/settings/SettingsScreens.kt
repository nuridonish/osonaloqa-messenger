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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import tj.osonaloqa.chat.BuildConfig
import tj.osonaloqa.chat.Dependencies
import tj.osonaloqa.chat.OsonAloqaApp
import tj.osonaloqa.chat.R
import tj.osonaloqa.chat.data.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreen(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
internal fun SettingsToggleRow(label: String, checked: Boolean, subtitle: String? = null, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 16.sp)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
internal fun SettingsNavRow(label: String, icon: ImageVector? = null, subtitle: String? = null, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 16.sp)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 20.sp)
    }
}

@Composable
internal fun SectionHeader(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 4.dp),
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
    )
}

@Composable
internal fun ThemeButtons(current: String, onPick: (String) -> Unit, labels: List<Pair<String, String>>) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEach { (v, label) ->
            Button(
                onClick = { onPick(v) },
                colors = if (current == v) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) { Text(label) }
        }
    }
}

/** Экрани асосии Танзимот — тамоми дарахт */
@Composable
fun SettingsTreeScreen(onBack: () -> Unit, onOpenRoute: (String) -> Unit) {
    val container = Dependencies.get()
    val session by container.sessionManager.session.collectAsState(initial = null)

    SubScreen(title = stringResource(R.string.settings), onBack) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenRoute("settings/profile") }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text((session?.name?.take(1) ?: "?").uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 26.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(session?.name?.ifBlank { session?.phone ?: "" } ?: "", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text(session?.phone ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.settings_tap_edit), color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            }
        }
        HorizontalDivider()

        SettingsNavRow(stringResource(R.string.settings_account), Icons.Filled.PhotoCamera) { onOpenRoute("settings/profile") }
        SettingsNavRow(stringResource(R.string.settings_privacy), Icons.Filled.Lock) { onOpenRoute("settings/privacy") }
        SettingsNavRow(stringResource(R.string.settings_lists), Icons.Filled.GroupAdd) { onOpenRoute("settings/lists") }
        SettingsNavRow(stringResource(R.string.settings_chats), Icons.Filled.Info) { onOpenRoute("settings/chats") }
        SettingsNavRow(stringResource(R.string.settings_notifications), Icons.Filled.Notifications) { onOpenRoute("settings/notifications") }
        SettingsNavRow(stringResource(R.string.settings_data), Icons.Filled.DataUsage) { onOpenRoute("settings/data") }
        SettingsNavRow(stringResource(R.string.settings_parental), Icons.Filled.Shield) { onOpenRoute("settings/parental") }
        SettingsNavRow(stringResource(R.string.settings_accessibility), Icons.Filled.DarkMode) { onOpenRoute("settings/accessibility") }
        SettingsNavRow(stringResource(R.string.language), Icons.Filled.Language) { onOpenRoute("settings/language") }
        SettingsNavRow(stringResource(R.string.settings_help), Icons.Filled.Feedback) { onOpenRoute("settings/help") }
        SettingsNavRow(stringResource(R.string.settings_updates), Icons.Filled.Info) { onOpenRoute("settings/about") }
        Spacer(Modifier.height(24.dp))
    }
}

/** Профил: ном, маълумот */
@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val container = Dependencies.get()
    val scope = rememberCoroutineScope()
    val session by container.sessionManager.session.collectAsState(initial = null)
    var name by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    val saved = stringResource(R.string.save)

    LaunchedEffect(session) {
        if (session != null && !loaded) {
            name = session?.name ?: ""
            about = session?.about ?: ""
            loaded = true
        }
    }

    SubScreen(title = stringResource(R.string.settings_account), onBack) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text((name.take(1).ifBlank { "?" }).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 40.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.profile_avatar_hint), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.profile_name)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = about,
            onValueChange = { about = it },
            label = { Text(stringResource(R.string.profile_about)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        msg?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary) }
        Button(
            onClick = {
                saving = true
                scope.launch {
                    container.authRepository.updateProfile(name = name.trim(), about = about.trim(), username = null)
                        .onSuccess { msg = saved }
                    saving = false
                }
            },
            enabled = name.isNotBlank() && !saving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            if (saving) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
            Text(stringResource(R.string.save))
        }
        HorizontalDivider()
        val deleteContact = stringResource(R.string.settings_delete_contact)
        SettingsNavRow(stringResource(R.string.settings_delete_account), subtitle = stringResource(R.string.settings_delete_hint)) {
            msg = deleteContact
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Махфият */
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val container = Dependencies.get()
    val prefs by container.prefs.settings.collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()
    val set = { key: String, v: Boolean -> scope.launch { container.prefs.setBool(key, v) } }

    SubScreen(title = stringResource(R.string.settings_privacy), onBack) {
        SectionHeader(stringResource(R.string.privacy_visibility))
        SettingsToggleRow(stringResource(R.string.privacy_last_seen), prefs.privacyLastSeen) { set("p_last_seen", it) }
        SettingsToggleRow(stringResource(R.string.privacy_read), prefs.privacyReadReceipts) { set("p_read", it) }
        SettingsToggleRow(stringResource(R.string.privacy_typing), prefs.privacyTyping) { set("p_typing", it) }
        HorizontalDivider()
        SectionHeader(stringResource(R.string.privacy_lists))
        SettingsNavRow(stringResource(R.string.privacy_blocked)) { }
        SettingsNavRow(stringResource(R.string.privacy_disappearing)) { }
        HorizontalDivider()
        SectionHeader(stringResource(R.string.settings_lock_title))
        SettingsToggleRow(stringResource(R.string.settings_lock), subtitle = stringResource(R.string.settings_lock_hint), checked = prefs.privacyTyping) { set("p_typing", it) }
        Spacer(Modifier.height(24.dp))
    }
}

/** Рӯйхатҳо */
@Composable
fun ListsScreen(onBack: () -> Unit) {
    SubScreen(title = stringResource(R.string.settings_lists), onBack) {
        SectionHeader(stringResource(R.string.lists_title))
        SettingsNavRow(stringResource(R.string.menu_starred)) { }
        SettingsNavRow(stringResource(R.string.privacy_blocked)) { }
        SettingsNavRow(stringResource(R.string.lists_archived)) { }
        Spacer(Modifier.height(24.dp))
    }
}

/** Чатҳо */
@Composable
fun ChatsSettingsScreen(onBack: () -> Unit) {
    val container = Dependencies.get()
    val prefs by container.prefs.settings.collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()
    val set = { key: String, v: Boolean -> scope.launch { container.prefs.setBool(key, v) } }

    SubScreen(title = stringResource(R.string.settings_chats), onBack) {
        SectionHeader("1. " + stringResource(R.string.chats_screen))
        Text(stringResource(R.string.chats_theme), Modifier.padding(horizontal = 16.dp), fontSize = 15.sp)
        ThemeButtons(prefs.theme, { scope.launch { container.prefs.setString("theme", it) } }, listOf(
            "system" to stringResource(R.string.theme_system),
            "light" to stringResource(R.string.theme_light),
            "dark" to stringResource(R.string.theme_dark),
        ))
        HorizontalDivider()
        SectionHeader("2. " + stringResource(R.string.chats_settings))
        SettingsToggleRow(stringResource(R.string.chats_enter_send), prefs.enterToSend) { set("enter_send", it) }
        SettingsToggleRow(stringResource(R.string.chats_media_vis), prefs.mediaVisibility) { set("media_vis", it) }
        Text(stringResource(R.string.chats_font_size), Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 15.sp)
        Slider(
            value = prefs.fontScale,
            onValueChange = { scope.launch { container.prefs.setFloat("font_scale", it) } },
            valueRange = 0.8f..1.4f,
            steps = 5,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        SettingsToggleRow(stringResource(R.string.chats_voice_transcript), subtitle = stringResource(R.string.chats_voice_hint), checked = prefs.animateMedia) { set("anim", it) }
        SettingsToggleRow(stringResource(R.string.chats_stickers), checked = prefs.animateMedia) { set("anim", it) }
        HorizontalDivider()
        SectionHeader("3. " + stringResource(R.string.chats_archive))
        SettingsToggleRow(stringResource(R.string.chats_keep_archived), prefs.archivedVisible) { set("archived_visible", it) }
        SettingsNavRow(stringResource(R.string.chats_backup), subtitle = stringResource(R.string.chats_backup_hint)) { }
        SettingsNavRow(stringResource(R.string.chats_transfer)) { }
        SettingsNavRow(stringResource(R.string.chats_history)) { }
        Spacer(Modifier.height(24.dp))
    }
}

/** Огоҳиномаҳо */
@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val container = Dependencies.get()
    val prefs by container.prefs.settings.collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()
    val set = { key: String, v: Boolean -> scope.launch { container.prefs.setBool(key, v) } }

    SubScreen(title = stringResource(R.string.settings_notifications), onBack) {
        SectionHeader(stringResource(R.string.notif_main))
        SettingsToggleRow(stringResource(R.string.tab_communities), prefs.notifyGroups) { set("n_group", it) }
        SettingsToggleRow(stringResource(R.string.notif_messages), prefs.notifyMessages) { set("n_msg", it) }
        SettingsToggleRow(stringResource(R.string.tab_calls), prefs.notifyCalls) { set("n_call", it) }
        SettingsToggleRow(stringResource(R.string.tab_status), prefs.notifyStatus) { set("n_status", it) }
        HorizontalDivider()
        SectionHeader(stringResource(R.string.notif_message_group))
        SettingsToggleRow(stringResource(R.string.notif_sound), prefs.notifSound) { set("n_sound", it) }
        SettingsToggleRow(stringResource(R.string.notif_vibration), prefs.notifVibration) { set("n_vibro", it) }
        SettingsToggleRow(stringResource(R.string.notif_light), prefs.notifLight) { set("n_light", it) }
        SettingsToggleRow(stringResource(R.string.notif_priority), prefs.notifPriority) { set("n_priority", it) }
        SettingsToggleRow(stringResource(R.string.notif_reactions), prefs.notifReactions) { set("n_reactions", it) }
        HorizontalDivider()
        SectionHeader(stringResource(R.string.notif_calls_section))
        SettingsToggleRow(stringResource(R.string.notif_ringtone), prefs.notifSound) { set("n_sound", it) }
        SettingsToggleRow(stringResource(R.string.notif_vibration), prefs.notifVibration) { set("n_vibro", it) }
        HorizontalDivider()
        SectionHeader(stringResource(R.string.notif_status_section))
        SettingsToggleRow(stringResource(R.string.notif_sound), prefs.notifSound) { set("n_sound", it) }
        SettingsToggleRow(stringResource(R.string.notif_vibration), prefs.notifVibration) { set("n_vibro", it) }
        SettingsToggleRow(stringResource(R.string.notif_priority), prefs.notifPriority) { set("n_priority", it) }
        SettingsToggleRow(stringResource(R.string.notif_reactions), prefs.notifReactions) { set("n_reactions", it) }
        SettingsToggleRow(stringResource(R.string.notif_badge), prefs.notifBadge) { set("n_badge", it) }
        Spacer(Modifier.height(24.dp))
    }
}

/** Маълумот ва хотира */
@Composable
fun DataStorageScreen(onBack: () -> Unit) {
    val container = Dependencies.get()
    val prefs by container.prefs.settings.collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()
    val set = { key: String, v: Boolean -> scope.launch { container.prefs.setBool(key, v) } }

    SubScreen(title = stringResource(R.string.settings_data), onBack) {
        SettingsNavRow(stringResource(R.string.data_manage_storage)) { }
        SettingsNavRow(stringResource(R.string.data_statistics)) { }
        SettingsToggleRow(stringResource(R.string.data_saver), checked = prefs.mediaQuality == "economy") {
            scope.launch { container.prefs.setString("media_quality", if (it) "economy" else "auto") }
        }
        SettingsNavRow(stringResource(R.string.data_proxy)) { }
        HorizontalDivider()
        SectionHeader(stringResource(R.string.data_quality))
        ThemeButtons(prefs.mediaQuality, { scope.launch { container.prefs.setString("media_quality", it) } }, listOf(
            "auto" to stringResource(R.string.theme_system),
            "high" to stringResource(R.string.data_quality_high),
            "economy" to stringResource(R.string.data_quality_econ),
        ))
        HorizontalDivider()
        SectionHeader(stringResource(R.string.data_autodownload))
        Text(stringResource(R.string.data_voice_hint), Modifier.padding(horizontal = 16.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SettingsToggleRow(stringResource(R.string.data_mobile), prefs.autoDownloadMobile) { set("ad_mobile", it) }
        SettingsToggleRow("Wi-Fi", prefs.autoDownloadWifi) { set("ad_wifi", it) }
        SettingsToggleRow(stringResource(R.string.data_roaming), prefs.autoDownloadRoaming) { set("ad_roaming", it) }
        Spacer(Modifier.height(24.dp))
    }
}

/** Назорати волидайн */
@Composable
fun ParentalScreen(onBack: () -> Unit) {
    val container = Dependencies.get()
    val prefs by container.prefs.settings.collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()
    val set = { key: String, v: Boolean -> scope.launch { container.prefs.setBool(key, v) } }

    SubScreen(title = stringResource(R.string.settings_parental), onBack) {
        SettingsToggleRow(stringResource(R.string.parental_lock_settings), subtitle = stringResource(R.string.parental_lock_hint), checked = !prefs.privacyTyping) { set("p_typing", !it) }
        SettingsToggleRow(stringResource(R.string.parental_hidden_chats), checked = prefs.archivedVisible) { set("archived_visible", it) }
        SettingsToggleRow(stringResource(R.string.parental_no_status), subtitle = stringResource(R.string.parental_no_status_hint), checked = !prefs.notifyStatus) { set("n_status", !it) }
        Spacer(Modifier.height(24.dp))
    }
}

/** Дастрасии махсус */
@Composable
fun AccessibilityScreen(onBack: () -> Unit) {
    val container = Dependencies.get()
    val prefs by container.prefs.settings.collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()
    val set = { key: String, v: Boolean -> scope.launch { container.prefs.setBool(key, v) } }

    SubScreen(title = stringResource(R.string.settings_accessibility), onBack) {
        SettingsToggleRow(stringResource(R.string.access_contrast), subtitle = stringResource(R.string.access_contrast_hint), checked = prefs.highContrast) { set("contrast", it) }
        SettingsToggleRow(stringResource(R.string.access_animation), subtitle = stringResource(R.string.access_animation_hint), checked = prefs.animateMedia) { set("anim", it) }
        Text(stringResource(R.string.chats_font_size), Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 15.sp)
        Slider(
            value = prefs.fontScale,
            onValueChange = { scope.launch { container.prefs.setFloat("font_scale", it) } },
            valueRange = 0.8f..1.4f,
            steps = 5,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(24.dp))
    }
}

data class Lang(val tag: String, val flag: String, val name: String)

/** Забонҳо бо парчамҳо */
@Composable
fun LanguageScreen(onBack: () -> Unit) {
    val langs = listOf(
        Lang("tg", "🇹🇯", "Тоҷикӣ"), Lang("ru", "🇷🇺", "Русский"), Lang("uz", "🇺🇿", "Oʻzbekcha"),
        Lang("fa", "🇮🇷", "فارسی"), Lang("ar", "🇸🇦", "العربية"), Lang("en", "🇬🇧", "English"),
        Lang("tr", "🇹🇷", "Türkçe"), Lang("de", "🇩🇪", "Deutsch"), Lang("fr", "🇫🇷", "Français"),
        Lang("es", "🇪🇸", "Español"), Lang("pt", "🇵🇹", "Português"), Lang("it", "🇮🇹", "Italiano"),
        Lang("zh", "🇨🇳", "中文"), Lang("ja", "🇯🇵", "日本語"), Lang("ko", "🇰🇷", "한국어"),
        Lang("hi", "🇮🇳", "हिन्दी"), Lang("ur", "🇵🇰", "اردو"), Lang("id", "🇮🇩", "Indonesia"),
        Lang("ms", "🇲🇾", "Melayu"), Lang("th", "🇹🇭", "ไทย"), Lang("vi", "🇻🇳", "Tiếng Việt"),
        Lang("kk", "🇰🇿", "Қазақша"), Lang("ky", "🇰🇬", "Кыргызча"), Lang("tk", "🇹🇲", "Türkmen"),
        Lang("az", "🇦🇿", "Azərbaycan"), Lang("hy", "🇦🇲", "Հայերեն"), Lang("ka", "🇬🇪", "ქართული"),
        Lang("be", "🇧🇾", "Беларуская"), Lang("uk", "🇺🇦", "Українська"), Lang("he", "🇮🇱", "עברית"),
        Lang("nl", "🇳🇱", "Nederlands"), Lang("pl", "🇵🇱", "Polski"), Lang("sv", "🇸🇪", "Svenska"),
        Lang("el", "🇬🇷", "Ελληνικά"), Lang("ro", "🇷🇴", "Română"), Lang("hu", "🇭🇺", "Magyar"),
        Lang("cs", "🇨🇿", "Čeština"), Lang("bg", "🇧🇬", "Български"), Lang("sr", "🇷🇸", "Српски"),
        Lang("hr", "🇭🇷", "Hrvatski"), Lang("fi", "🇫🇮", "Suomi"), Lang("da", "🇩🇰", "Dansk"),
        Lang("no", "🇳🇴", "Norsk"), Lang("sw", "🇰🇪", "Kiswahili"), Lang("am", "🇪🇹", "አማርኛ"),
        Lang("bn", "🇧🇩", "বাংলা"), Lang("ta", "🇱🇰", "தமிழ்"),
    )
    var current by remember { mutableStateOf("tg") }
    LaunchedEffect(Unit) {
        val loc = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags()
        current = loc.ifBlank { "tg" }
    }

    SubScreen(title = stringResource(R.string.language), onBack) {
        LazyColumn {
            items(langs.size) { i ->
                val l = langs[i]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            current = l.tag
                            OsonAloqaApp.setLanguage(l.tag)
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(l.flag, fontSize = 22.sp)
                    Spacer(Modifier.width(14.dp))
                    Text(l.name, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    if (current == l.tag) Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Кумак ва фикру мулоҳиза */
@Composable
fun HelpScreen(onBack: () -> Unit, onOpenRoute: (String) -> Unit) {
    SubScreen(title = stringResource(R.string.settings_help), onBack) {
        SettingsNavRow(stringResource(R.string.help_center), subtitle = stringResource(R.string.help_center_hint)) { onOpenRoute("settings/about") }
        SettingsNavRow(stringResource(R.string.help_feedback), subtitle = stringResource(R.string.help_feedback_hint)) { }
        SettingsNavRow(stringResource(R.string.help_terms), subtitle = stringResource(R.string.help_terms_hint)) { }
        SettingsNavRow(stringResource(R.string.help_reports), subtitle = stringResource(R.string.help_reports_hint)) { }
        SettingsNavRow(stringResource(R.string.help_about)) { onOpenRoute("settings/about") }
        Spacer(Modifier.height(24.dp))
    }
}

/** Дар бораи барнома */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    SubScreen(title = stringResource(R.string.about_app), onBack) {
        Column(Modifier.padding(16.dp)) {
            Text("OSON ALOQA", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.welcome_tagline), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            InfoRowStatic(stringResource(R.string.version), BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")")
            InfoRowStatic(stringResource(R.string.server_url), BuildConfig.BASE_URL)
            InfoRowStatic(stringResource(R.string.target_android), "Android 8.0+ (14–16)")
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.about_updates_none), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InfoRowStatic(title: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}
