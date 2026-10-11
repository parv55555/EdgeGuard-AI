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
            
            val options = Interpreter.Options()
            interpreter = Interpreter(buffer, options)
            Log.d("TFLiteScamClassifier", "Model loaded successfully")
        } catch (e: Exception) {
            Log.e("TFLiteScamClassifier", "Error loading model: ${e.message}")
        }
    }

    private fun tokenize(text: String): List<Int> {
        val tokens = mutableListOf<Int>()
        tokens.add(vocab["[CLS]"] ?: 101)
        
        val cleanText = text.lowercase().replace(Regex("([^a-z0-9])"), " $1 ")
        val words = cleanText.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        
        for (word in words) {
            var start = 0
            var isBad = false
            val subTokens = mutableListOf<Int>()
            while (start < word.length) {
                var end = word.length
                var subword = ""
                var match: Int? = null
                while (start < end) {
                    subword = if (start == 0) word.substring(start, end) else "##" + word.substring(start, end)
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
                tokens.add(vocab["[UNK]"] ?: 100)
            } else {
                tokens.addAll(subTokens)
            }
        }
        tokens.add(vocab["[SEP]"] ?: 102)
        return tokens
    }

    @Synchronized
    fun classify(text: String): Pair<String, Float> {
        if (interpreter == null) return Pair("Safe", 0.0f)

        val tokenIds = tokenize(text)
        
        val inputIds = ByteBuffer.allocateDirect(maxLen * 4).order(ByteOrder.nativeOrder())
        val attentionMask = ByteBuffer.allocateDirect(maxLen * 4).order(ByteOrder.nativeOrder())
        
        for (i in 0 until maxLen) {
            val token = if (i < tokenIds.size) tokenIds[i] else 0
            val mask = if (i < tokenIds.size) 1 else 0
            inputIds.putInt(token)
            attentionMask.putInt(mask)
        }
        
        inputIds.rewind()
        attentionMask.rewind()

        val inputs = arrayOf<Any>(attentionMask, inputIds)
        
        val output = ByteBuffer.allocateDirect(2 * 4).order(ByteOrder.nativeOrder())
        val outputs = mutableMapOf<Int, Any>()
        outputs[0] = output
        
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
            Log.e("TFLiteScamClassifier", "Inference error: ")
            return Pair("Err: " + e.message?.take(10), 0.0f)
        }
    }
}
