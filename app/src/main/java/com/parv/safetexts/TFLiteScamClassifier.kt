package com.parv.safetexts

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.MappedByteBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class TFLiteScamClassifier(private val context: Context) {
    private var interpreter: Interpreter? = null
    private val vocab = mutableMapOf<String, Int>()
    private val maxLen = 64
    private var clsTokenId = 101
    private var sepTokenId = 102
    private var unkTokenId = 100

    // Reusable buffers to avoid native heap churn and GC pauses
    private val inputIds = ByteBuffer.allocateDirect(maxLen * 4).order(ByteOrder.nativeOrder())
    private val attentionMask = ByteBuffer.allocateDirect(maxLen * 4).order(ByteOrder.nativeOrder())
    private val output = ByteBuffer.allocateDirect(2 * 4).order(ByteOrder.nativeOrder())
    private val outputs = mutableMapOf<Int, Any>(0 to output)
    private val inputs = arrayOf<Any>(attentionMask, inputIds)

    companion object {
        private val PUNCT_REGEX = Regex("([^a-z0-9])")
        private val WHITESPACE_REGEX = Regex("\\s+")
    }

    init {
        loadVocab()
        loadModel()
    }

    private fun loadVocab() {
        try {
            val inputStream = context.assets.open("vocab.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            var index = 0
            reader.forEachLine { line ->
                val token = line.trim()
                if (token.isNotEmpty()) {
                    vocab[token] = index
                }
                index++
            }
            reader.close()
            clsTokenId = vocab["[CLS]"] ?: 101
            sepTokenId = vocab["[SEP]"] ?: 102
            unkTokenId = vocab["[UNK]"] ?: 100
            Log.d("TFLiteScamClassifier", "Loaded ${vocab.size} tokens")
        } catch (e: Exception) {
            Log.e("TFLiteScamClassifier", "Error loading vocab: ${e.message}")
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
            }
            interpreter = Interpreter(buffer, options)
            Log.d("TFLiteScamClassifier", "Model loaded successfully with $numCores threads")
        } catch (e: Exception) {
            Log.e("TFLiteScamClassifier", "Error loading model: ${e.message}")
        }
    }

    private fun tokenize(text: String): List<Int> {
        val tokens = ArrayList<Int>(maxLen)
        tokens.add(clsTokenId)
        
        val cleanText = PUNCT_REGEX.replace(text.lowercase(), " $1 ")
        val words = cleanText.split(WHITESPACE_REGEX)
        
        for (word in words) {
            if (word.isEmpty()) continue
            if (tokens.size >= maxLen - 1) break

            var start = 0
            var isBad = false
            val subTokens = ArrayList<Int>(8)
            while (start < word.length) {
                var end = word.length
                var match: Int? = null
                while (start < end) {
                    val subword = if (start == 0) word.substring(start, end) else "##" + word.substring(start, end)
                    match = vocab[subword]
                    if (match != null) break
                    end--
                }
                if (match == null) {
                    isBad = true
                    break
                }
                subTokens.add(match)
                start = end
            }
            if (isBad) {
                tokens.add(unkTokenId)
            } else {
                tokens.addAll(subTokens)
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
            val token = if (i < tokenIds.size) tokenIds[i] else 0
            val mask = if (i < tokenIds.size) 1 else 0
            inputIds.putInt(token)
            attentionMask.putInt(mask)
        }
        
        inputIds.rewind()
        attentionMask.rewind()

        try {
            interpreter?.runForMultipleInputsOutputs(inputs, outputs)
            output.rewind()
            val safeProb = output.getFloat()
            val scamProb = output.getFloat()
            
            if (scamProb > 0.54f) {
                return Pair("Alert", scamProb)
            }
            return Pair("Safe", scamProb)
        } catch (e: Exception) {
            Log.e("TFLiteScamClassifier", "Inference error: ${e.message}")
            return Pair("Err: " + e.message?.take(10), 0.0f)
        }
    }
}
