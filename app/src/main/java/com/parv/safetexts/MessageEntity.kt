// programmed by parv
package com.parv.safetexts

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String,
    val content: String,
    val appName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val analysisResult: String = "Pending",
    val scamProbability: Float = 0f
)
