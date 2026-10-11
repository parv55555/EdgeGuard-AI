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
}
