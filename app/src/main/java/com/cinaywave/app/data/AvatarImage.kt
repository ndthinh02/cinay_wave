package com.cinaywave.app.data

// File providers can omit or misreport MIME types; use the image's actual header.
internal fun avatarMimeType(bytes: ByteArray): String? {
    fun matches(offset: Int, signature: ByteArray): Boolean =
        bytes.size >= offset + signature.size && signature.indices.all { bytes[offset + it] == signature[it] }

    return when {
        bytes.size >= 4 && matches(0, byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte())) -> "image/jpeg"
        bytes.size >= 33 && matches(0, byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10)) &&
            matches(12, "IHDR".toByteArray(Charsets.US_ASCII)) -> "image/png"
        bytes.size >= 20 && matches(0, "RIFF".toByteArray(Charsets.US_ASCII)) &&
            matches(8, "WEBP".toByteArray(Charsets.US_ASCII)) &&
            listOf("VP8 ", "VP8L", "VP8X").any { matches(12, it.toByteArray(Charsets.US_ASCII)) } -> "image/webp"
        else -> null
    }
}
