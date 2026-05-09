package com.sillyandroid.app.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.sillyandroid.app.data.entity.CharacterEntity
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

/**
 * PNG 角色卡导入导出处理器。
 *
 * SillyTavern 生态的 PNG 角色卡将 JSON 数据嵌入 PNG 的 tEXt chunk：
 *   - keyword "chara" → TavernAI / SillyTavern v1-2
 *   - keyword "ccv3"  → Character Card v3
 */
object PngCardHandler {

    private const val PNG_SIGNATURE: Long = 0x89504E470D0A1A0AL
    private val gson = Gson()

    // ==================== IMPORT ====================

    /**
     * 从 URI 读取 PNG 文件，提取内嵌的字符卡 JSON。
     *
     * @return CharacterEntity 如果成功，null 如果文件不是有效的 PNG 角色卡
     */
    suspend fun importFromPng(context: Context, uri: Uri): CharacterEntity? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.readBytes()
            inputStream.close()

            val json = extractCharaJson(bytes)
                ?: extractCcv3Json(bytes)
                ?: return null

            CharacterImporter.parseCharacterCard(json)
        } catch (e: Exception) {
            null
        }
    }

    // ==================== EXPORT ====================

    /**
     * 将角色导出为 PNG 角色卡文件。
     *
     * @param character 要导出的角色
     * @param context Android Context
     * @return File 临时 PNG 文件
     */
    fun exportToPng(character: CharacterEntity, context: Context): File {
        val json = CharacterImporter.exportToJson(character)

        // Create a simple card bitmap
        val bitmap = createCardBitmap(character.name)

        // Encode PNG with embedded tEXt chunk
        val outputFile = File(context.cacheDir, "${character.name}_card.png")
        writePngWithChara(bitmap, json, outputFile)
        bitmap.recycle()

        return outputFile
    }

    // ==================== PNG Binary Parsing ====================

    data class PngChunk(
        val type: String,
        val data: ByteArray
    )

    /**
     * 解析 PNG 文件为 chunk 列表。
     */
    fun parsePngChunks(bytes: ByteArray): List<PngChunk> {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

        // Verify signature
        val signature = buffer.getLong()
        if (signature != PNG_SIGNATURE) throw IllegalArgumentException("Not a valid PNG file")

        val chunks = mutableListOf<PngChunk>()

        while (buffer.hasRemaining()) {
            val length = buffer.getInt()
            if (length < 0 || buffer.remaining() < length + 8) break

            val typeBytes = ByteArray(4)
            buffer.get(typeBytes)
            val type = String(typeBytes, Charsets.US_ASCII)

            val data = ByteArray(length)
            buffer.get(data)

            // Skip CRC (4 bytes)
            buffer.getInt()

            chunks.add(PngChunk(type, data))

            if (type == "IEND") break
        }

        return chunks
    }

    /**
     * 读取 tEXt chunk 中指定 keyword 的值。
     */
    fun readTextChunk(data: ByteArray, keyword: String): String? {
        // tEXt chunk format: keyword\0text
        val nullIndex = data.indexOf(0)
        if (nullIndex <= 0) return null

        val foundKeyword = String(data, 0, nullIndex, Charsets.US_ASCII)
        if (foundKeyword != keyword) return null

        return String(data, nullIndex + 1, data.size - nullIndex - 1, Charsets.UTF_8)
    }

    private fun extractCharaJson(bytes: ByteArray): String? {
        return try {
            val chunks = parsePngChunks(bytes)
            for (chunk in chunks) {
                if (chunk.type == "tEXt") {
                    val json = readTextChunk(chunk.data, "chara")
                    if (json != null) return json
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun extractCcv3Json(bytes: ByteArray): String? {
        return try {
            val chunks = parsePngChunks(bytes)
            for (chunk in chunks) {
                if (chunk.type == "tEXt") {
                    val data = readTextChunk(chunk.data, "ccv3")
                    if (data != null) {
                        // ccv3 is base64-encoded
                        return try {
                            String(android.util.Base64.decode(data, android.util.Base64.DEFAULT), Charsets.UTF_8)
                        } catch (e: Exception) {
                            data // Fallback: try as raw JSON
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    // ==================== PNG Binary Writing ====================

    /**
     * 创建一张简单的角色卡位图。
     */
    private fun createCardBitmap(name: String): Bitmap {
        val width = 400
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background gradient-like solid
        canvas.drawColor(Color.parseColor("#1C1B1F"))

        // Top bar
        val topPaint = Paint().apply {
            color = Color.parseColor("#6750A4")
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 80f, topPaint)

        // Character name
        val namePaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText(name, 20f, 55f, namePaint)

        // Card body
        val labelPaint = Paint().apply {
            color = Color.parseColor("#D0BCFF")
            textSize = 14f
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = Color.parseColor("#E6E1E5")
            textSize = 14f
            isAntiAlias = true
        }

        canvas.drawText("角色卡 Character Card", 20f, 110f, labelPaint)
        canvas.drawText("SillyAndroid", 20f, 140f, textPaint)
        canvas.drawText("───────────────", 20f, 180f, textPaint)
        canvas.drawText("此 PNG 包含完整角色数据 (tEXt/chara)", 20f, 220f, textPaint)
        canvas.drawText("可在 SillyTavern 中直接导入", 20f, 250f, textPaint)

        return bitmap
    }

    /**
     * 将 Bitmap + JSON 写入 PNG 文件，嵌入 tEXt/chara chunk。
     */
    private fun writePngWithChara(bitmap: Bitmap, json: String, outputFile: File) {
        val output = ByteArrayOutputStream()

        // Convert bitmap to raw RGBA pixel data
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // PNG Signature
        output.write(longToBytes(PNG_SIGNATURE))

        // IHDR chunk
        val ihdrData = ByteArrayOutputStream()
        ihdrData.write(intToBytes(width))
        ihdrData.write(intToBytes(height))
        ihdrData.write(8)  // bit depth = 8 (RGBA)
        ihdrData.write(6)  // color type = RGBA
        ihdrData.write(0)  // compression
        ihdrData.write(0)  // filter
        ihdrData.write(0)  // interlace
        writePngChunk(output, "IHDR", ihdrData.toByteArray())

        // tEXt chunk: keyword "chara" + \0 + JSON
        val textData = ByteArrayOutputStream()
        textData.write("chara".toByteArray(Charsets.US_ASCII))
        textData.write(0)
        textData.write(json.toByteArray(Charsets.UTF_8))
        writePngChunk(output, "tEXt", textData.toByteArray())

        // tEXt chunk: keyword "ccv3" + \0 + base64(JSON) — for CCv3 compatibility
        val ccv3Json = android.util.Base64.encodeToString(
            json.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP
        )
        val ccv3Data = ByteArrayOutputStream()
        ccv3Data.write("ccv3".toByteArray(Charsets.US_ASCII))
        ccv3Data.write(0)
        ccv3Data.write(ccv3Json.toByteArray(Charsets.UTF_8))
        writePngChunk(output, "tEXt", ccv3Data.toByteArray())

        // IDAT chunk: compressed image data
        val rawData = ByteArrayOutputStream()
        for (y in 0 until height) {
            rawData.write(0) // filter byte (None)
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                rawData.write((pixel shr 16) and 0xFF) // R
                rawData.write((pixel shr 8) and 0xFF)  // G
                rawData.write(pixel and 0xFF)           // B
                rawData.write((pixel shr 24) and 0xFF)  // A
            }
        }

        val compressedData = ByteArrayOutputStream()
        val deflater = java.util.zip.DeflaterOutputStream(compressedData)
        deflater.write(rawData.toByteArray())
        deflater.close()

        writePngChunk(output, "IDAT", compressedData.toByteArray())

        // IEND chunk
        writePngChunk(output, "IEND", ByteArray(0))

        // Write to file
        FileOutputStream(outputFile).use { fos ->
            fos.write(output.toByteArray())
        }
    }

    private fun writePngChunk(output: ByteArrayOutputStream, type: String, data: ByteArray) {
        output.write(intToBytes(data.size))
        output.write(type.toByteArray(Charsets.US_ASCII))
        output.write(data)

        // CRC32 of type + data
        val crc = CRC32()
        crc.update(type.toByteArray(Charsets.US_ASCII))
        crc.update(data)
        output.write(intToBytes(crc.value.toInt()))
    }

    private fun intToBytes(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(value).array()
    }

    private fun longToBytes(value: Long): ByteArray {
        return ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(value).array()
    }
}
