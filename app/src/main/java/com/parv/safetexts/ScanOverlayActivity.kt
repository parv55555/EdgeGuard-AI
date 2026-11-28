package com.parv.safetexts

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
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
    data class Flagged(
        val isPage: Boolean,
        val title: String,
        val indicator: String,
        val detail: String,
        val isPaymentAdvisory: Boolean = false
    ) : ScanOverlayUiState
    data class Clean(val isPage: Boolean, val title: String, val detail: String, val targetSnippet: String) : ScanOverlayUiState
}

class ScanOverlayActivity : ComponentActivity() {

    private val urlRegex = Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE)
    private var uiState by mutableStateOf<ScanOverlayUiState>(ScanOverlayUiState.Loading(true, ""))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                ScanOverlayScreen(
                    state = uiState,
                    onFinish = { finish() }
                )
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

            if (result.isPaymentAdvisory) {
                uiState = ScanOverlayUiState.Flagged(
                    isPage = true,
                    title = "Unknown website",
                    indicator = snippet,
                    detail = "Unknown website, verify the originality of website before paying",
                    isPaymentAdvisory = true
                )
            } else if (result.isScam) {
                uiState = ScanOverlayUiState.Flagged(
                    isPage = true,
                    title = "This looks like it's a scam",
                    indicator = snippet,
                    detail = result.reason,
                    isPaymentAdvisory = false
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanOverlayScreen(
    state: ScanOverlayUiState,
    onFinish: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onFinish,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1E24),
        scrimColor = Color.Transparent,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color(0xFF71717A)
            )
        }
    ) {
        ScanOverlayContent(
            state = state,
            onClose = {
                scope.launch {
                    sheetState.hide()
                    onFinish()
                }
            }
        )
    }
}

@Composable
private fun ScanOverlayContent(
    state: ScanOverlayUiState,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                        color = Color(0xFF6366F1)
                    )
                    Column {
                        Text(
                            text = if (state.isPage) "Scanning page on-device..." else "Scanning message on-device...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (state.targetSnippet.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = state.targetSnippet,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Text(
                    text = "100% on-device AI analysis • Zero cloud upload",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF818CF8)
                )
            }

            is ScanOverlayUiState.Flagged -> {
                val isAdvisory = state.isPaymentAdvisory
                val themeColor = if (isAdvisory) Color(0xFFF59E0B) else Color(0xFFEF4444)
                val cardBg = if (isAdvisory) Color(0xFF261E14) else Color(0xFF2B1416)
                val btnColor = if (isAdvisory) Color(0xFFD97706) else Color(0xFFDC2626)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = if (isAdvisory) "Advisory" else "Warning",
                        tint = themeColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = themeColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, themeColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (state.indicator.isNotBlank()) {
                            Text(
                                text = state.indicator,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Text(
                            text = state.detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFF1F5F9),
                            lineHeight = 20.sp
                        )
                    }
                }

                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = btnColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "Close & Stay Safe",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
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
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F241A),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (state.targetSnippet.isNotBlank()) {
                            Text(
                                text = state.targetSnippet,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Text(
                            text = state.detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFF1F5F9),
                            lineHeight = 20.sp
                        )
                    }
                }

                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF059669),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "Done",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
