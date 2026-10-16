package tj.osonaloqa.chat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(chats: List<ChatEntity>)

    @Query("SELECT * FROM chats WHERE archived = 0 ORDER BY pinned DESC, lastMessageTime DESC")
    fun observeAll(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :id")
    fun observe(id: String): Flow<ChatEntity?>

    @Query("UPDATE chats SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean)

    @Query("UPDATE chats SET muted = :muted WHERE id = :id")
    suspend fun setMuted(id: String, muted: Boolean)

    @Query("UPDATE chats SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean)

    @Query("UPDATE chats SET unread = 0 WHERE id = :id")
    suspend fun markRead(id: String)
}

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY seq ASC")
    fun observeChat(chatId: String): Flow<List<MessageEntity>>

    @Query("UPDATE messages SET state = :state WHERE chatId = :chatId AND seq <= :seq AND outgoing = 1 AND state != 'read'")
    suspend fun markDeliveredUpTo(chatId: String, seq: Long, state: String)

    @Query("UPDATE messages SET state = 'read' WHERE chatId = :chatId AND seq <= :seq AND outgoing = 1")
    suspend fun markReadUpTo(chatId: String, seq: Long)

    @Query("UPDATE messages SET state = :state WHERE id = :messageId AND outgoing = 1")
    suspend fun setState(messageId: String, state: String)

    @Query("SELECT MAX(seq) FROM messages WHERE chatId = :chatId")
    suspend fun maxSeq(chatId: String): Long?

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun byId(id: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE clientId = :clientId LIMIT 1")
    suspend fun byClientId(clientId: String): MessageEntity?
}
