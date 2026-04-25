package com.videonote.presentation

import android.content.Intent
import android.net.Uri
import com.videonote.util.Logger
import com.videonote.util.MarkdownFileImporter

/**
 * Intent处理器 - 管理从外部打开的Intent
 * 用于处理文件打开Intent，传递文件信息给UI层
 * 使用单例模式，确保全局只有一个实例
 */
object IntentHandler {

    /**
     * 待处理的文件导入信息
     */
    var pendingFileImport: FileImportInfo? = null
        private set

    /**
     * 处理Activity的Intent
     *
     * @param intent Activity收到的Intent
     * @return 是否有待处理的文件导入
     */
    fun handleIntent(intent: Intent?): Boolean {
        if (intent == null) return false

        val action = intent.action
        val data = intent.data

        // 检查是否为VIEW action且有数据
        if (Intent.ACTION_VIEW == action && data != null) {
            Logger.d("IntentHandler", "收到文件打开Intent: $data")

            // 处理文件
            pendingFileImport = FileImportInfo(uri = data)
            return true
        }

        // 检查是否为剪贴板数据
        if (Intent.ACTION_SEND == action && intent.type != null) {
            val clipData = intent.clipData
            if (clipData != null && clipData.itemCount > 0) {
                val item = clipData.getItemAt(0)
                val uri = item.uri
                if (uri != null) {
                    Logger.d("IntentHandler", "收到分享文件Intent: $uri")
                    pendingFileImport = FileImportInfo(uri = uri)
                    return true
                }
            }
        }

        return false
    }

    /**
     * 清除待处理的文件导入
     */
    fun clearPendingImport() {
        pendingFileImport = null
    }

    private const val TAG = "IntentHandler"
}

/**
 * 文件导入信息
 *
 * @property uri 文件Uri
 * @property fileName 文件名（可选，后续解析）
 * @property filePath 文件路径（可选，后续解析）
 */
data class FileImportInfo(
    val uri: Uri,
    val fileName: String? = null,
    val filePath: String? = null
)
