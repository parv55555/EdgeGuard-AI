package com.parv.safetexts

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_context")
data class ChatContextEntity(
    @PrimaryKey
    val chatTitle: String,
    val contextType: String, // e.g., "BOOKING_SCAM_RISK"
    val timestamp: Long
)
