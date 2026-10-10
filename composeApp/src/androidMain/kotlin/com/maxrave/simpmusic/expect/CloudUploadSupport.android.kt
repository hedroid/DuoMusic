package com.maxrave.simpmusic.expect

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.asImageBitmap
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

actual fun readCloudUploadFileName(uri: String): String? {
    val context = CloudUploadStore.appContext ?: return null
    return runCatching {
        context.contentResolver
            .query(Uri.parse(uri), null, null, null, null)
            ?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIdx >= 0) cursor.getString(nameIdx) else null
            }
            ?: uri.substringAfterLast('/')
    }.getOrNull() ?: uri.substringAfterLast('/')
}

/**
 * 内嵌封面解码的采样上限(px):上传队列行的封面槽只有 48dp(@3x≈144px),256px
 * 已留足余量。内嵌封面常见 1000~3000px,全尺寸解码一张 3000²≈36MB——批量入列时
 * 逐张叠加直接把堆打爆,必须按行内显示尺寸采样。
 */
private const val CLOUD_UPLOAD_ARTWORK_MAX_PX = 256

/**
 * 封面位图缓存(按 uri):队列行改行内按需解析后,这里限制同时存活的位图数
 * (24 张 × 256² ARGB ≈ 6MB 上限);滚动回看命中缓存,超出 LRU 逐出后重解。
 * LruCache 自带线程安全,produceState 的 IO 协程直接用。
 */
private val cloudArtworkCache =
    android.util.LruCache<String, androidx.compose.ui.graphics.ImageBitmap>(24)

actual fun readCloudUploadArtwork(uri: String): androidx.compose.ui.graphics.ImageBitmap? {
    val context = CloudUploadStore.appContext ?: return null
    cloudArtworkCache.get(uri)?.let { return it }
    return runCatching {
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, android.net.Uri.parse(uri))
            val picture = retriever.embeddedPicture
            if (picture == null) {
                null
            } else {
                // 两遍解码:先量尺寸算 inSampleSize,再按行内显示档位采样解码
                val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeByteArray(picture, 0, picture.size, bounds)
                var sample = 1
                while (
                    bounds.outWidth / (sample * 2) >= CLOUD_UPLOAD_ARTWORK_MAX_PX ||
                    bounds.outHeight / (sample * 2) >= CLOUD_UPLOAD_ARTWORK_MAX_PX
                ) {
                    sample *= 2
                }
                val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
                android.graphics.BitmapFactory
                    .decodeByteArray(picture, 0, picture.size, opts)
                    ?.asImageBitmap()
            }
        } finally {
            retriever.release()
        }
    }.getOrNull()?.also { cloudArtworkCache.put(uri, it) }
}

actual fun cloudUploadBodyFactory(uri: String): (() -> ByteReadChannel)? {
    val context = CloudUploadStore.appContext ?: return null
    val parsed = Uri.parse(uri)
    return {
        context.contentResolver.openInputStream(parsed)?.toByteReadChannel()
            ?: ByteReadChannel("".encodeToByteArray())
    }
}
