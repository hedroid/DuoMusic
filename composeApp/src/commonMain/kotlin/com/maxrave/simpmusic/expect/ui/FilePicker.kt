package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

interface FilePickerLauncher {
    fun launch()
}

@Composable
expect fun filePickerResult(
    mimeType: String,
    onResultUri: (String?) -> Unit,
): FilePickerLauncher

/** 多选文件(云盘批量上传用):Android=OpenMultipleDocuments;桌面 no-op 回空 */
@Composable
expect fun multiFilePickerResult(
    mimeType: String,
    onResultUris: (List<String>) -> Unit,
): FilePickerLauncher

@Composable
expect fun fileSaverResult(
    fileName: String,
    mimeType: String,
    onResultUri: (String?) -> Unit,
): FilePickerLauncher
@Composable
expect fun directoryPickerResult(onResultUri: (String?) -> Unit): FilePickerLauncher
