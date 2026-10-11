package com.parv.safetexts

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChatContextDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(chatContext: ChatContextEntity)

    @Query("SELECT * FROM chat_context WHERE chatTitle = :chatTitle LIMIT 1")
    suspend fun getContextForChat(chatTitle: String): ChatContextEntity?

    @Query("DELETE FROM chat_context WHERE timestamp < :threshold")
    suspend fun deleteContextsOlderThan(threshold: Long)
}
