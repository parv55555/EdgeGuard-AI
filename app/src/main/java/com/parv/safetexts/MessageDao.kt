// programmed by parv
package com.parv.safetexts

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT COUNT(*) FROM messages WHERE sender = :sender AND content = :content")
    suspend fun countMessages(sender: String, content: String): Int

    @Query("SELECT * FROM messages WHERE sender = :sender AND content = :content LIMIT 1")
    suspend fun getMessage(sender: String, content: String): MessageEntity?

    @Insert
    suspend fun insertMessage(message: MessageEntity): Long

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("DELETE FROM messages")
    suspend fun deleteAll()

    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE timestamp < :threshold")
    suspend fun deleteMessagesOlderThan(threshold: Long)

    @Query("DELETE FROM messages WHERE LOWER(content) IN ('settings', 'account', 'privacy', 'avatar', 'chats', 'notifications', 'storage and data', 'app language', 'help', 'invite a friend', 'wallpaper', 'media, links, and docs', 'search', 'mute notifications', 'disappearing messages', 'more', 'view contact', 'clear chat', 'export chat', 'add shortcut', 'security notifications', 'two-step verification', 'change number', 'request account info', 'delete my account', 'theme', 'chat backup', 'chat history') OR LOWER(sender) IN ('screen reader', 'settings', 'whatsapp', 'chats') OR LOWER(content) LIKE '%security notifications, change number%' OR LOWER(content) LIKE '%theme, wallpapers%' OR LOWER(content) LIKE '%network usage, auto-download%' OR LOWER(content) LIKE '%help center, contact us%' OR LOWER(content) LIKE '%block contacts, disappearing messages%' OR LOWER(content) LIKE '%message, group & call tones%' OR LOWER(content) LIKE '%fake website: google.com%'")
    suspend fun deleteJunkUiMessages()
}
