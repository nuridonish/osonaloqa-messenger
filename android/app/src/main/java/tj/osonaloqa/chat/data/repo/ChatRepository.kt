package tj.osonaloqa.chat.data.repo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import tj.osonaloqa.chat.data.SessionManager
import tj.osonaloqa.chat.data.local.AppDatabase
import tj.osonaloqa.chat.data.local.ChatEntity
import tj.osonaloqa.chat.data.local.MessageEntity
import tj.osonaloqa.chat.data.remote.ChatDto
import tj.osonaloqa.chat.data.remote.CreateChatReq
import tj.osonaloqa.chat.data.remote.MessageDto
import tj.osonaloqa.chat.data.remote.OsonApi
import tj.osonaloqa.chat.data.remote.ReadReq
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
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _peerStates = MutableStateFlow<Map<String, PeerState>>(emptyMap())
    val peerStates: StateFlow<Map<String, PeerState>> = _peerStates

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState

    fun observeChats(): Flow<List<ChatEntity>> = db.chatDao().observeAll()
    fun observeChat(id: String): Flow<ChatEntity?> = db.chatDao().observe(id)
    fun observeMessages(chatId: String): Flow<List<MessageEntity>> = db.messageDao().observeChat(chatId)

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
            ws.events.collect { onWsEvent(it) }
        }
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
        val auth = "Bearer ${sessions.current().accessToken}"
        runCatching { api.chats(auth) }.onSuccess { res ->
            val me = sessions.current().userId
            db.chatDao().upsertAll(
                res.chats.map { c -> c.toEntity(myId = me) }
            )
        }
    }

    suspend fun openChat(chatId: String) {
        val auth = "Bearer ${sessions.current().accessToken}"
        db.chatDao().markRead(chatId)
        runCatching { api.messages(auth, chatId) }.onSuccess { res ->
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
        }
        runCatching { api.markRead(auth, chatId, ReadReq(seq = db.messageDao().maxSeq(chatId) ?: 0)) }
    }

    suspend fun createPrivateChat(peerId: String): String? {
        val auth = "Bearer ${sessions.current().accessToken}"
        return runCatching { api.createChat(auth, CreateChatReq(type = "private", peerId = peerId)).chatId }
            .getOrNull()
            ?.also { refreshChats() }
    }

    suspend fun sendMessage(chatId: String, text: String) {
        val me = sessions.current().userId
        val clientId = UUID.randomUUID().toString()
        val entity = MessageEntity(
            id = "local-$clientId",
            chatId = chatId,
            senderId = me,
            seq = (db.messageDao().maxSeq(chatId) ?: 0) + 1,
            clientId = clientId,
            type = "text",
            body = text,
            createdAt = System.currentTimeMillis(),
            outgoing = true,
            state = "pending",
        )
        db.messageDao().upsert(entity)
        val payload = WsMessage(type = "message.send", clientId = clientId, chatId = chatId, body = text)
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