package com.videonote.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.videonote.domain.model.FileTreeNode
import java.io.File

/**
 * 文件树加载器
 * 用于扫描目录并构建文件树结构
 */
object FileTreeLoader {

    private val SUPPORTED_EXTENSIONS = setOf(".md", ".markdown", ".txt")

    /**
     * 从SAF URI加载文件树（用于Android 11+）
     */
    fun loadFromSAFUri(context: Context, uri: Uri): List<FileTreeNode> {
        val documentFile = DocumentFile.fromTreeUri(context, uri) ?: return emptyList()
        return buildFileTreeFromDocumentFile(documentFile)
    }

    /**
     * 从本地文件系统路径加载文件树
     */
    fun loadFromPath(path: String): List<FileTreeNode> {
        val directory = File(path)
        if (!directory.exists() || !directory.isDirectory) {
            return emptyList()
        }
        return buildFileTreeFromFile(directory)
    }

    /**
     * 从DocumentFile构建文件树（SAF）
     */
    private fun buildFileTreeFromDocumentFile(documentFile: DocumentFile): List<FileTreeNode> {
        val nodes = mutableListOf<FileTreeNode>()

        documentFile.listFiles().forEach { file ->
            if (file.isDirectory) {
                // 忽略隐藏目录
                if (file.name?.startsWith(".") != true) {
                    val children = buildFileTreeFromDocumentFile(file)
                    // 只添加有子节点的目录（排除空目录）
                    if (children.isNotEmpty() || hasSupportedFilesInDocumentFile(file)) {
                        nodes.add(
                            FileTreeNode(
                                name = file.name ?: "未知",
                                path = file.uri.toString(),
                                isDirectory = true,
                                children = children,
                                isExpanded = false
                            )
                        )
                    }
                }
            } else if (file.isFile) {
                val name = file.name ?: return@forEach
                if (isSupportedFile(name)) {
                    nodes.add(
                        FileTreeNode(
                            name = name,
                            path = file.uri.toString(),
                            isDirectory = false,
                            children = emptyList(),
                            isExpanded = false
                        )
                    )
                }
            }
        }

        // 排序：目录在前，文件在后，按名称排序
        return sortNodes(nodes)
    }

    /**
     * 从File构建文件树（本地文件系统）
     */
    private fun buildFileTreeFromFile(directory: File): List<FileTreeNode> {
        val nodes = mutableListOf<FileTreeNode>()

        directory.listFiles()?.forEach { file ->
            if (file.isHidden) return@forEach

            if (file.isDirectory) {
                val children = buildFileTreeFromFile(file)
                // 只添加有子节点的目录（排除空目录）
                if (children.isNotEmpty() || hasSupportedFilesInFile(file)) {
                    nodes.add(
                        FileTreeNode(
                            name = file.name,
                            path = file.absolutePath,
                            isDirectory = true,
                            children = children,
                            isExpanded = false
                        )
                    )
                }
            } else if (file.isFile) {
                if (isSupportedFile(file.name)) {
                    nodes.add(
                        FileTreeNode(
                            name = file.name,
                            path = file.absolutePath,
                            isDirectory = false,
                            children = emptyList(),
                            isExpanded = false
                        )
                    )
                }
            }
        }

        // 排序：目录在前，文件在后，按名称排序
        return sortNodes(nodes)
    }

    /**
     * 对节点列表进行排序：目录在前，文件在后，按名称排序
     */
    private fun sortNodes(nodes: List<FileTreeNode>): List<FileTreeNode> {
        return nodes.sortedWith(Comparator { a, b ->
            when {
                a.isDirectory && !b.isDirectory -> -1
                !a.isDirectory && b.isDirectory -> 1
                else -> a.name.lowercase().compareTo(b.name.lowercase())
            }
        })
    }

    /**
     * 检查目录或其子目录是否包含支持的文件
     */
    private fun hasSupportedFilesInFile(file: File): Boolean {
        if (!file.isDirectory) {
            return isSupportedFile(file.name)
        }
        return file.listFiles()?.any { f ->
            if (f.isHidden) false
            else if (f.isDirectory) hasSupportedFilesInFile(f)
            else isSupportedFile(f.name)
        } ?: false
    }

    /**
     * 检查DocumentFile是否包含支持的文件
     */
    private fun hasSupportedFilesInDocumentFile(documentFile: DocumentFile): Boolean {
        if (!documentFile.isDirectory) {
            return isSupportedFile(documentFile.name ?: "")
        }
        return documentFile.listFiles().any { f ->
            if (f.name?.startsWith(".") == true) false
            else if (f.isDirectory) hasSupportedFilesInDocumentFile(f)
            else isSupportedFile(f.name ?: "")
        }
    }

    /**
     * 检查是否为支持的文件类型
     */
    fun isSupportedFile(fileName: String): Boolean {
        val lowerName = fileName.lowercase()
        return SUPPORTED_EXTENSIONS.any { lowerName.endsWith(it) }
    }

    /**
     * 展开目录获取其子节点
     */
    fun expandNode(node: FileTreeNode, context: Context? = null): FileTreeNode {
        if (!node.isDirectory) return node

        return if (node.path.startsWith("content://")) {
            // SAF URI
            if (context != null) {
                try {
                    val uri = Uri.parse(node.path)
                    val documentFile = DocumentFile.fromTreeUri(context, uri)
                    val children = documentFile?.listFiles()?.mapNotNull { file ->
                        when {
                            file.name?.startsWith(".") == true -> null
                            file.isDirectory -> {
                                FileTreeNode(
                                    name = file.name ?: "未知",
                                    path = file.uri.toString(),
                                    isDirectory = true,
                                    children = emptyList(),
                                    isExpanded = false
                                )
                            }
                            file.isFile && isSupportedFile(file.name ?: "") -> {
                                FileTreeNode(
                                    name = file.name ?: return@mapNotNull null,
                                    path = file.uri.toString(),
                                    isDirectory = false,
                                    children = emptyList(),
                                    isExpanded = false
                                )
                            }
                            else -> null
                        }
                    } ?: emptyList()

                    node.copy(children = sortNodes(children), isExpanded = true)
                } catch (e: Exception) {
                    Logger.e("FileTreeLoader", "Error expanding SAF node: ${e.message}")
                    node.copy(isExpanded = true)
                }
            } else {
                node.copy(isExpanded = true)
            }
        } else {
            // 本地文件系统路径
            val directory = File(node.path)
            val children = directory.listFiles()?.filter { !it.isHidden }?.mapNotNull { file ->
                when {
                    file.isDirectory -> {
                        FileTreeNode(
                            name = file.name,
                            path = file.absolutePath,
                            isDirectory = true,
                            children = emptyList(),
                            isExpanded = false
                        )
                    }
                    isSupportedFile(file.name) -> {
                        FileTreeNode(
                            name = file.name,
                            path = file.absolutePath,
                            isDirectory = false,
                            children = emptyList(),
                            isExpanded = false
                        )
                    }
                    else -> null
                }
            } ?: emptyList()

            node.copy(children = sortNodes(children), isExpanded = true)
        }
    }

    /**
     * 折叠目录
     */
    fun collapseNode(node: FileTreeNode): FileTreeNode {
        return node.copy(isExpanded = false)
    }
}
