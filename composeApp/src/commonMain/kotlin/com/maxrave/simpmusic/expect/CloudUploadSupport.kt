package com.maxrave.simpmusic.expect

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

/** 上传流工厂:每次调用返回新的整文件流(上传链读 body 一遍);null=打不开 */
expect fun cloudUploadBodyFactory(uri: String): (() -> ByteReadChannel)?
