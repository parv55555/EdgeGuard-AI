package com.parv.safetexts

import org.junit.Assert.*
import org.junit.Test
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.Arrays

class TokenizerUnitTest {

    @Test
    fun testPreprocessingUrlAndPhone() {
        val rawMessage = "Your SBI account is blocked! Click http://bit.ly/sbi-kyc to update or call +919876543210 immediately."
        val preprocessed = TFLiteScamClassifier.preprocessText(rawMessage)

        assertTrue("URL must be normalized to 'url'", preprocessed.contains("url"))
        assertTrue("Phone number must be normalized to 'phone'", preprocessed.contains("phone"))
        assertFalse("Raw URL must not remain", preprocessed.contains("http://"))
        assertFalse("Raw phone number must not remain", preprocessed.contains("9876543210"))
        assertEquals("Text must be lowercase", preprocessed, preprocessed.lowercase())
    }

    @Test
    fun testPreprocessingVariousPhoneFormats() {
        val test1 = TFLiteScamClassifier.preprocessText("Call +91 98765 43210 immediately")
        assertTrue("Formatted +91 space number must be normalized to phone", test1.contains("phone"))

        val test2 = TFLiteScamClassifier.preprocessText("Call 09876543210 now")
        assertTrue("0-prefixed number must be normalized to phone", test2.contains("phone"))

        val test3 = TFLiteScamClassifier.preprocessText("Helpline: 98765-43210")
        assertTrue("Hyphenated number must be normalized to phone", test3.contains("phone"))
    }

    @Test
    fun testVocabTabSeparatedLoading() {
        val dummyVocab = """
            <pad>	0.000000
            <unk>	0.000000
            [CLS]	0.000000
            [SEP]	0.000000
            [MASK]	0.000000
            ▁.	-3.514260
            ▁the	-5.234537
            ▁account	-6.100000
            ▁blocked	-7.200000
            url	-14.090066
            phone	-13.120926
        """.trimIndent()

        // Verify splitting logic on tab-separated lines
        val lines = dummyVocab.lines()
        val vocabMap = HashMap<String, Int>()
        val scoreList = ArrayList<Float>()

        lines.forEachIndexed { index, line ->
            val tabIdx = line.indexOf('\t')
            assertTrue("Line must contain tab separator", tabIdx != -1)
            val token = line.substring(0, tabIdx)
            val score = line.substring(tabIdx + 1).toFloat()
            vocabMap[token] = index
            scoreList.add(score)
        }

        assertEquals(0, vocabMap["<pad>"])
        assertEquals(1, vocabMap["<unk>"])
        assertEquals(2, vocabMap["[CLS]"])
        assertEquals(3, vocabMap["[SEP]"])
        assertEquals(4, vocabMap["[MASK]"])
        assertEquals(7, vocabMap["▁account"])
        assertEquals(8, vocabMap["▁blocked"])
        assertEquals(9, vocabMap["url"])
        assertEquals(10, vocabMap["phone"])
        assertEquals(-5.234537f, scoreList[6], 0.0001f)
    }

    @Test
    fun testActualVocabFileAndBenchmarkTokenization() {
        val vocabFile = File("src/main/assets/vocab.txt")
        if (!vocabFile.exists()) return

        val vocab = HashMap<String, Int>(130000)
        val idToToken = ArrayList<String>(120000)
        val scores = FloatArray(116754)
        var index = 0

        BufferedReader(InputStreamReader(FileInputStream(vocabFile), Charsets.UTF_8), 65536).use { reader ->
            reader.forEachLine { line ->
                val tabIdx = line.indexOf('\t')
                if (tabIdx != -1) {
                    val token = line.substring(0, tabIdx)
                    val score = line.substring(tabIdx + 1).toFloatOrNull() ?: 0.0f
                    vocab[token] = index
                    idToToken.add(token)
                    if (index < scores.size) {
                        scores[index] = score
                    }
                    index++
                }
            }
        }

        assertEquals("Vocab count must be exactly 116,754", 116754, vocab.size)
        assertEquals(0, vocab["<pad>"])
        assertEquals(1, vocab["<unk>"])
        assertEquals(2, vocab["[CLS]"])
        assertEquals(3, vocab["[SEP]"])

        // Test string from EDGEGUARD_AUDIT
        val testString = "Alert We have recently observed various malicious activities from your account, to verify yourself give the otp that you recently received. Team SBI"
        val preprocessed = TFLiteScamClassifier.preprocessText(testString)

        val tokens = ArrayList<Int>(128)
        tokens.add(2) // [CLS]

        val bestScore = FloatArray(128)
        val bestPrev = IntArray(128)
        val bestToken = IntArray(128)
        val words = preprocessed.split(Regex("\\s+"))

        for (word in words) {
            if (word.isEmpty()) continue
            if (tokens.size >= 127) break

            var wText = "\u2581$word"
            var n = minOf(wText.length, 126)
            if (wText.length > 126) wText = wText.substring(0, 126)

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
                if (bestScore[i] - 100.0f > bestScore[i + 1]) {
                    bestScore[i + 1] = bestScore[i] - 100.0f
                    bestPrev[i + 1] = i
                    bestToken[i + 1] = 1
                }
            }

            var curr = n
            val wordTokens = ArrayList<Int>(8)
            while (curr > 0) {
                val p = bestPrev[curr]
                val t = bestToken[curr]
                if (p < 0) {
                    wordTokens.add(1)
                    curr -= 1
                } else {
                    wordTokens.add(t)
                    curr = p
                }
            }
            for (k in wordTokens.indices.reversed()) {
                tokens.add(wordTokens[k])
                if (tokens.size >= 127) break
            }
        }
        tokens.add(3) // [SEP]

        val first10Ids = tokens.take(10)
        val first10Tokens = first10Ids.map { idToToken.getOrNull(it) ?: "<unk>" }

        println("[TEST AUDIT] First 10 tokens: $first10Tokens")
        println("[TEST AUDIT] First 10 input_ids: $first10Ids")

        assertEquals(listOf(2, 13046, 325, 106, 3409, 12691, 2032, 91465, 5761, 88), first10Ids)
        assertFalse("Tokens must NOT be all 1s", first10Ids.drop(1).all { it == 1 })
    }
}
