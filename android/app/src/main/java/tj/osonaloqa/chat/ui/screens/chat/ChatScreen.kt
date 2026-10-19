package tj.osonaloqa.chat.ui.screens.chat

import android.Manifest
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tj.osonaloqa.chat.Dependencies
import tj.osonaloqa.chat.R
import tj.osonaloqa.chat.data.local.MessageEntity
import tj.osonaloqa.chat.ui.screens.main.formatTime

private val EMOJIS = listOf(
    "😀", "😂", "🥰", "😍", "😎", "🤩", "🥳", "😭", "😡", "🤔",
    "👍", "👎", "👏", "🙏", "💪", "🤝", "❤️", "🔥", "🎉", "✨",
    "🌹", "☀️", "🌙", "⭐", "🍕", "☕", "⚽", "🎓", "📌", "✅",
    "❌", "❓", "💯", "🤖", "🎁", "🚗", "🏠", "📞", "📸", "🎵",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(chatId: String, onBack: () -> Unit, onCall: (video: Boolean, outgoing: Boolean) -> Unit = { _, _ -> }) {
    val container = Dependencies.get()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var attachOpen by remember { mutableStateOf(false) }
    var emojiOpen by remember { mutableStateOf(false) }
    var longPressed by remember { mutableStateOf<MessageEntity?>(null) }
    var pollOpen by remember { mutableStateOf(false) }
    var eventOpen by remember { mutableStateOf(false) }

    val chat by container.chatRepository.observeChat(chatId).collectAsState(initial = null)
    val messages by container.chatRepository.observeMessages(chatId).collectAsState(initial = emptyList())
    val peerStates by container.chatRepository.peerStates.collectAsState()

    fun uploadAndSend(bytes: ByteArray, mime: String, type: String, suffix: String? = null) {
        scope.launch {
            runCatching {
                val res = container.chatRepository.uploadMedia(bytes, mime)
                res?.let { container.chatRepository.sendMessage(chatId, it.url + (suffix?.let { s -> "|$s" } ?: ""), type) }
            }
        }
    }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return@rememberLauncherForActivityResult
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val type = if (mime.startsWith("image/")) "image" else "video"
            uploadAndSend(bytes, mime, type)
        }
    }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return@rememberLauncherForActivityResult
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            uploadAndSend(bytes, mime, "document", fileNameOf(context, uri))
        }
    }
    val pickContact = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) {
            runCatching {
                var name = ""
                var number = ""
                context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        name = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)) ?: ""
                    }
                }
                if (name.isNotBlank()) {
                    scope.launch { container.chatRepository.sendMessage(chatId, "$name|", "contact") }
                }
            }
        }
    }
    val cameraUri = remember { mutableStateOf<Uri?>(null) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            cameraUri.value?.let { uri ->
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return@rememberLauncherForActivityResult
                uploadAndSend(bytes, "image/jpeg", "image")
            }
        }
    }
    val locPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
            val loc = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            if (loc != null) {
                scope.launch { container.chatRepository.sendMessage(chatId, "${loc.latitude},${loc.longitude}", "location") }
            }
        }
    }

    LaunchedEffect(chatId) { container.chatRepository.openChat(chatId) }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    val typing = peerStates[chat?.peerId]?.typing == true
    val online = peerStates[chat?.peerId]?.online == true || chat?.peerOnline == true

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
                            Text(chat?.title?.ifBlank { chat?.peerPhone ?: "" } ?: "", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
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
                    IconButton(onClick = { onCall(true, true) }) { Icon(Icons.Filled.Videocam, contentDescription = null) }
                    IconButton(onClick = { onCall(false, true) }) { Icon(Icons.Filled.Call, contentDescription = null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.navigationBarsPadding().imePadding()) {
                    if (emojiOpen) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(8),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(4.dp),
                        ) {
                            items(EMOJIS.size) { i ->
                                Box(Modifier.clickable { text += EMOJIS[i]; emojiOpen = false }.padding(6.dp)) {
                                    Text(EMOJIS[i], fontSize = 24.sp)
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        IconButton(onClick = { emojiOpen = !emojiOpen }) {
                            Icon(Icons.Filled.Mood, contentDescription = stringResource(R.string.emoji), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.message_hint)) },
                            maxLines = 4,
                            shape = RoundedCornerShape(22.dp),
                        )
                        Box {
                            IconButton(onClick = { attachOpen = !attachOpen }) {
                                Icon(Icons.Filled.AttachFile, contentDescription = stringResource(R.string.attach), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DropdownMenu(expanded = attachOpen, onDismissRequest = { attachOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("📎 " + stringResource(R.string.attach_gallery)) },
                                    leadingIcon = { Icon(Icons.Filled.AddCircle, contentDescription = null) },
                                    onClick = {
                                        attachOpen = false
                                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.attach_document)) },
                                    leadingIcon = { Icon(Icons.Filled.AttachFile, contentDescription = null) },
                                    onClick = { attachOpen = false; pickFile.launch("*/*") },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.attach_file)) },
                                    leadingIcon = { Icon(Icons.Filled.AttachFile, contentDescription = null) },
                                    onClick = { attachOpen = false; pickFile.launch("*/*") },
                                )
                                DropdownMenuItem(
                                    text = { Text("📍 " + stringResource(R.string.attach_location)) },
                                    leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                                    onClick = {
                                        attachOpen = false
                                        locPerm.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("👤 " + stringResource(R.string.attach_contact)) },
                                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                    onClick = {
                                        attachOpen = false
                                        pickContact.launch(android.provider.ContactsContract.Contacts.CONTENT_URI)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("📊 " + stringResource(R.string.attach_poll)) },
                                    leadingIcon = { Icon(Icons.Filled.Poll, contentDescription = null) },
                                    onClick = { attachOpen = false; pollOpen = true },
                                )
                                DropdownMenuItem(
                                    text = { Text("📅 " + stringResource(R.string.attach_event)) },
                                    leadingIcon = { Icon(Icons.Filled.Event, contentDescription = null) },
                                    onClick = { attachOpen = false; eventOpen = true },
                                )
                            }
                        }
                        IconButton(onClick = {
                            val f = java.io.File(context.cacheDir, "cam-${System.currentTimeMillis()}.jpg")
                            cameraUri.value = androidx.core.content.FileProvider.getUriForFile(context, "tj.osonaloqa.chat.fileprovider", f)
                            val i = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                                putExtra(android.provider.MediaStore.EXTRA_OUTPUT, cameraUri.value)
                                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                            }
                            runCatching { context.startActivity(i) }
                        }) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = stringResource(R.string.attach_camera), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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
                                    emojiOpen = false
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
                            VoiceButton(
                                onVoiceRecorded = { bytes, dur -> uploadAndSend(bytes, "audio/aac", "voice", "$dur") },
                                onCircleRecorded = { bytes, dur -> uploadAndSend(bytes, "video/mp4", "video_circle", "$dur") },
                            )
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
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(messages, key = { it.id }) { m ->
                        MessageBubble(
                            m,
                            onLongPress = { longPressed = m },
                            onOpenUrl = { url ->
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                            },
                            onDial = { phone ->
                                runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) }
                            },
                            onCalendar = { title, time ->
                                runCatching {
                                    val i = Intent(Intent.ACTION_INSERT).apply {
                                        data = android.provider.CalendarContract.Events.CONTENT_URI
                                        putExtra(android.provider.CalendarContract.Events.TITLE, title)
                                        putExtra(android.provider.CalendarContract.Events.DESCRIPTION, time)
                                    }
                                    context.startActivity(i)
                                }
                            },
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
            text = { Text(m.body, maxLines = 3, fontSize = 14.sp) },
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

    if (pollOpen) {
        var q by remember { mutableStateOf("") }
        var o1 by remember { mutableStateOf("") }
        var o2 by remember { mutableStateOf("") }
        var o3 by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pollOpen = false },
            title = { Text(stringResource(R.string.attach_poll)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = q, onValueChange = { q = it }, label = { Text(stringResource(R.string.poll_question)) }, singleLine = true)
                    OutlinedTextField(value = o1, onValueChange = { o1 = it }, label = { Text("1") }, singleLine = true)
                    OutlinedTextField(value = o2, onValueChange = { o2 = it }, label = { Text("2") }, singleLine = true)
                    OutlinedTextField(value = o3, onValueChange = { o3 = it }, label = { Text("3") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val opts = listOf(o1, o2, o3).filter { it.isNotBlank() }
                    if (q.isNotBlank() && opts.isNotEmpty()) {
                        scope.launch { container.chatRepository.sendMessage(chatId, "$q|${opts.joinToString(";")}", "poll") }
                    }
                    pollOpen = false
                }) { Text(stringResource(R.string.send)) }
            },
            dismissButton = { TextButton(onClick = { pollOpen = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (eventOpen) {
        var title by remember { mutableStateOf("") }
        var date by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { eventOpen = false },
            title = { Text(stringResource(R.string.attach_event)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(R.string.event_title)) }, singleLine = true)
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text(stringResource(R.string.event_date)) },
                        supportingText = { Text(stringResource(R.string.event_date_hint)) },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) {
                        scope.launch { container.chatRepository.sendMessage(chatId, "${title.trim()}|${date.trim()}", "event") }
                    }
                    eventOpen = false
                }) { Text(stringResource(R.string.send)) }
            },
            dismissButton = { TextButton(onClick = { eventOpen = false }) { Text(stringResource(R.string.cancel)) } },
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

