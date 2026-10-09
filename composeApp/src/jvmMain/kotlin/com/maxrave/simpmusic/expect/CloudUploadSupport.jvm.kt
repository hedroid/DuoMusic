package com.maxrave.simpmusic.expect

import io.ktor.util.cio.toByteReadChannel
import io.ktor.utils.io.ByteReadChannel
import java.io.File
import java.security.MessageDigest

// 桌面端 picker 是 no-op(filePickerResult 直接回 null),上传入口实际到不了这里;
// 按"uri=本地文件路径"给个可用兜底,真接桌面 picker 时无需改。

actual fun readCloudUploadFile(uri: String): CloudUploadLocalFile? {
    val file = File(uri)
    if (!file.exists() || !file.isFile) return null
    return runCatching {
        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buf = ByteArray(1 shl 16)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                digest.update(buf, 0, n)
            }
        }
        CloudUploadLocalFile(
            fileName = file.name,
            sizeBytes = file.length(),
            mimeType = "audio/mpeg",
            md5 = digest.digest().joinToString("") { "%02x".format(it) },
        )
    }.getOrNull()
}

actual fun readCloudUploadFileName(uri: String): String? = File(uri).name

actual fun cloudUploadBodyFactory(uri: String): (() -> ByteReadChannel)? =
    File(uri).takeIf { it.isFile }?.let { file ->
        { file.inputStream().toByteReadChannel() }
    }
