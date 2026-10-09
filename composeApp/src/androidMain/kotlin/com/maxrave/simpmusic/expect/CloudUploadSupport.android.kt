package com.maxrave.simpmusic.expect

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import io.ktor.util.cio.toByteReadChannel
import io.ktor.utils.io.ByteReadChannel
import java.security.MessageDigest

/** SAF URI 读取用的全局 context(SimpMusicApplication.onCreate 注入) */
object CloudUploadStore {
    @Volatile
    var appContext: Context? = null
}

actual fun readCloudUploadFile(uri: String): CloudUploadLocalFile? {
    val context = CloudUploadStore.appContext ?: return null
    val resolver = context.contentResolver
    val parsed = Uri.parse(uri)
    return runCatching {
        var name: String? = null
        var size = -1L
        resolver.query(parsed, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIdx >= 0) name = cursor.getString(nameIdx)
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
            }
        }
        val digest = MessageDigest.getInstance("MD5")
        var total = 0L
        resolver.openInputStream(parsed)?.use { input ->
            val buf = ByteArray(1 shl 16)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                digest.update(buf, 0, n)
                total += n
            }
        } ?: return null
        if (size < 0) size = total
        CloudUploadLocalFile(
            fileName = name ?: uri.substringAfterLast('/'),
            sizeBytes = size,
            mimeType = resolver.getType(parsed) ?: "audio/mpeg",
            md5 = digest.digest().joinToString("") { "%02x".format(it) },
        )
    }.getOrNull()
}

actual fun cloudUploadBodyFactory(uri: String): (() -> ByteReadChannel)? {
    val context = CloudUploadStore.appContext ?: return null
    val parsed = Uri.parse(uri)
    return {
        context.contentResolver.openInputStream(parsed)?.toByteReadChannel()
            ?: ByteReadChannel("".encodeToByteArray())
    }
}
