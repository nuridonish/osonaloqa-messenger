package tj.osonaloqa.chat.ui.screens.extra

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import tj.osonaloqa.chat.Dependencies
import tj.osonaloqa.chat.R
import tj.osonaloqa.chat.data.remote.UserDto
import tj.osonaloqa.chat.ui.screens.main.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) { content() }
    }
}

@Composable
private fun UserRow(
    user: UserDto,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text((user.name.ifBlank { user.phone }).take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(user.name.ifBlank { user.phone }, fontWeight = FontWeight.SemiBold)
            Text(if (user.online) stringResource(R.string.online) else user.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Checkbox(checked = selected, onCheckedChange = { onClick() })
    }
}

/** Экрани сохтани гурӯҳ */
@Composable
fun GroupScreen(onBack: () -> Unit, onCreated: (String) -> Unit) {
    val container = Dependencies.get()
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<UserDto>?>(null) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var title by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { users = container.chatRepository.listUsers() }

    PickerScaffold(title = stringResource(R.string.menu_new_group), onBack) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            label = { Text(stringResource(R.string.group_name)) },
            singleLine = true,
        )
        Text(
            stringResource(R.string.group_pick_members),
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
        when {
            users == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            users!!.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_users)) }
            else -> LazyColumn(Modifier.weight(1f)) {
                items(users!!.size) { i ->
                    val u = users!![i]
                    UserRow(
                        user = u,
                        selected = u.id in selected,
                        onClick = {
                            selected = if (u.id in selected) selected - u.id else selected + u.id
                        },
                    )
                }
            }
        }
        Button(
            onClick = {
                if (title.isBlank() || selected.isEmpty()) return@Button
                busy = true
                scope.launch {
                    val res = runCatching {
                        container.api.createChat(
                            "Bearer ${container.sessionManager.current().accessToken}",
                            tj.osonaloqa.chat.data.remote.CreateChatReq(type = "group", title = title.trim(), memberIds = selected.toList()),
                        )
                    }
                    busy = false
                    res.getOrNull()?.chatId?.let { onCreated(it) } ?: run { error = "хато" }
                }
            },
            enabled = title.isNotBlank() && selected.isNotEmpty() && !busy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            if (busy) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
            Text(stringResource(R.string.group_create_btn))
        }
    }
}

/** Рассылка: паём ба чанд контакт */
@Composable
fun BroadcastScreen(onBack: () -> Unit, onDone: () -> Unit) {
    val container = Dependencies.get()
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<UserDto>?>(null) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { users = container.chatRepository.listUsers() }

    PickerScaffold(title = stringResource(R.string.menu_broadcast), onBack) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            label = { Text(stringResource(R.string.broadcast_text)) },
        )
        if (busy) {
            LinearProgressIndicator(
                progress = { if (selected.isEmpty()) 0f else progress.toFloat() / selected.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
        LazyColumn(Modifier.weight(1f)) {
            users?.let { list ->
                items(list.size) { i ->
                    val u = list[i]
                    UserRow(u, u.id in selected) {
                        selected = if (u.id in selected) selected - u.id else selected + u.id
                    }
                }
            }
        }
        Button(
            onClick = {
                busy = true
                scope.launch {
                    var sent = 0
                    for (id in selected) {
                        val chatId = container.chatRepository.createPrivateChat(id)
                        if (chatId != null) {
                            container.chatRepository.sendMessage(chatId, text.trim())
                            sent++
                        }
                        progress = sent
                    }
                    busy = false
                    onDone()
                }
            },
            enabled = text.isNotBlank() && selected.isNotEmpty() && !busy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) { Text(stringResource(R.string.broadcast_send)) }
    }
}

/** Избранные паёмҳо */
@Composable
fun StarredScreen(onBack: () -> Unit) {
    val container = Dependencies.get()
    val starred by container.chatRepository.observeStarred().collectAsState(initial = emptyList())
    PickerScaffold(title = stringResource(R.string.menu_starred), onBack) {
        if (starred.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.starred_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn {
                items(starred.size) { i ->
                    val m = starred[i]
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(m.body, fontWeight = if (i == 0) FontWeight.SemiBold else FontWeight.Normal)
                        Row {
                            Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text(formatTime(m.createdAt), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

/** Дастгоҳҳои пайваст */
@Composable
fun DevicesScreen(onBack: () -> Unit) {
    val container = Dependencies.get()
    val scope = rememberCoroutineScope()
    var devices by remember { mutableStateOf<List<tj.osonaloqa.chat.data.remote.DeviceDto>?>(null) }

    fun load() { scope.launch { devices = container.chatRepository.listDevices() } }
    LaunchedEffect(Unit) { load() }

    PickerScaffold(title = stringResource(R.string.menu_devices), onBack) {
        Text(
            stringResource(R.string.devices_hint),
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
        when {
            devices == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            devices!!.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_devices)) }
            else -> LazyColumn {
                items(devices!!.size) { i ->
                    val d = devices!![i]
                    androidx.compose.material3.ListItem(
                        headlineContent = { Text(d.device_name.ifBlank { "Дастгоҳ" } + if (d.current) " •" else "") },
                        supportingContent = { Text(formatTime(d.created_at)) },
                        trailingContent = {
                            if (!d.current) {
                                TextButtonSmall(stringResource(R.string.devices_remove)) {
                                    scope.launch { container.chatRepository.removeDevice(d.id); load() }
                                }
                            } else Text(stringResource(R.string.devices_current), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TextButtonSmall(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = MaterialTheme.colorScheme.error,
        fontSize = 14.sp,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
    )
}

/** Намоишгари статусҳо бо навори пешрафт */
@Composable
fun StatusViewerScreen(onBack: () -> Unit) {
    val container = Dependencies.get()
    var statuses by remember { mutableStateOf<List<tj.osonaloqa.chat.data.remote.StatusDto>>(emptyList()) }
    var index by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        runCatching {
            container.api.statuses("Bearer ${container.sessionManager.current().accessToken}")
        }.onSuccess { statuses = it.statuses }
    }
    LaunchedEffect(index, statuses.size) {
        if (statuses.isNotEmpty() && index < statuses.size) {
            kotlinx.coroutines.delay(5000)
            if (index < statuses.size - 1) index++ else onBack()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (statuses.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_statuses), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val s = statuses[index]
            Column(Modifier.fillMaxSize()) {
                LinearProgressIndicator(
                    progress = { (index + 1f) / statuses.size },
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                )
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                    Text(s.authorName ?: s.authorPhone ?: "", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.width(8.dp))
                    Text(formatTime(s.createdAt), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        s.body,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(32.dp),
                    )
                }
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("‹", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 32.sp,
                        modifier = Modifier.clickable { if (index > 0) index-- })
                    Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 32.sp,
                        modifier = Modifier.clickable { if (index < statuses.size - 1) index++ else onBack() })
                }
            }
        }
    }
}