/** Микрофон: 1 пахн = овоз, 2 пахн = видео-кружок; ҳангоми сабт — таймер */
@Composable
private fun VoiceButton(onVoiceRecorded: (ByteArray, Int) -> Unit, onCircleRecorded: (ByteArray, Int) -> Unit) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf<String?>(null) } // null | "voice" | "circle"
    var seconds by remember { mutableStateOf(0) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var file by remember { mutableStateOf<java.io.File?>(null) }

    var pendingMode by remember { mutableStateOf<Boolean?>(null) }

    fun startRec(withVideo: Boolean) {
        val f = java.io.File(context.cacheDir, if (withVideo) "circle-${System.currentTimeMillis()}.mp4" else "voice-${System.currentTimeMillis()}.m4a")
        file = f
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        if (withVideo) {
            r.setVideoSource(MediaRecorder.VideoSource.CAMERA)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setVideoSize(480, 480)
            r.setVideoFrameRate(24)
            r.setVideoEncodingBitRate(1_200_000)
        } else {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        }
        r.setOutputFile(f.absolutePath)
        runCatching {
            r.prepare()
            r.start()
            recorder = r
            mode = if (withVideo) "circle" else "voice"
            seconds = 0
        }
    }

    fun stopRec(send: Boolean) {
        runCatching { recorder?.stop() }
        recorder?.release()
        recorder = null
        val f = file
        val m = mode
        mode = null
        if (send && f != null && f.exists() && m != null) {
            val dur = seconds
            val bytes = f.readBytes()
            if (m == "voice") onVoiceRecorded(bytes, dur) else onCircleRecorded(bytes, dur)
        }
        f?.delete()
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.all { it }) {
            pendingMode?.let { startRec(it) }
        }
        pendingMode = null
    }

    fun askAndStart(withVideo: Boolean) {
        val perms = if (withVideo) listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA) else listOf(Manifest.permission.RECORD_AUDIO)
        val allGranted = perms.all { androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (allGranted) startRec(withVideo) else {
            pendingMode = withVideo
            permLauncher.launch(perms.toTypedArray())
        }
    }

    LaunchedEffect(mode) {
        if (mode != null) {
            while (mode != null) {
                delay(1000)
                seconds++
            }
        }
    }

    DisposableEffect(Unit) { onDispose { runCatching { recorder?.stop(); recorder?.release() } } }

    if (mode == null) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            askAndStart(false)
                        },
                        onDoubleTap = {
                            askAndStart(true)
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Mic, contentDescription = stringResource(R.string.voice_record), tint = MaterialTheme.colorScheme.onPrimary)
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Нишони сабт + таймер
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.error, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(
                    String.format("%d:%02d", seconds / 60, seconds % 60),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            IconButton(onClick = { stopRec(false) }) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel), tint = MaterialTheme.colorScheme.error)
            }
            if (mode == "voice") {
                IconButton(onClick = {
                    stopRec(false)
                    startRec(true)
                }) {
                    Icon(Icons.Filled.Videocam, contentDescription = stringResource(R.string.voice_circle), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { stopRec(true) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (mode == "circle") Icons.Filled.Stop else Icons.Filled.Send,
                    contentDescription = stringResource(R.string.send),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    m: MessageEntity,
    onLongPress: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onDial: (String) -> Unit,
    onCalendar: (String, String) -> Unit,
) {
    val baseUrl = remember { Dependencies.get().baseUrl }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    var votes by remember(m.id) { mutableStateOf(mapOf<Int, Int>()) }
    var myVote by remember(m.id) { mutableStateOf<Int?>(null) }

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
                val parts = m.body.split("|")
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
                    m.type == "voice" && m.body.startsWith("/") -> {
                        val dur = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                if (playing) {
                                    player?.stop(); player?.release(); player = null; playing = false
                                } else {
                                    player = MediaPlayer().apply {
                                        setDataSource(baseUrl + parts[0])
                                        setOnCompletionListener { playing = false }
                                        prepare()
                                        start()
                                    }
                                    playing = true
                                }
                            },
                        ) {
                            Icon(
                                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(6.dp))
                            // Мавҷи оддӣ
                            listOf(4, 9, 14, 18, 12, 7).forEach { h ->
                                Box(
                                    Modifier
                                        .padding(horizontal = 1.dp)
                                        .size(3.dp, h.dp)
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(String.format("%d:%02d", dur / 60, dur % 60), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    m.type == "video_circle" && m.body.startsWith("/") -> {
                        val dur = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onOpenUrl(baseUrl + parts[0]) },
                        ) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.voice_circle) + " · ${dur}s", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    m.type == "video" && m.body.startsWith("/") -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onOpenUrl(baseUrl + m.body) },
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.msg_video), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    m.type == "document" && m.body.startsWith("/") -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onOpenUrl(baseUrl + parts[0]) },
                        ) {
                            Icon(Icons.Filled.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(parts.getOrNull(1) ?: parts[0], color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    m.type == "location" -> {
                                                Column(Modifier.clickable { onOpenUrl("geo:${m.body}") }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text(stringResource(R.string.attach_location), fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                "https://maps.google.com/?q=${m.body}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    m.type == "contact" -> {
                        val name = parts.getOrNull(0) ?: ""
                        val phone = parts.getOrNull(1) ?: ""
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(name, fontWeight = FontWeight.SemiBold)
                                if (phone.isNotBlank()) Text(phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { onDial(phone) }) {
                                Icon(Icons.Filled.Call, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    m.type == "poll" -> {
                        val q = parts.getOrNull(0) ?: ""
                        val opts = (parts.getOrNull(1) ?: "").split(";").filter { it.isNotBlank() }
                        Text(q, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        opts.forEachIndexed { idx, o ->
                            val count = votes[idx] ?: 0
                            val mine = myVote == idx
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        myVote = if (myVote == idx) null else idx
                                        val v = votes.toMutableMap()
                                        if (myVote == idx) v[idx] = (v[idx] ?: 0) + 1
                                        votes = v
                                    }
                                    .padding(vertical = 3.dp),
                            ) {
                                Icon(
                                    if (mine) Icons.Filled.Check else Icons.Filled.AddCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(o, modifier = Modifier.weight(1f), color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                if (count > 0) Text("$count", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    m.type == "event" -> {
                        val title = parts.getOrNull(0) ?: ""
                        val time = parts.getOrNull(1) ?: ""
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(title, fontWeight = FontWeight.SemiBold)
                                if (time.isNotBlank()) Text(time, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (time.isNotBlank()) {
                            TextButton(onClick = { onCalendar(title, time) }) {
                                Text(stringResource(R.string.event_add_calendar), fontSize = 12.sp)
                            }
                        }
                    }
                    else -> Text(m.body, fontSize = 16.sp)
                }
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(formatTime(m.createdAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
