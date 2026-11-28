package com.parv.safetexts

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.parv.safetexts.ui.theme.EdgeGuardAITheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ScanOverlayUiState {
    data class Loading(val isPage: Boolean, val targetSnippet: String) : ScanOverlayUiState
    data class Flagged(val isPage: Boolean, val title: String, val indicator: String, val detail: String) : ScanOverlayUiState
    data class Clean(val isPage: Boolean, val title: String, val detail: String, val targetSnippet: String) : ScanOverlayUiState
}

class ScanOverlayActivity : ComponentActivity() {

    private val urlRegex = Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE)
    private var uiState by mutableStateOf<ScanOverlayUiState>(ScanOverlayUiState.Loading(true, ""))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setupContent()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun setupContent() {
        setContent {
            EdgeGuardAITheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            finish()
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                            }
                    ) {
                        ScanOverlayBottomSheet(
                            state = uiState,
                            onClose = { finish() }
                        )
                    }
                }
            }
        }
    }

    private fun handleIntent(incomingIntent: Intent?) {
        if (incomingIntent == null) {
            finish()
            return
        }

        val action = incomingIntent.action
        val sharedText = incomingIntent.getStringExtra(Intent.EXTRA_TEXT)
        val dataUriString = incomingIntent.dataString ?: incomingIntent.data?.toString()

        if (action == Intent.ACTION_SEND && !sharedText.isNullOrBlank()) {
            val matchedUrl = urlRegex.find(sharedText)?.value
            if (matchedUrl != null) {
                runPageScan(matchedUrl)
            } else {
                runMessageScan(sharedText)
            }
            return
        }

        val directUrl = dataUriString ?: sharedText
        if (!directUrl.isNullOrBlank()) {
            val matchedUrl = urlRegex.find(directUrl)?.value ?: directUrl
            runPageScan(matchedUrl)
            return
        }

        finish()
    }

    private fun runPageScan(rawUrl: String) {
        val snippet = try {
            Uri.parse(rawUrl).host ?: rawUrl
        } catch (_: Exception) {
            rawUrl
        }

        uiState = ScanOverlayUiState.Loading(isPage = true, targetSnippet = snippet)

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                LinkShieldScanner.scan(applicationContext, rawUrl)
            }

            if (result.isScam) {
                uiState = ScanOverlayUiState.Flagged(
                    isPage = true,
                    title = "This looks like it's a scam",
                    indicator = snippet,
                    detail = result.reason
                )
            } else {
                uiState = ScanOverlayUiState.Clean(
                    isPage = true,
                    title = "This looks safe",
                    detail = "No deceptive patterns, phishing forms, or executable threats detected.",
                    targetSnippet = snippet
                )
            }
        }
    }

    private fun runMessageScan(rawText: String) {
        val snippet = if (rawText.length > 60) rawText.take(57) + "..." else rawText
        uiState = ScanOverlayUiState.Loading(isPage = false, targetSnippet = snippet)

        lifecycleScope.launch {
            val cleanText = FraudDetectionEngine.cleanText(rawText)
            val fast = ScamAnalyzer.checkFastDeterministicScam(cleanText)

            if (fast != null) {
                uiState = ScanOverlayUiState.Flagged(
                    isPage = false,
                    title = "This looks like it's a scam",
                    indicator = "High-Risk Threat Indicators",
                    detail = "Message contains deceptive financial, banking, or threat keywords."
                )
                return@launch
            }

            val (label, score) = withContext(Dispatchers.IO) {
                ScamAnalyzer.getInstance(applicationContext).classifyText(cleanText)
            }

            if (label == "Alert" || score >= 0.70f) {
                uiState = ScanOverlayUiState.Flagged(
                    isPage = false,
                    title = "This looks like it's a scam",
                    indicator = "Detected Scam Pattern",
                    detail = "Message text matches social engineering or cyber fraud tactics."
                )
            } else {
                uiState = ScanOverlayUiState.Clean(
                    isPage = false,
                    title = "This looks safe",
                    detail = "No deceptive keywords or cyber scam patterns detected.",
                    targetSnippet = snippet
                )
            }
        }
    }
}

@Composable
private fun ScanOverlayBottomSheet(
    state: ScanOverlayUiState,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            when (state) {
                is ScanOverlayUiState.Loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = if (state.isPage) "Scanning page on-device..." else "Scanning message on-device...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (state.targetSnippet.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = state.targetSnippet,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Text(
                        text = "100% on-device AI analysis • Zero cloud upload",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                is ScanOverlayUiState.Flagged -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = state.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            if (state.indicator.isNotBlank()) {
                                Text(
                                    text = state.indicator,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Text(
                                text = state.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Close & Stay Safe",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                is ScanOverlayUiState.Clean -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Safe",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = state.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            if (state.targetSnippet.isNotBlank()) {
                                Text(
                                    text = state.targetSnippet,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Text(
                                text = state.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Done",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
