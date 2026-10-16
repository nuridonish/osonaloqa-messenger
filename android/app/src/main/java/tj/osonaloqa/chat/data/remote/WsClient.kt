package tj.osonaloqa.chat.data.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import kotlin.math.min
import kotlin.random.Random

/** WebSocket-клиент бо reconnect-и худкор (exponential backoff + jitter) */
class WsClient(
    private val client: OkHttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true; explicitNulls = false },
) {
    private var ws: WebSocket? = null
    private var baseUrl: String = ""
    private var tokenProvider: (suspend () -> String)? = null
    private var attempts = 0
    private var wantConnected = false

    private val _state = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _state

    private val _events = MutableSharedFlow<WsMessage>(extraBufferCapacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<WsMessage> = _events

    fun connect(baseUrl: String, wsPath: String, tokenProvider: suspend () -> String) {
        this.baseUrl = baseUrl.replace(Regex("^http"), "ws").trimEnd('/') + wsPath
        this.tokenProvider = tokenProvider
        wantConnected = true
        open()
    }

    private fun open() {
        if (!wantConnected) return
        val tp = tokenProvider ?: return
        val request = Request.Builder().url(baseUrl).build()
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                attempts = 0
                CoroutineScope(Dispatchers.IO).launch {
                    val token = runCatching { tp() }.getOrDefault("")
                    webSocket.send(json.encodeToString(
                        WsMessage.serializer(), WsMessage(type = "auth", token = token)
                    ))
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val msg = runCatching { json.decodeFromString(WsMessage.serializer(), text) }.getOrNull() ?: return
                if (msg.type == "ready") _state.value = true
                _events.tryEmit(msg)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _state.value = false
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _state.value = false
                if (code == 4001) scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (!wantConnected) return
        attempts += 1
        val backoff = min(30_000L, 1_000L * (1L shl min(attempts, 5))) + Random.nextLong(0, 800)
        CoroutineScope(Dispatchers.IO).launch {
            delay(backoff)
            open()
        }
    }

    fun send(msg: WsMessage): Boolean {
        val socket = ws ?: return false
        return socket.send(json.encodeToString(WsMessage.serializer(), msg))
    }

    fun disconnect() {
        wantConnected = false
        _state.value = false
        ws?.close(1000, "bye")
        ws = null
    }
}
