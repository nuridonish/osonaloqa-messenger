package tj.osonaloqa.chat.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val peerId: String?,
    val peerPhone: String?,
    val peerName: String?,
    val peerOnline: Boolean,
    val peerLastSeen: Long,
    val lastSeq: Long,
    val unread: Long,
    val lastMessagePreview: String,
    val lastMessageTime: Long,
    val lastMessageSenderId: String?,
    val pinned: Boolean = false,
    val muted: Boolean = false,
    val archived: Boolean = false,
)

@Entity(tableName = "messages", indices = [Index("chatId", "seq")])
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val seq: Long,
    val clientId: String,
    val type: String,
    val body: String,
    val createdAt: Long,
    val outgoing: Boolean,
    val state: String,
)
