package com.videonote.domain.model

/**
 * 文件树节点 - 表示文件系统中单个文件或目录
 *
 * @property name 文件或目录名称
 * @property path 文件或目录的完整路径
 * @property isDirectory 是否为目录
 * @property children 子节点列表（仅目录有）
 * @property isExpanded 是否展开（仅目录有）
 */
data class FileTreeNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val children: List<FileTreeNode> = emptyList(),
    val isExpanded: Boolean = false
)

/**
 * 笔记目录配置 - 用户添加的本地笔记存储目录
 *
 * @property path 目录路径
 * @property name 显示名称
 */
data class NoteDirectory(
    val path: String,
    val name: String
)
