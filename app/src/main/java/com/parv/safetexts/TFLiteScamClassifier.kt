package com.parv.safetexts

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.Arrays

class TFLiteScamClassifier(private val context: Context) {
    private var interpreter: Interpreter? = null
    private val vocab = HashMap<String, Int>(130000)
    private var scores = FloatArray(116754)
    private val maxLen = 128
    private var clsTokenId = 2
    private var sepTokenId = 3
    private var unkTokenId = 1
    private var padTokenId = 0
    private val optimalThreshold = 0.70f

    // Reusable buffers for zero-allocation inference
    private val inputIds = ByteBuffer.allocateDirect(maxLen * 4).order(ByteOrder.nativeOrder())
    private val attentionMask = ByteBuffer.allocateDirect(maxLen * 4).order(ByteOrder.nativeOrder())
    private val output = ByteBuffer.allocateDirect(2 * 4).order(ByteOrder.nativeOrder())
    private val outputs = mutableMapOf<Int, Any>(0 to output)
    // Model signature: index 0 is attention_mask, index 1 is input_ids
    private val inputs = arrayOf<Any>(attentionMask, inputIds)

    // Reusable scratch buffers for SentencePiece Viterbi DP (max word length ~128 chars)
    private val bestScore = FloatArray(128)
    private val bestPrev = IntArray(128)
    private val bestToken = IntArray(128)

    companion object {
        private val WHITESPACE_REGEX = Regex("\\s+")
        private const val UNK_PENALTY = -100.0f
    }

    init {
        loadVocab()
        loadModel()
    }

    private fun loadVocab() {
        try {
            val inputStream = context.assets.open("vocab.txt")
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8), 65536)
            var index = 0
            var tempScores = FloatArray(120000)

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
            Log.d("TFLiteScamClassifier", "Loaded ${vocab.size} tokens (cls=$clsTokenId, sep=$sepTokenId, unk=$unkTokenId, pad=$padTokenId)")
        } catch (e: Exception) {
            Log.e("TFLiteScamClassifier", "Error loading vocab: ${e.message}", e)
        }
    }

    private fun loadModel() {
        try {
            val fileDescriptor = context.assets.openFd("model.tflite")
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            val buffer: MappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            val numCores = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
            val options = Interpreter.Options().apply {
                setNumThreads(numCores)
                setUseXNNPACK(true)
            }
            interpreter = Interpreter(buffer, options)
            Log.d("TFLiteScamClassifier", "Model loaded successfully with $numCores threads and XNNPACK acceleration")
        } catch (e: Exception) {
            Log.e("TFLiteScamClassifier", "Error loading model: ${e.message}", e)
        }
    }

    /**
     * SentencePiece Unigram Viterbi DP tokenizer matching IndicBERT vocabulary.
     */
    fun tokenize(text: String): List<Int> {
        val tokens = ArrayList<Int>(maxLen)
        tokens.add(clsTokenId)

        val words = text.lowercase().trim().split(WHITESPACE_REGEX)

        for (word in words) {
            if (word.isEmpty()) continue
            if (tokens.size >= maxLen - 1) break

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
                if (bestScore[i] + UNK_PENALTY > bestScore[i + 1] && bestPrev[i + 1] == -1) {
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
        if (interpreter == null) return Pair("Safe", 0.0f)

        val tokenIds = tokenize(text)

        inputIds.clear()
        attentionMask.clear()
        output.clear()

        for (i in 0 until maxLen) {
            val token = if (i < tokenIds.size) tokenIds[i] else padTokenId
            val mask = if (i < tokenIds.size) 1 else 0
            inputIds.putInt(token)
            attentionMask.putInt(mask)
        }

        inputIds.rewind()
        attentionMask.rewind()

        return try {
            interpreter?.runForMultipleInputsOutputs(inputs, outputs)
            output.rewind()
            val safeLogit = output.getFloat()
            val scamLogit = output.getFloat()

            // Numerically stable softmax
            val maxLogit = maxOf(safeLogit, scamLogit)
            val expSafe = Math.exp((safeLogit - maxLogit).toDouble()).toFloat()
            val expScam = Math.exp((scamLogit - maxLogit).toDouble()).toFloat()
            val scamProb = expScam / (expSafe + expScam)

            if (scamProb >= optimalThreshold) {
                Pair("Alert", scamProb)
            } else {
                Pair("Safe", scamProb)
            }
        } catch (e: Exception) {
            Log.e("TFLiteScamClassifier", "Inference error: ${e.message}")
            Pair("Err: " + e.message?.take(10), 0.0f)
        }
    }
}
