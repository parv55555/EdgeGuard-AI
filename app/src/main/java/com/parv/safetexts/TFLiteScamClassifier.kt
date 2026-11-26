package com.parv.safetexts

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.Arrays

class TFLiteScamClassifier(private val context: Context) {
    private var interpreter: Interpreter? = null
    private val vocab = HashMap<String, Int>(130000)
    private val idToToken = ArrayList<String>(120000)
    private var scores = FloatArray(116754)
    private val maxLen = 128
    private var clsTokenId = 2
    private var sepTokenId = 3
    private var unkTokenId = 1
    private var padTokenId = 0
    private val optimalThreshold = 0.70f

    // Reusable direct buffers for zero-allocation, thread-safe inference
    private val inputIds = ByteBuffer.allocateDirect(maxLen * 4).order(ByteOrder.nativeOrder())
    private val attentionMask = ByteBuffer.allocateDirect(maxLen * 4).order(ByteOrder.nativeOrder())
    private val output = ByteBuffer.allocateDirect(2 * 4).order(ByteOrder.nativeOrder())
    private val outputs = mutableMapOf<Int, Any>(0 to output)
    // Model signature: index 0 is attention_mask ([1, 128], INT32), index 1 is input_ids ([1, 128], INT32)
    private val inputs = arrayOf<Any>(attentionMask, inputIds)

    // Reusable scratch buffers for SentencePiece Viterbi DP (max word length ~128 chars)
    private val bestScore = FloatArray(128)
    private val bestPrev = IntArray(128)
    private val bestToken = IntArray(128)

    companion object {
        private const val AUDIT_TAG = "EDGEGUARD_AUDIT"
        private val WHITESPACE_REGEX = Regex("\\s+")
        private const val UNK_PENALTY = -100.0f

        // Normalization regexes per IndicBERT fine-tuning specifications
        private val URL_REGEX = Regex(
            """(?i)\b(?:https?://|www\.)[^\s,!?]+|\b[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}(?:/[^\s,!?]*)?"""
        )
        private val PHONE_REGEX = Regex(
            """(?:\+91[\-\s]?|91[\-\s]?|0)?[6-9](?:\d{9}|\d{4}[\-\s]\d{5}|\d{2}[\-\s]\d{3}[\-\s]\d{5})\b"""
        )

        fun removeEmojis(text: String): String {
            if (text.isEmpty()) return text
            val sb = java.lang.StringBuilder(text.length)
            var i = 0
            val len = text.length
            while (i < len) {
                val codePoint = text.codePointAt(i)
                val charCount = java.lang.Character.charCount(codePoint)
                val isEmoji = when {
                    codePoint >= 0x10000 -> true
                    codePoint in 0x2600..0x27BF -> true
                    codePoint in 0x2300..0x23FF -> true
                    codePoint in 0x2B50..0x2B55 -> true
                    codePoint in 0xFE00..0xFE0F -> true
                    codePoint == 0x200D || codePoint == 0x20E3 -> true
                    codePoint in 0x3030..0x303D -> true
                    codePoint == 0x3297 || codePoint == 0x3299 -> true
                    else -> false
                }
                if (!isEmoji) {
                    sb.appendCodePoint(codePoint)
                } else {
                    sb.append(' ')
                }
                i += charCount
            }
            return sb.toString()
        }

        fun preprocessText(text: String): String {
            val withoutEmojis = removeEmojis(text)
            var processed = URL_REGEX.replace(withoutEmojis, "URL")
            processed = PHONE_REGEX.replace(processed, "PHONE")
            processed = WHITESPACE_REGEX.replace(processed, " ")
            return processed.lowercase().trim()
        }
    }

    init {
        loadVocab()
        loadModel()
        runStartupSelfTest()
    }

    fun loadVocab(inputStream: InputStream? = null) {
        try {
            val stream = inputStream ?: context.assets.open("vocab.txt")
            val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8), 65536)
            var index = 0
            var tempScores = FloatArray(120000)

            vocab.clear()
            idToToken.clear()

            reader.forEachLine { line ->
                val tabIdx = line.indexOf('\t')
                val token: String
                val score: Float
                if (tabIdx != -1) {
                    token = line.substring(0, tabIdx)
                    score = line.substring(tabIdx + 1).toFloatOrNull() ?: 0.0f
                } else {
                    token = line.trim()
                    score = 0.0f
                }
                if (token.isNotEmpty()) {
                    vocab[token] = index
                    idToToken.add(token)
                    if (index >= tempScores.size) {
                        tempScores = tempScores.copyOf(tempScores.size * 2)
                    }
                    tempScores[index] = score
                    index++
                }
            }
            reader.close()
            scores = tempScores.copyOf(index)

