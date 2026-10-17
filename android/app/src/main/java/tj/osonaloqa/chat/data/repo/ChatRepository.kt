package tj.osonaloqa.chat.data.repo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tj.osonaloqa.chat.data.SessionManager
import tj.osonaloqa.chat.data.local.AppDatabase
import tj.osonaloqa.chat.data.local.ChatEntity
import tj.osonaloqa.chat.data.local.MessageEntity
import tj.osonaloqa.chat.data.local.RecentCallEntity
import tj.osonaloqa.chat.data.remote.ChatDto
import tj.osonaloqa.chat.data.remote.CreateChatReq
import tj.osonaloqa.chat.data.remote.MessageDto
import tj.osonaloqa.chat.data.remote.OsonApi
import tj.osonaloqa.chat.data.remote.ReadReq
import tj.osonaloqa.chat.data.remote.LookupReq
import tj.osonaloqa.chat.data.remote.uploadMedia
import tj.osonaloqa.chat.data.remote.WsClient
import tj.osonaloqa.chat.data.remote.WsMessage
import java.util.UUID

data class PeerState(
    val online: Boolean = false,
    val lastSeenAt: Long = 0,
    val typing: Boolean = false,
)

class ChatRepository(
    private val api: OsonApi,
    private val ws: WsClient,
    private val db: AppDatabase,
    private val sessions: SessionManager,
    private val authRepo: AuthRepository? = null,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _peerStates = MutableStateFlow<Map<String, PeerState>>(emptyMap())
    val peerStates: StateFlow<Map<String, PeerState>> = _peerStates

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState

    fun observeChats(): Flow<List<ChatEntity>> = db.chatDao().observeAll()
    fun observeChat(id: String): Flow<ChatEntity?> = db.chatDao().observe(id)
    fun observeMessages(chatId: String): Flow<List<MessageEntity>> = db.messageDao().observeChat(chatId)
    fun observeStarred(): Flow<List<MessageEntity>> = db.messageDao().observeStarred()
    fun observeRecentCalls(): Flow<List<RecentCallEntity>> = db.recentCallDao().observeAll()

    suspend fun toggleStar(id: String) = db.messageDao().toggleStar(id)

    suspend fun markAllRead() {
        val chats = db.chatDao().observeAll().first()
        val auth = "Bearer ${sessions.current().accessToken}"
        for (c in chats) {
            db.chatDao().markRead(c.id)
            if (c.lastSeq > 0) {
                runCatching { api.markRead(auth, c.id, ReadReq(seq = c.lastSeq)) }
            }
        }
    }

    suspend fun addRecentCall(name: String, phone: String, outgoing: Boolean) {
        db.recentCallDao().add(
            RecentCallEntity(id = java.util.UUID.randomUUID().toString(), name = name, phone = phone, time = System.currentTimeMillis(), outgoing = outgoing)
        )
    }

    suspend fun listUsers(): List<tj.osonaloqa.chat.data.remote.UserDto>? =
        authedCall { auth -> api.usersList(auth) }?.users

    suspend fun listDevices(): List<tj.osonaloqa.chat.data.remote.DeviceDto>? =
        authedCall { auth -> api.devices(auth) }?.devices

    suspend fun removeDevice(id: String): Boolean =
        authedCall { auth -> api.removeDevice(auth, id) } != null

    suspend fun uploadMedia(bytes: ByteArray, mime: String): tj.osonaloqa.chat.data.remote.MediaRes? =
        authedCall { auth -> api.uploadMedia(auth, bytes, mime) }

    suspend fun setAvatar(url: String?): Boolean =
        authedCall { auth -> api.setAvatar(auth, tj.osonaloqa.chat.data.remote.SetAvatarReq(avatar_url = url)) } != null

    fun startSync(tokenProvider: suspend () -> String) {
        scope.launch {
            ws.connect(tj.osonaloqa.chat.Dependencies.get().baseUrl, "/v1/ws", tokenProvider)
        }
        scope.launch {
            ws.connected.collect { online ->
                _connectionState.value = online
                if (online) refreshChats()
            }
        }
        scope.launch {
            // Ҳар 10 дақиқа токенро нав мекунад, то WS ҳамеша бо токени корӣ пайваст шавад
            while (true) {
                kotlinx.coroutines.delay(10 * 60 * 1000L)
                runCatching { authRepo?.ensureFreshToken() }
            }
        }
        scope.launch {
            ws.events.collect { onWsEvent(it) }
        }
    }

    /** Даъват бо auth; агар 401 бошад — токенро аз нав гирифта такрор мекунад */
    private suspend fun <T> authedCall(call: suspend (String) -> T): T? {
        var res = runCatching { call("Bearer ${sessions.current().accessToken}") }
        val code = (res.exceptionOrNull() as? retrofit2.HttpException)?.code()
        if (res.isFailure && code == 401) {
            runCatching { authRepo?.refreshIfNeeded() }
            res = runCatching { call("Bearer ${sessions.current().accessToken}") }
        }
        return res.getOrNull()
    }

    private suspend fun baseUrlForWs(): String = tj.osonaloqa.chat.Dependencies.get().baseUrl

    private suspend fun onWsEvent(msg: WsMessage) {
        val me = sessions.current().userId
        when (msg.type) {
            "message.new" -> {
                val m = msg.message ?: return
                val outgoing = m.senderId == me
                val existing = db.messageDao().byId(m.id)
                if (existing == null) {
                    db.messageDao().upsert(
                        MessageEntity(
                            id = m.id,
                            chatId = m.chatId ?: msg.chatId ?: "",
                            senderId = m.senderId,
                            seq = m.seq,
                            clientId = m.clientId,
                            type = m.msgType,
                            body = m.body,
                            createdAt = m.createdAt,
                            outgoing = outgoing,
                            state = if (outgoing) "sent" else "delivered",
                        )
                    )
                }
                if (!outgoing) {
                    ws.send(WsMessage(type = "ack", messageId = m.id, state = "delivered"))
                }
                refreshChats()
            }
            "ack.sent" -> {
                val clientId = msg.clientId ?: return
                db.messageDao().byClientId(clientId)?.let { db.messageDao().setState(it.id, "sent") }
            }
            "ack.update" -> {
                val messageId = msg.messageId
                if (messageId != null) {
                    val m = db.messageDao().byId(messageId)
                    if (m != null) {
                        when (msg.state) {
                            "delivered" -> db.messageDao().markDeliveredUpTo(m.chatId, m.seq, "delivered")
                            "read" -> db.messageDao().markReadUpTo(m.chatId, m.seq)
                        }
                    }
                } else if (msg.chatId != null && msg.lastReadSeq != null) {
                    // қабулкунанда тавассути REST хонд
                    db.messageDao().markReadUpTo(msg.chatId, msg.lastReadSeq)
                }
            }
            "typing" -> {
                val uid = msg.userId ?: return
                val chatId = msg.chatId ?: return
                if (chatId.isNotBlank()) {
                    _peerStates.value = _peerStates.value + (uid to (_peerStates.value[uid] ?: PeerState()).copy(typing = msg.on == true))
                }
            }
            "presence" -> {
                val uid = msg.userId ?: return
                _peerStates.value = _peerStates.value + (uid to (
                    _peerStates.value[uid] ?: PeerState()
                    ).copy(online = msg.online == true, lastSeenAt = msg.lastSeenAt ?: 0, typing = false))
            }
        }
    }

    suspend fun refreshChats() {
        authedCall { auth -> api.chats(auth) }?.let { res ->
            val me = sessions.current().userId
            db.chatDao().upsertAll(res.chats.map { c -> c.toEntity(myId = me) })
        }
    }

    suspend fun openChat(chatId: String) {
        db.chatDao().markRead(chatId)
        val res = authedCall { auth -> api.messages(auth, chatId, limit = 200) } ?: return
        val me = sessions.current().userId
        db.messageDao().upsertAll(
            res.messages.map { m ->
                val outgoing = m.senderId == me
                MessageEntity(
                    id = m.id, chatId = chatId, senderId = m.senderId, seq = m.seq,
                    clientId = m.clientId, type = m.msgType, body = m.body,
                    createdAt = m.createdAt, outgoing = outgoing,
                    state = if (outgoing) "sent" else "delivered",
                )
            }
        )
        val last = res.messages.lastOrNull()
        if (last != null) {
            ws.send(WsMessage(type = "ack", messageId = last.id, state = "read"))
        }
        val maxSeq = db.messageDao().maxSeq(chatId) ?: 0
        if (maxSeq > 0) authedCall { auth -> api.markRead(auth, chatId, ReadReq(seq = maxSeq)) }
    }

    suspend fun createPrivateChat(peerId: String): String? {
        val id = authedCall { auth -> api.createChat(auth, CreateChatReq(type = "private", peerId = peerId)) }?.chatId
        if (id != null) refreshChats()
        return id
    }

    suspend fun createChatByPhone(e164: String): String? {
        val found = authedCall { auth -> api.lookup(auth, LookupReq(listOf(e164))) }
        val peer = found?.users?.firstOrNull() ?: return null
        return createPrivateChat(peer.id)
    }

    suspend fun sendMessage(chatId: String, text: String, msgType: String = "text") {
        val me = sessions.current().userId
        val clientId = java.util.UUID.randomUUID().toString()
        val entity = MessageEntity(
            id = "local-$clientId",
            chatId = chatId,
            senderId = me,
            seq = (db.messageDao().maxSeq(chatId) ?: 0) + 1,
            clientId = clientId,
            type = msgType,
            body = text,
            createdAt = System.currentTimeMillis(),
            outgoing = true,
            state = "pending",
        )
        db.messageDao().upsert(entity)
        val payload = WsMessage(type = "message.send", clientId = clientId, chatId = chatId, msgType = msgType, body = text)
        val ok = ws.send(payload)
        if (!ok) {
            scope.launch {
                var sent = false
                ws.connected.collect { online ->
                    if (online && !sent) {
                        sent = true
                        ws.send(payload)
                        return@collect
                    }
                }
            }
        }
    }

    fun sendTyping(chatId: String, on: Boolean) {
        ws.send(WsMessage(type = "typing", chatId = chatId, on = on))
    }

    private fun ChatDto.toEntity(myId: String): ChatEntity = ChatEntity(
        id = id,
        type = type,
        title = title,
        peerId = peer?.id,
        peerPhone = peer?.phone,
        peerName = peer?.name,
        peerAvatar = peer?.avatarUrl,
        peerOnline = peer?.online == true,
        peerLastSeen = peer?.lastSeenAt ?: 0,
        lastSeq = lastSeq,
        unread = unread,
        lastMessagePreview = lastMessage?.body ?: "",
        lastMessageTime = lastMessage?.createdAt ?: updatedAt,
        lastMessageSenderId = lastMessage?.senderId,
        pinned = false,
        muted = false,
        archived = false,
    )
}