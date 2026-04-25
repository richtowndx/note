package com.videonote.presentation.ui.filepicker

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 本地文件选择器组件
 * 用于选择本地的Markdown/TXT文件并读取其内容
 *
 * @param onFileSelected 文件选择成功回调，返回文件名、路径和内容
 * @param onError 错误回调，返回错误信息
 * @param onDismiss 取消回调
 */
@Composable
fun LocalFilePickerDialog(
    onFileSelected: (fileName: String, filePath: String, content: String) -> Unit,
    onError: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // 获取文件名
                val fileName = getFileName(context, uri)
                val filePath = uri.toString()

                // 检查文件大小（限制10MB）
                val fileSize = getFileSize(context, uri)
                if (fileSize > 10 * 1024 * 1024) {
                    onError("文件过大（超过10MB），无法导入")
                    onDismiss()
                    return@rememberLauncherForActivityResult
                }

                // 读取文件内容
                val content = readFileContent(context, uri)

                // 回调文件内容
                onFileSelected(fileName, filePath, content)
                onDismiss()
            } catch (e: Exception) {
                onError("读取文件失败: ${e.message}")
                onDismiss()
            }
        } else {
            onDismiss()
        }
    }

    // 触发文件选择
    LaunchedEffect(Unit) {
        filePickerLauncher.launch(arrayOf(
            "text/markdown",
            "text/plain",
            "text/x-markdown",
            "application/octet-stream",
            "*/*"
        ))
    }
}

/**
 * 获取文件名
 * 使用 ContentResolver 查询实际的显示名称，而不是 URI 中的 ID
 */
private fun getFileName(context: android.content.Context, uri: Uri): String {
    // 尝试从 ContentResolver 获取真实的文件名（显示名称）
    val fileNameFromResolver: String? = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) {
                cursor.getString(nameIndex)
            } else null
        } else null
    }

    // 如果无法从 ContentResolver 获取，回退到 URI 路径提取
    val result = if (fileNameFromResolver.isNullOrBlank()) {
        val fallback = uri.lastPathSegment ?: "unknown"
        fallback.substringAfterLast('/')
    } else {
        fileNameFromResolver
    }

    // 确保有文件扩展名
    return if (!result.contains('.')) {
        "$result.md"
    } else {
        result
    }
}

/**
 * 读取文件内容
 */
private fun readFileContent(context: android.content.Context, uri: Uri): String {
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: throw Exception("无法打开文件")

    return BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
        val content = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            content.append(line).append("\n")
        }
        content.toString()
    }
}

/**
 * 获取文件大小
 */
private fun getFileSize(context: android.content.Context, uri: Uri): Long {
    return context.contentResolver.openFileDescriptor(uri, "r")?.use { parcelFileDescriptor ->
        parcelFileDescriptor.statSize
    } ?: 0L
}
