package com.videonote.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Markdown文件导入工具类
 * 用于处理从外部打开的markdown/txt文件，支持文件内容读取和文件名提取
 */
object MarkdownFileImporter {

    /**
     * 从Uri读取文件内容
     *
     * @param context 应用上下文
     * @param uri 文件Uri
     * @return 文件内容字符串，失败返回null
     */
    fun readFileContent(context: Context, uri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val content = reader.use { it.readText() }
            content
        } catch (e: Exception) {
            Logger.e("MarkdownFileImporter", "读取文件内容失败: ${e.message}")
            null
        }
    }

    /**
     * 从Uri获取文件名
     *
     * @param context 应用上下文
     * @param uri 文件Uri
     * @return 文件名字符串，失败返回null
     */
    fun getFileName(context: Context, uri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && nameIndex >= 0) {
                    it.getString(nameIndex)
                } else {
                    null
                }
            } ?: run {
                // 如果查询失败，尝试从URI路径中提取文件名
                uri.lastPathSegment?.let { path ->
                    val fileName = path.substringAfterLast('/')
                    if (fileName.isNotEmpty()) fileName else null
                }
            }
        } catch (e: Exception) {
            Logger.e("MarkdownFileImporter", "获取文件名失败: ${e.message}")
            // 最后的回退方案：从URI路径中提取文件名
            uri.lastPathSegment?.let { path ->
                val fileName = path.substringAfterLast('/')
                if (fileName.isNotEmpty()) fileName else null
            }
        }
    }

    /**
     * 从Uri获取文件路径（用于显示）
     *
     * @param uri 文件Uri
     * @return 文件路径字符串
     */
    fun getFilePath(uri: Uri): String {
        return uri.toString()
    }

    /**
     * 判断文件是否为支持的markdown文件
     *
     * @param fileName 文件名
     * @return 是否为markdown文件
     */
    fun isMarkdownFile(fileName: String): Boolean {
        val lowerCase = fileName.lowercase()
        return lowerCase.endsWith(".md") ||
                lowerCase.endsWith(".markdown") ||
                lowerCase.endsWith(".txt")
    }

    /**
     * 验证文件内容是否有效
     *
     * @param content 文件内容
     * @return 内容是否有效
     */
    fun isValidContent(content: String): Boolean {
        return content.isNotBlank() && content.length <= 10 * 1024 * 1024 // 限制10MB
    }
}
