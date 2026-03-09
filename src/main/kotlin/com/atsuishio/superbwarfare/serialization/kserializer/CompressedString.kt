package com.atsuishio.superbwarfare.serialization.kserializer

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

typealias CompressedString = @Serializable(CompressedStringSerializer::class) String

object CompressedStringSerializer : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("CompressedString", PrimitiveKind.STRING)

    private fun compress(data: ByteArray): ByteArray {
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).use { output ->
            output.write(data)
            output.finish()
        }
        return outputStream.toByteArray()
    }

    private fun decompress(compressedData: ByteArray): ByteArray {
        val outputStream = ByteArrayOutputStream()

        GZIPInputStream(ByteArrayInputStream(compressedData)).use { input ->
            val buffer = ByteArray(1024)
            var len: Int
            while ((input.read(buffer).also { len = it }) != -1) {
                outputStream.write(buffer, 0, len)
            }
        }
        return outputStream.toByteArray()
    }

    private val CACHE = CacheBuilder.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(object : CacheLoader<String, ByteArray>() {
            override fun load(str: String): ByteArray {
                return compress(str.toByteArray())
            }
        })

    override fun serialize(encoder: Encoder, value: String) {
        val compressed = CACHE.getUnchecked(value)

        encoder.encodeInt(compressed.size)
        compressed.forEach {
            encoder.encodeByte(it)
        }
    }

    override fun deserialize(decoder: Decoder): String {
        val size = decoder.decodeInt()
        val bytes = ByteArray(size)

        repeat(size) { index ->
            bytes[index] = decoder.decodeByte()
        }

        return String(decompress(bytes))
    }
}