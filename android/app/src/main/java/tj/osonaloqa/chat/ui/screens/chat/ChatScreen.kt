package tj.osonaloqa.chat.ui.screens.chat

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import tj.osonaloqa.chat.Dependencies
import tj.osonaloqa.chat.R
import tj.osonaloqa.chat.data.local.MessageEntity
import tj.osonaloqa.chat.ui.screens.main.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(chatId: String, onBack: () -> Unit) {
    val container = Dependencies.get()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var attachOpen by remember { mutableStateOf(false) }
    var longPressed by remember { mutableStateOf<MessageEntity?>(null) }

    val chat by container.chatRepository.observeChat(chatId).collectAsState(initial = null)
    val messages by container.chatRepository.observeMessages(chatId).collectAsState(initial = emptyList())
    val peerStates by container.chatRepository.peerStates.collectAsState()

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return@runCatching
                    val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                    val type = if (mime.startsWith("image/")) "image" else if (mime.startsWith("video/")) "video" else "document"
                    val res = container.chatRepository.uploadMedia(bytes, mime)
                    res?.let { container.chatRepository.sendMessage(chatId, it.url, type) }
                }
            }
        }
    }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return@runCatching
                    val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                    val res = container.chatRepository.uploadMedia(bytes, mime)
                    res?.let { container.chatRepository.sendMessage(chatId, it.url + "|" + fileNameOf(context, uri), "document") }
                }
            }
        }
    }

    LaunchedEffect(chatId) { container.chatRepository.openChat(chatId) }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    val peer = chat?.peerId
    val peerState = peerStates[peer]
    val typing = peerState?.typing == true
    val online = peerState?.online == true || chat?.peerOnline == true

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                (chat?.title ?: chat?.peerPhone ?: "?").take(1).uppercase(),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                chat?.title?.ifBlank { chat?.peerPhone ?: "" } ?: "",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp,
                            )
                            Text(
                                when {
                                    typing -> stringResource(R.string.typing)
                                    online -> stringResource(R.string.online)
                                    else -> ""
                                },
                                fontSize = 12.sp,
                                color = if (typing || online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { }) { Icon(Icons.Filled.Videocam, contentDescription = null) }
                    IconButton(onClick = { }) { Icon(Icons.Filled.Call, contentDescription = null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.navigationBarsPadding().imePadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Box {
                            IconButton(onClick = { attachOpen = !attachOpen }) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DropdownMenu(expanded = attachOpen, onDismissRequest = { attachOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.attach_gallery)) },
                                    onClick = {
                                        attachOpen = false
                                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.attach_document)) },
                                    onClick = { attachOpen = false; pickFile.launch("*/*") },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.attach_location)) },
                                    onClick = { attachOpen = false },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.attach_contact)) },
                                    onClick = { attachOpen = false },
                                )
                            }
                        }
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.message_hint)) },
                            maxLines = 4,
                            shape = RoundedCornerShape(22.dp),
                        )
                        if (text.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                IconButton(onClick = {
                                    val body = text.trim()
                                    text = ""
                                    scope.launch { container.chatRepository.sendMessage(chatId, body) }
                                }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = stringResource(R.string.send),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                        } else {
                            MicButton(onRecorded = { bytes ->
                                scope.launch {
                                    runCatching {
                                        val res = container.chatRepository.uploadMedia(bytes, "audio/aac")
                                        res?.let { container.chatRepository.sendMessage(chatId, it.url, "voice") }
                                    }
                                }
                            })
                        }
                    }
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Text(
                stringResource(R.string.e2e_banner),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (messages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_messages), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(messages, key = { it.id }) { m ->
                        MessageBubble(
                            m,
                            onLongPress = { longPressed = m },
                            onOpenUrl = { url -> runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))) } },
                        )
                    }
                }
            }
        }
    }

    longPressed?.let { m ->
        AlertDialog(
            onDismissRequest = { longPressed = null },
            title = { Text(stringResource(R.string.msg_actions)) },
            text = {
                Column {
                    Text(m.body, maxLines = 3, fontSize = 14.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { container.chatRepository.toggleStar(m.id) }
                    longPressed = null
                }) { Text(stringResource(R.string.msg_star)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    val cm = context.getSystemService(android.content.ClipboardManager::class.java)
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("msg", m.body))
                    longPressed = null
                }) { Text(stringResource(R.string.msg_copy)) }
            },
        )
    }
}

private fun fileNameOf(context: android.content.Context, uri: Uri): String {
    return runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (c.moveToFirst() && idx >= 0) c.getString(idx) ?: "file"
            else "file"
        } ?: "file"
    }.getOrDefault("file")
}

@Composable
private fun MicButton(onRecorded: (ByteArray) -> Unit) {
    val context = LocalContext.current
    var recording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var file by remember { mutableStateOf<java.io.File?>(null) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val f = java.io.File(context.cacheDir, "voice-${System.currentTimeMillis()}.m4a")
            file = f
            val r = if (android.os.Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setOutputFile(f.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            recording = true
        }
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                if (recording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                CircleShape,
            )
            .clickable {
                if (recording) {
                    runCatching { recorder?.stop() }
                    recorder?.release()
                    recorder = null
                    recording = false
                    val f = file
                    if (f != null && f.exists()) onRecorded(f.readBytes())
                    f?.delete()
                } else {
                    permLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (recording) Icons.Filled.Check else Icons.Filled.Mic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun MessageBubble(
    m: MessageEntity,
    onLongPress: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val container = Dependencies.get()
    val baseUrl = remember { tj.osonaloqa.chat.Dependencies.get().baseUrl }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }

    val shape = if (m.outgoing) {
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (m.outgoing) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (m.outgoing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            shape = shape,
            tonalElevation = if (m.outgoing) 0.dp else 2.dp,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .pointerInput(m.id) { detectTapGestures(onLongPress = { onLongPress() }) },
        ) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                when {
                    m.type == "image" && m.body.startsWith("/") -> {
                        AsyncImage(
                            model = baseUrl + m.body,
                            contentDescription = null,
                            modifier = Modifier
                                .widthIn(max = 260.dp)
                                .height(200.dp)
                                .clickable { onOpenUrl(baseUrl + m.body) },
                        )
                    }
                    m.type == "video" && m.body.startsWith("/") -> {
                        Row(
                            modifier = Modifier.clickable { onOpenUrl(baseUrl + m.body) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.msg_video), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    m.type == "voice" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                if (playing) {
                                    player?.stop(); player?.release(); player = null; playing = false
                                } else {
                                    player = MediaPlayer().apply {
                                        setDataSource(baseUrl + m.body)
                                        setOnCompletionListener { playing = false }
                                        prepare()
                                        start()
                                    }
                                    playing = true
                                }
                            },
                        ) {
                            Icon(
                                if (playing) Icons.Filled.Check else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = if (m.outgoing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Filled.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(" •••", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    m.type == "document" -> {
                        val parts = m.body.split("|")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onOpenUrl(baseUrl + parts[0]) },
                        ) {
                            Icon(Icons.Filled.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(parts.getOrNull(1) ?: parts[0], color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    else -> Text(m.body, fontSize = 16.sp)
                }
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        formatTime(m.createdAt),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (m.starred) {
                        Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    if (m.outgoing) {
                        val tint = if (m.state == "read") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        when (m.state) {
                            "pending" -> Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = tint)
                            "sent" -> Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = tint)
                            else -> Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp), tint = tint)
                        }
                    }
                }
            }
        }
    }
}
