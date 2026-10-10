package com.maxrave.simpmusic.expect

import androidx.compose.ui.graphics.ImageBitmap
import io.ktor.utils.io.ByteReadChannel

/** 待上传本地文件的元数据(md5 由平台层流式算好,大文件不进内存) */
data class CloudUploadLocalFile(
    val fileName: String,
    val sizeBytes: Long,
    val mimeType: String,
    val md5: String,
)

/**
 * 读取待上传文件元数据+md5(IO 密集,调用方负责线程);null=URI 读不到
 * (失效/权限丢失/平台不支持)。桌面 picker no-op,实际只在 Android 走到。
 */
expect fun readCloudUploadFile(uri: String): CloudUploadLocalFile?

/**
 * 轻量文件名读取(上传队列行显示用):只查显示名,不读文件内容不算 md5——
 * 整队入列要立刻拿到名字,重活(readCloudUploadFile)留在逐首上传时做。
 */
expect fun readCloudUploadFileName(uri: String): String?

/**
 * 读文件内嵌封面(上传队列行显示用,Android=MediaMetadataRetriever 的 embedded
 * picture,只解析头部元数据);null=无内嵌封面/读取失败,行内退云朵兜底。
 * 返回值已按行内显示尺寸采样(≤256px)且平台层带 LruCache 限量缓存——IO 密集,
 * 调用方必须在 IO 线程/协程里调。桌面 picker no-op,恒 null。
 */
expect fun readCloudUploadArtwork(uri: String): ImageBitmap?

/** 上传流工厂:每次调用返回新的整文件流(上传链读 body 一遍);null=打不开 */
expect fun cloudUploadBodyFactory(uri: String): (() -> ByteReadChannel)?
