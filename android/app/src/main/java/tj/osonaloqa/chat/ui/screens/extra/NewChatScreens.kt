package tj.osonaloqa.chat.ui.screens.extra

import android.content.Intent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import tj.osonaloqa.chat.Dependencies
import tj.osonaloqa.chat.R
import tj.osonaloqa.chat.data.remote.UserDto

@Composable
internal fun UserRowSimple(user: UserDto, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                (user.name.ifBlank { user.phone }).take(1).uppercase(),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(user.name.ifBlank { user.phone }, fontWeight = FontWeight.SemiBold)
            val sub = subtitle ?: buildString {
                append(user.phone)
                user.username?.let { append(" · @").append(it) }
                if (user.online) append(" • онлайн")
            }
            Text(sub, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** ЗИНА 0 — экрани «Новый чат» */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen(onBack: () -> Unit, onOpenRoute: (String) -> Unit, onOpenChat: (String) -> Unit) {
    val container = Dependencies.get()
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<UserDto>?>(null) }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            users = if (query.startsWith("@") || query.isNotBlank()) {
                container.chatRepository.searchUsers(query) ?: container.chatRepository.listUsers()
            } else container.chatRepository.listUsers()
        }
    }
    LaunchedEffect(Unit) { load() }
    LaunchedEffect(query) {
        if (query.length >= 2) users = container.chatRepository.searchUsers(query) ?: users
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.new_chat), fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                    actions = {
                        IconButton(onClick = { onOpenRoute("group/new") }) {
                            Icon(Icons.Filled.GroupAdd, contentDescription = stringResource(R.string.menu_new_group))
                        }
                        IconButton(onClick = { onOpenRoute("contact/new") }) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = stringResource(R.string.step1_new_contact))
                        }
                        IconButton(onClick = { onOpenRoute("community/new") }) {
                            Icon(Icons.Filled.Groups, contentDescription = stringResource(R.string.step1_new_community))
                        }
                        IconButton(onClick = { searching = !searching }) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
                        }
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_contact_settings)) },
                                onClick = { menuOpen = false; onOpenRoute("settings/contact") },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_invite_friend)) },
                                onClick = { menuOpen = false; onOpenRoute("invite") },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.step2_contacts_in_app)) },
                                onClick = { menuOpen = false; onOpenRoute("contacts/inapp") },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_refresh)) },
                                onClick = {
                                    menuOpen = false
                                    query = ""
                                    load()
                                    scope.launch { container.chatRepository.refreshChats() }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_help)) },
                                onClick = { menuOpen = false; onOpenRoute("usage/help") },
                            )
                        }
                    },
                )
                if (searching) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        placeholder = { Text(stringResource(R.string.search_hint_name_at)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                    )
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenRoute("contacts/inapp") }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(stringResource(R.string.step2_contacts_in_app), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(stringResource(R.string.step2_subtitle), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenRoute("invite") }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(stringResource(R.string.step3_invite_to_app), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(stringResource(R.string.step3_subtitle), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider()
            when {
                users == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                users!!.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_users), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn {
                    items(users!!.size) { i ->
                        val u = users!![i]
                        UserRowSimple(u) {
                            scope.launch {
                                val chatId = container.chatRepository.createPrivateChat(u.id)
                                if (chatId != null) onOpenChat(chatId)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** ЗИНА 1.1 — Новая группа бо ҷустуҷӯ + клавиатура + 3 қисм */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreenV2(onBack: () -> Unit, onCreated: (String) -> Unit) {
    val container = Dependencies.get()
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<UserDto>?>(null) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var title by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var keypad by remember { mutableStateOf(false) }
    var section by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { users = container.chatRepository.listUsers() }
    LaunchedEffect(query) {
        if (query.length >= 2) users = container.chatRepository.searchUsers(query) ?: users
    }

    val filtered = users?.filter {
        query.isBlank() || it.name.contains(query, true) || it.phone.contains(query) || (it.username ?: "").contains(query, true)
    }
    val sections = listOf(
        stringResource(R.string.group_sec_frequent),
        stringResource(R.string.group_sec_all),
        stringResource(R.string.group_sec_device),
    )
    val chats by container.chatRepository.observeChats().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(stringResource(R.string.menu_new_group), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.search_hint_name_at)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                )
                IconButton(onClick = { keypad = !keypad }) {
                    Icon(Icons.Filled.Keyboard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (keypad) {
                Text(
                    query,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                val kp = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("*", "0", "#"))
                kp.forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { d ->
                            Text(
                                d,
                                fontSize = 22.sp,
                                modifier = Modifier
                                    .clickable { query += d }
                                    .padding(10.dp),
                            )
                        }
                    }
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                label = { Text(stringResource(R.string.group_name)) },
                singleLine = true,
            )
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                sections.forEachIndexed { i, s ->
                    FilterChip(selected = section == i, onClick = { section = i }, label = { Text("${i + 1}. $s") })
                }
            }
            LazyColumn(Modifier.weight(1f)) {
                val list = filtered ?: emptyList()
                val shown = when (section) {
                    0 -> list.sortedByDescending { u -> chats.firstOrNull { it.peerId == u.id }?.lastMessageTime ?: 0 }
                    else -> list
                }
                items(shown.size) { i ->
                    val u = shown[i]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            UserRowSimple(u, subtitle = u.phone) {
                                selected = if (u.id in selected) selected - u.id else selected + u.id
                            }
                        }
                        Checkbox(checked = u.id in selected, onCheckedChange = {
                            selected = if (u.id in selected) selected - u.id else selected + u.id
                        })
                    }
                }
            }
            Button(
                onClick = {
                    busy = true
                    scope.launch {
                        val res = runCatching {
                            container.api.createChat(
                                "Bearer ${container.sessionManager.current().accessToken}",
                                tj.osonaloqa.chat.data.remote.CreateChatReq(type = "group", title = title.trim(), memberIds = selected.toList()),
                            )
                        }
                        busy = false
                        res.getOrNull()?.chatId?.let { onCreated(it) }
                    }
                },
                enabled = title.isNotBlank() && selected.isNotEmpty() && !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                if (busy) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
                Text(stringResource(R.string.group_create_btn) + if (selected.isNotEmpty()) " (${selected.size})" else "")
            }
        }
    }
}

/** ЗИНА 1.2 — Новый контакт: QR дар боло + 4 сатр + Сохранить */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewContactScreen(onBack: () -> Unit, onAdded: (String) -> Unit) {
    val container = Dependencies.get()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var qrOpen by remember { mutableStateOf(false) }
    var foundId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(stringResource(R.string.step1_new_contact), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                actions = {
                    IconButton(onClick = { qrOpen = true }) {
                        Icon(Icons.Filled.QrCode2, contentDescription = stringResource(R.string.contact_qr), tint = MaterialTheme.colorScheme.primary)
                    }
                },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(16.dp)) {
            OutlinedTextField(value = first, onValueChange = { first = it }, label = { Text("1. " + stringResource(R.string.contact_first_name)) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = last, onValueChange = { last = it }, label = { Text("2. " + stringResource(R.string.contact_last_name)) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.removePrefix("@") },
                label = { Text("3. " + stringResource(R.string.contact_username)) },
                supportingText = { Text(stringResource(R.string.contact_username_hint)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter { c -> c.isDigit() } },
                label = { Text("4. " + stringResource(R.string.phone_number)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            msg?.let { Text(it, Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.primary) }
            val addedTxt = stringResource(R.string.contact_added)
            val notInAppTxt = stringResource(R.string.contact_not_in_app)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch {
                        val e164 = "+992" + phone.filter { it.isDigit() }
                        val found = container.chatRepository.searchUsers(phone) ?: emptyList()
                        val match = found.find { it.phone.endsWith(phone) || (username.isNotBlank() && it.username == username) }
                        if (match != null) {
                            val chatId = container.chatRepository.createPrivateChat(match.id)
                            msg = "✓ $addedTxt"
                            if (chatId != null) onAdded(chatId)
                        } else {
                            msg = notInAppTxt
                        }
                        foundId = match?.id
                    }
                },
                enabled = phone.length >= 6,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.contact_save))
            }
        }
    }

    if (qrOpen) {
        AlertDialog(
            onDismissRequest = { qrOpen = false },
            title = { Text(stringResource(R.string.contact_qr)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    QrImage(
                        data = "osonaloqa://add?u=" + username + "&p=" + phone,
                        modifier = Modifier.size(220.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.contact_qr_hint), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { qrOpen = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** ЗИНА 1.3 — Новое сообщество */
@OptIn(ExperimentalMaterial3Api::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(onBack: () -> Unit, onCreated: (String) -> Unit) {
    val container = Dependencies.get()
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(stringResource(R.string.step1_new_community), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(16.dp)) {
            Icon(
                Icons.Filled.Groups,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.community_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.community_desc), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.community_examples),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { },
            )
            Spacer(Modifier.height(16.dp))
            CommunityFeatureRow("1", stringResource(R.string.community_f1))
            CommunityFeatureRow("2", stringResource(R.string.community_f2))
            CommunityFeatureRow("3", stringResource(R.string.community_f3))
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.group_name)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    busy = true
                    scope.launch {
                        val res = runCatching {
                            container.api.createChat(
                                "Bearer ${container.sessionManager.current().accessToken}",
                                tj.osonaloqa.chat.data.remote.CreateChatReq(type = "group", title = title.trim()),
                            )
                        }
                        busy = false
                        res.getOrNull()?.chatId?.let { onCreated(it) }
                    }
                },
                enabled = title.isNotBlank() && !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.community_create_btn)) }
        }
    }
}

@Composable
private fun CommunityFeatureRow(num: String, text: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(num, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 15.sp)
    }
}

/** ЗИНА 2 — Контакты в Осон Алоқа */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsInAppScreen(onBack: () -> Unit, onOpenChat: (String) -> Unit) {
    val container = Dependencies.get()
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<UserDto>?>(null) }
    LaunchedEffect(Unit) { users = container.chatRepository.listUsers() }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(stringResource(R.string.step2_contacts_in_app), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            when {
                users == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                users!!.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_users))
                }
                else -> LazyColumn {
                    items(users!!.size) { i ->
                        val u = users!![i]
                        UserRowSimple(u) {
                            scope.launch {
                                val id = container.chatRepository.createPrivateChat(u.id)
                                if (id != null) onOpenChat(id)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** ЗИНА 3 — Пригласить в Осон Алоқа (WhatsApp, MAX, OK, Instagram, VK, браузерҳо) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val link = "https://osonaloqa.tj/invite — Осон Алоқа мессенҷери зебо ва бехатар! Инро насб кунед!"
    val apps = listOf(
        "WhatsApp" to "com.whatsapp",
        "MAX" to "ru.maxim.chat",
        "Telegram" to "org.telegram.messenger",
        "OK" to "ru.ok.android",
        "Instagram" to "com.instagram.android",
        "VK" to "com.vkontakte.android",
        "Chrome" to "com.android.chrome",
        "Яндекс Браузер" to "com.yandex.browser",
        "Gmail" to "com.google.android.gm",
        "SMS" to null,
    )

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(stringResource(R.string.step3_invite_to_app), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(16.dp)) {
            Text(stringResource(R.string.invite_desc), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val i = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, link)
                    }
                    context.startActivity(Intent.createChooser(i, stringResource(R.string.step3_invite_to_app)))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.invite_share_any))
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn {
                items(apps.size) { i ->
                    val (name, pkg) = apps[i]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val i = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, link)
                                    pkg?.let { setPackage(it) }
                                }
                                runCatching {
                                    context.startActivity(Intent.createChooser(i, name))
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(name.take(1), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(name, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

/** Настройка контакта (⋮) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSettingsScreen(onBack: () -> Unit) {
    val container = Dependencies.get()
    val prefs by container.prefs.settings.collectAsState(initial = tj.osonaloqa.chat.data.AppSettings())
    val blocked by container.chatRepository.blocked.collectAsState()
    val scope = rememberCoroutineScope()

    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(stringResource(R.string.menu_contact_settings), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Text(
                stringResource(R.string.contact_settings_sync),
                Modifier.padding(16.dp),
                fontSize = 15.sp,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.contact_sync_toggle), Modifier.weight(1f))
                androidx.compose.material3.Switch(
                    checked = prefs.contactSync,
                    onCheckedChange = { scope.launch { container.prefs.setContactSync(it) } },
                )
            }
            Text(
                stringResource(R.string.contact_sync_hint),
                Modifier.padding(horizontal = 16.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text(
                stringResource(R.string.privacy_blocked),
                Modifier.padding(horizontal = 16.dp),
                fontWeight = FontWeight.SemiBold,
            )
            if (blocked.isEmpty()) {
                Text(
                    stringResource(R.string.blocked_empty),
                    Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn {
                    items(blocked.size) { i ->
                        val phone = blocked.elementAt(i)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(phone, Modifier.weight(1f))
                            TextButton(onClick = {
                                scope.launch { container.chatRepository.toggleBlockedPhone(container, phone) }
                            }) { Text(stringResource(R.string.blocked_unblock)) }
                        }
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text(
                stringResource(R.string.contact_settings_blacklist_hint),
                Modifier.padding(16.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Помощь — дастури истифода */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageHelpScreen(onBack: () -> Unit) {
    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(stringResource(R.string.menu_help), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(16.dp)) {
            items(GUIDE.size) { i ->
                val (q, a) = GUIDE[i]
                Text("❓ $q", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(a, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

private val GUIDE = listOf(
    "Чӣ тавр сабти ном кардан мумкин?" to "Барномаро кушоед → Продолжить → коди кишвар ва рақами телефонро ворид кунед → коди тасдиқ (SMS) — ҳама. Профил: ном ва акс илова кунед.",
    "Чӣ тавр паём фиристидан?" to "Ҷадвали «Чатҳо» → Новый чат ё чати кушода → паёмро нависед → тугмаи фиристодан. Барои акс/файл — тугмаи ＋, барои овоз — микрофон.",
    "Чӣ тавр занг задан?" to "Дар болои чат тугмаи 📞 (овозӣ) ё 🎥 (видео). Барои занг ба рақамҳои телефон — ҷадвали «Звонки» → клавиатура.",
    "Чӣ тавр гурӯҳ сохтан?" to "Новый чат → Новая группа → ном ва аъзоҳоро интихоб кунед → Создать группу.",
    "Чӣ тавр шахсро илова кунам?" to "Новый чат → Новый контакт → 4 сатрро пур кунед (ном, насаб, @username, рақам) → QR-и худро ҳам дидан мумкин аст → Сохранить.",
    "Чӣ тавр дӯстонро даъват кардан?" to "Новый чат → Пригласить друга → интихоб кунед: WhatsApp, MAX, OK, Instagram, VK ё браузер — паём бо истинод фиристода мешавад.",
    "Ҷустуҷӯ чӣ тавр кор мекунад?" to "Дар Новый чат тугмаи 🔍 → ном, рақам ё @username-ро нависед. @username-и ягона барои ҳар истифодабаранда аст.",
    "Аккаунтро чӣ тавр барқарор кардан?" to "Бо ҳамон рақами телефон ворид шавед — ҳамаи чатҳо бар меоянд. @username-и шумо ягона аст ва дигарон шуморо тавассути он меёбанд.",
    "Овоз чӣ тавр хомӯш кардан?" to "Дар чат дароз пахн кунед ё танзимот → Огоҳиномаҳо → овоз/вибрация/нурро танзим кунед.",
    "Маълумоти ман чӣ қадар бехатар аст?" to "Паёмҳо тавассути сервер мераванд; барои махфият — Танзимот → Конфиденциальность: охирин бор дар шабака, қвитанцияҳо, ҳолати менависад.",
)

/** QR-тасвир тавассути zxing */
@Composable
fun QrImage(data: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(data) {
        runCatching {
            val bits = com.google.zxing.qrcode.QRCodeWriter().encode(
                data,
                com.google.zxing.BarcodeFormat.QR_CODE,
                512,
                512,
            )
            val bmp = android.graphics.Bitmap.createBitmap(512, 512, android.graphics.Bitmap.Config.RGB_565)
            for (x in 0 until 512) for (y in 0 until 512) {
                bmp.setPixel(x, y, if (bits.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
            bmp
        }.getOrNull()
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            CircularProgressIndicator()
        }
    }
}