            clsTokenId = vocab["[CLS]"] ?: 2
            sepTokenId = vocab["[SEP]"] ?: 3
            unkTokenId = vocab["<unk>"] ?: vocab["[UNK]"] ?: 1
            padTokenId = vocab["<pad>"] ?: 0
            Log.e(AUDIT_TAG, "[VOCAB_LOAD] Successfully loaded ${vocab.size} entries from vocab.txt (cls=$clsTokenId, sep=$sepTokenId, unk=$unkTokenId, pad=$padTokenId)")
        } catch (e: Throwable) {
            Log.e(AUDIT_TAG, "[VOCAB_LOAD_ERROR] Failed loading vocab.txt: ${e.message}", e)
            throw e
        }
    }

    private fun loadModel() {
        try {
            val candidateFiles = listOf("A0908I.tflite", "A0908i.tflite", "edgeguard_indicbert_int8.tflite", "A0908I", "A0908i", "model.tflite")
            var resolvedFile = "A0908I.tflite"
            val assetList = try { context.assets.list("")?.toSet() ?: emptySet() } catch (_: Exception) { emptySet() }
            for (candidate in candidateFiles) {
                if (assetList.contains(candidate)) {
                    resolvedFile = candidate
                    break
                }
            }

            val buffer: ByteBuffer = try {
                val fileDescriptor = context.assets.openFd(resolvedFile)
                val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
                val fileChannel = inputStream.channel
                val startOffset = fileDescriptor.startOffset
                val declaredLength = fileDescriptor.declaredLength
                fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            } catch (fdEx: Exception) {
                Log.e(AUDIT_TAG, "[MODEL_LOAD] openFd failed for '$resolvedFile' (${fdEx.message}), loading via asset stream buffer")
                val bytes = context.assets.open(resolvedFile).use { it.readBytes() }
                val directBuf = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
                directBuf.put(bytes)
                directBuf.rewind()
                directBuf
            }

            val numCores = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
            try {
                val options = Interpreter.Options().apply {
                    setNumThreads(numCores)
                    setUseXNNPACK(true)
                }
                interpreter = Interpreter(buffer, options)
                Log.e(AUDIT_TAG, "[MODEL_LOAD] Active SLM model '$resolvedFile' initialized with $numCores threads and XNNPACK acceleration")
            } catch (xnnEx: Throwable) {
                Log.e(AUDIT_TAG, "[MODEL_LOAD_WARN] XNNPACK failed: ${xnnEx.message}. Retrying with standard CPU threads.", xnnEx)
                val fallbackOptions = Interpreter.Options().apply {
                    setNumThreads(numCores)
                }
                interpreter = Interpreter(buffer, fallbackOptions)
                Log.e(AUDIT_TAG, "[MODEL_LOAD] Active SLM model '$resolvedFile' initialized with $numCores standard CPU threads")
            }
        } catch (e: Throwable) {
            Log.e(AUDIT_TAG, "[MODEL_LOAD_ERROR] Failed loading TFLite model: ${e.message}", e)
            throw e
        }
    }

    /**
     * Automated Startup Self-Test required by EDGEGUARD_AUDIT.
     * Evaluates the standard canonical fraud benchmark string synchronously upon startup.
     */
    private fun runStartupSelfTest() {
        Log.e(AUDIT_TAG, "=================================================================")
        Log.e(AUDIT_TAG, ">>> STARTING EDGEGUARD ACTIVE CLASSIFIER STARTUP SELF-TEST <<<")
        Log.e(AUDIT_TAG, "=================================================================")
        try {
            // 1. Vocab load status
            Log.e(AUDIT_TAG, "[AUDIT 1/5] Vocab load status: ${vocab.size} entries loaded from vocab.txt")
            if (vocab.size < 116000) {
                Log.e(AUDIT_TAG, "[AUDIT WARNING] Expected ~116,754 tokens but found ${vocab.size}")
            }

            // 2. Exact tensor indices verification
            val interp = interpreter ?: throw IllegalStateException("Interpreter is null! Model failed to initialize.")
            val in0 = interp.getInputTensor(0)
            val in1 = interp.getInputTensor(1)
            val out0 = interp.getOutputTensor(0)
            Log.e(AUDIT_TAG, "[AUDIT 2/5] Exact Tensor Indices:")
            Log.e(AUDIT_TAG, "  - Index 0: name='${in0.name()}', shape=${in0.shape().contentToString()}, type=${in0.dataType()} [attention_mask]")
            Log.e(AUDIT_TAG, "  - Index 1: name='${in1.name()}', shape=${in1.shape().contentToString()}, type=${in1.dataType()} [input_ids]")
            Log.e(AUDIT_TAG, "  - Output Index 0: name='${out0.name()}', shape=${out0.shape().contentToString()}, type=${out0.dataType()} [logits [1, 2]]")

            // 3. Tokenizer test on exact string
            val testString = "Alert We have recently observed various malicious activities from your account, to verify yourself give the otp that you recently received. Team SBI"
            Log.e(AUDIT_TAG, "[AUDIT 3/5] Test input string: \"$testString\"")

            val maskedText = preprocessText(testString)
            Log.e(AUDIT_TAG, "  Preprocessed text: \"$maskedText\"")

            val tokenIds = tokenizePreprocessed(maskedText)
            val first10Ids = tokenIds.take(10)
            val first10Tokens = first10Ids.map { idToToken.getOrNull(it) ?: "<unk>" }

            Log.e(AUDIT_TAG, "  First 10 tokens: $first10Tokens")
            Log.e(AUDIT_TAG, "  First 10 input_ids: $first10Ids")

            // Verify tokens are not all 1s / <unk>
            val isAllOnes = first10Ids.drop(1).all { it == unkTokenId }
            if (isAllOnes) {
                Log.e(AUDIT_TAG, "  [CRITICAL ERROR] Tokenizer mapped words to <unk> (array of 1s)!")
            } else {
                Log.e(AUDIT_TAG, "  [TOKENIZER OK] Valid subword pieces segmented (not all 1s).")
            }

            // 4. Run inference
            inputIds.clear()
            attentionMask.clear()
            output.clear()

            for (i in 0 until maxLen) {
                val token = if (i < tokenIds.size) tokenIds[i] else padTokenId
                val mask = if (i < tokenIds.size) 1 else 0
                inputIds.putInt(token)
                attentionMask.putInt(mask)
            }

            // Ensure ByteBuffers are rewound/positioned at 0 before execution
            inputIds.rewind()
            attentionMask.rewind()
            output.rewind()

            interp.runForMultipleInputsOutputs(inputs, outputs)

            output.rewind()
            val safeLogit = output.getFloat()
            val scamLogit = output.getFloat()

            // Numerically stable softmax
            val maxLogit = maxOf(safeLogit, scamLogit)
            val expSafe = Math.exp((safeLogit - maxLogit).toDouble()).toFloat()
            val expScam = Math.exp((scamLogit - maxLogit).toDouble()).toFloat()
            val scamProb = expScam / (expSafe + expScam)

            // 5. Log raw logits and calculated probability
            Log.e(AUDIT_TAG, "[AUDIT 4/5] Raw Logits: safeLogit=$safeLogit, scamLogit=$scamLogit")
            Log.e(AUDIT_TAG, "[AUDIT 5/5] Calculated scam_prob: $scamProb (optimalThreshold=$optimalThreshold)")

            if (scamProb > 0.90f) {
                Log.e(AUDIT_TAG, ">>> AUDIT RESULT: PASSED! High-confidence scam detected (prob=$scamProb > 0.90) <<<")
            } else {
                Log.e(AUDIT_TAG, ">>> AUDIT RESULT: FAILED! scam_prob=$scamProb is NOT > 0.90! <<<")
            }
            Log.e(AUDIT_TAG, "=================================================================")
        } catch (e: Throwable) {
            Log.e(AUDIT_TAG, "[FATAL AUDIT FAILURE] Exception during startup self-test: ${e.message}", e)
            throw e
        }
    }

    /**
     * Tokenize raw or preprocessed text using SentencePiece conventions (words prefixed with '\u2581')
     * and Viterbi dynamic-programming segmentation against the vocab scores.
     * Returns structured list: [CLS] + token IDs + [SEP].
     */
    fun tokenize(text: String): List<Int> {
        val maskedText = preprocessText(text)
        return tokenizePreprocessed(maskedText)
    }

    /**
     * Viterbi dynamic-programming segmentation on preprocessed text.
     */
    fun tokenizePreprocessed(preprocessedText: String): List<Int> {
        val tokens = ArrayList<Int>(maxLen)
        tokens.add(clsTokenId)

        val words = preprocessedText.split(WHITESPACE_REGEX)

        for (word in words) {
            if (word.isEmpty()) continue
            if (tokens.size >= maxLen - 1) break // Reserve space for [SEP]

            var wText = "\u2581$word"
            var n = wText.length
            if (n >= 127) {
                wText = wText.substring(0, 126)
                n = 126
            }

            Arrays.fill(bestScore, 0, n + 1, Float.NEGATIVE_INFINITY)
            Arrays.fill(bestPrev, 0, n + 1, -1)
            Arrays.fill(bestToken, 0, n + 1, -1)
            bestScore[0] = 0.0f

            for (i in 0 until n) {
                if (bestScore[i] == Float.NEGATIVE_INFINITY) continue

                val maxJ = minOf(n + 1, i + 32)
                for (j in (i + 1) until maxJ) {
                    val sub = wText.substring(i, j)
                    val idx = vocab[sub]
                    if (idx != null) {
                        val sc = if (idx < scores.size) scores[idx] else 0.0f
                        if (bestScore[i] + sc > bestScore[j]) {
                            bestScore[j] = bestScore[i] + sc
                            bestPrev[j] = i
                            bestToken[j] = idx
                        }
                    }
                }

                // Fallback unk step if a character cannot be matched
                if (bestScore[i] + UNK_PENALTY > bestScore[i + 1]) {
                    bestScore[i + 1] = bestScore[i] + UNK_PENALTY
                    bestPrev[i + 1] = i
                    bestToken[i + 1] = unkTokenId
                }
            }

            var curr = n
            val wordTokens = ArrayList<Int>(8)
            while (curr > 0) {
                val p = bestPrev[curr]
                val t = bestToken[curr]
                if (p < 0) {
                    wordTokens.add(unkTokenId)
                    curr -= 1
                } else {
                    wordTokens.add(t)
                    curr = p
                }
            }

            for (k in wordTokens.indices.reversed()) {
                tokens.add(wordTokens[k])
                if (tokens.size >= maxLen - 1) break
            }
        }

        tokens.add(sepTokenId)
        return tokens
    }

    @Synchronized
    fun classify(text: String): Pair<String, Float> {
        val interp = interpreter ?: run {
            Log.e(AUDIT_TAG, "[CLASSIFY_ERROR] Active interpreter is null! Cannot classify.")
            return Pair("Safe", 0.0f)
        }

        try {
            val maskedText = preprocessText(text)
            if (maskedText.isEmpty()) {
                return Pair("Safe", 0.0f)
            }
            val tokenIds = tokenizePreprocessed(maskedText)

            inputIds.clear()
            attentionMask.clear()
            output.clear()

            // Structure input arrays to fixed length 128: [CLS] + token IDs + [SEP], padded with 0
            for (i in 0 until maxLen) {
                val token = if (i < tokenIds.size) tokenIds[i] else padTokenId
                val mask = if (i < tokenIds.size) 1 else 0
                inputIds.putInt(token)
                attentionMask.putInt(mask)
            }

            // Ensure ByteBuffers are rewound/positioned at 0 before execution
            inputIds.rewind()
            attentionMask.rewind()
            output.rewind()

            // Tensor order: Index 0: attention_mask ([1, 128], INT32), Index 1: input_ids ([1, 128], INT32)
            interp.runForMultipleInputsOutputs(inputs, outputs)

            output.rewind()
            val safeLogit = output.getFloat()
            val scamLogit = output.getFloat()

            // Numerically stable softmax across raw logits [1, 2]
            val maxLogit = maxOf(safeLogit, scamLogit)
            val expSafe = Math.exp((safeLogit - maxLogit).toDouble()).toFloat()
            val expScam = Math.exp((scamLogit - maxLogit).toDouble()).toFloat()
            val scamProb = expScam / (expSafe + expScam)

            val first10 = tokenIds.take(10)
            Log.e(AUDIT_TAG, "[INFERENCE] '${maskedText.take(35)}...': first10=$first10, safe=$safeLogit, scam=$scamLogit, scamProb=$scamProb")

            return if (scamProb >= optimalThreshold) {
                Pair("Alert", scamProb)
            } else {
                Pair("Safe", scamProb)
            }
        } catch (e: Throwable) {
            Log.e(AUDIT_TAG, "[CLASSIFY_FATAL] Exception during classification: ${e.message}", e)
            throw e
        }
    }
}
