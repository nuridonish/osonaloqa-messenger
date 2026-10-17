package tj.osonaloqa.chat.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class UserDto(
    val id: String,
    val phone: String,
    val name: String = "",
    val about: String = "",
    val username: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val online: Boolean = false,
)

@Serializable
data class RequestOtpReq(val phone: String, val defaultCountry: String? = null)

@Serializable
data class RequestOtpRes(
    val ok: Boolean = false,
    val phone: String = "",
    val devCode: String? = null,
    val error: String? = null,
)

@Serializable
data class VerifyReq(val phone: String, val code: String, val deviceName: String = "android")

@Serializable
data class VerifyRes(
    val accessToken: String = "",
    val refreshToken: String = "",
    val isNew: Boolean = false,
    val user: UserDto? = null,
    val error: String? = null,
)

@Serializable
data class RefreshReq(val refreshToken: String)

@Serializable
data class RefreshRes(val accessToken: String, val refreshToken: String, val user: UserDto? = null)

@Serializable
data class PeerDto(
    val id: String,
    val phone: String,
    val name: String = "",
    val about: String = "",
    val username: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val online: Boolean = false,
    @SerialName("last_seen_at") val lastSeenAt: Long = 0,
)

@Serializable
data class LastMessageDto(
    val id: String,
    @SerialName("sender_id") val senderId: String,
    val type: String = "text",
    val body: String = "",
    val seq: Long = 0,
    @SerialName("created_at") val createdAt: Long = 0,
)

@Serializable
data class ChatDto(
    val id: String,
    val type: String,
    val title: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("last_seq") val lastSeq: Long = 0,
    val unread: Long = 0,
    val peer: PeerDto? = null,
    @SerialName("last_message") val lastMessage: LastMessageDto? = null,
    @SerialName("updated_at") val updatedAt: Long = 0,
)

@Serializable
data class ChatsRes(val chats: List<ChatDto>)

@Serializable
data class CreateChatReq(val type: String, val peerId: String? = null, val title: String? = null, val memberIds: List<String>? = null)

@Serializable
data class CreateChatRes(val chatId: String)

@Serializable
data class MessageDto(
    val id: String,
    @SerialName("sender_id") val senderId: String = "",
    val chatId: String? = null,
    val seq: Long = 0,
    @SerialName("client_id") val clientId: String = "",
    @SerialName("msg_type") val msgType: String = "text",
    val body: String = "",
    @SerialName("created_at") val createdAt: Long = 0,
)

@Serializable
data class MessagesRes(val messages: List<MessageDto>, val hasMore: Boolean = false)

@Serializable
data class StatusDto(
    val id: String,
    @SerialName("user_id") val userId: String = "",
    val type: String = "text",
    val body: String = "",
    val bg: String? = null,
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("expires_at") val expiresAt: Long = 0,
    @SerialName("author_name") val authorName: String? = null,
    @SerialName("author_phone") val authorPhone: String? = null,
)

@Serializable
data class CreateStatusReq(val type: String = "text", val body: String, val bg: String? = null)

@Serializable
data class StatusesRes(val statuses: List<StatusDto>)

@Serializable
data class WsMessage(
    val type: String,
    val token: String? = null,
    val clientId: String? = null,
    val chatId: String? = null,
    val msgType: String? = null,
    val body: String? = null,
    val messageId: String? = null,
    val state: String? = null,
    val on: Boolean? = null,
    val seq: Long? = null,
    val userId: String? = null,
    val online: Boolean? = null,
    val lastSeenAt: Long? = null,
    val message: MessageDto? = null,
    val duplicate: Boolean? = null,
    val lastReadSeq: Long? = null,
    val error: String? = null,
)
