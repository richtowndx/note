package com.videonote.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonote.data.preferences.NotePreferences
import com.videonote.domain.model.FileTreeNode
import com.videonote.domain.model.NoteDirectory
import com.videonote.util.FileTreeLoader
import com.videonote.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 侧边栏UI状态
 */
data class SidebarUiState(
    val isLoading: Boolean = false,
    val rootNodes: List<FileTreeNode> = emptyList(),
    val selectedDirectory: String? = null,
    val selectedDirectoryName: String? = null,
    val error: String? = null
)

/**
 * 侧边栏ViewModel
 * 管理文件树的状态和文件选择
 */
@HiltViewModel
class SidebarViewModel @Inject constructor(
    private val notePreferences: NotePreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SidebarUiState())
    val uiState: StateFlow<SidebarUiState> = _uiState.asStateFlow()

    init {
        loadDirectories()
    }

    /**
     * 重新加载目录列表（从设置页面返回时调用）
     */
    fun refreshDirectories() {
        loadDirectories()
    }

    /**
     * 加载目录列表
     */
    private fun loadDirectories() {
        viewModelScope.launch {
            val directories = notePreferences.getNoteDirectories()
            val selectedDir = notePreferences.getSelectedDirectory()

            if (selectedDir != null && directories.any { it.path == selectedDir }) {
                // 加载选中的目录
                loadFileTree(selectedDir)
            } else if (directories.isNotEmpty()) {
                // 默认加载第一个目录
                selectDirectory(directories.first().path)
            } else {
                _uiState.value = SidebarUiState()
            }
        }
    }

    /**
     * 选择目录并加载文件树
     */
    fun selectDirectory(path: String) {
        notePreferences.setSelectedDirectory(path)
        loadFileTree(path)
    }

    /**
     * 加载指定目录的文件树
     */
    private fun loadFileTree(path: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val nodes = withContext(Dispatchers.IO) {
                    FileTreeLoader.loadFromSAFUri(context, android.net.Uri.parse(path))
                }
                val dirName = getDirectoryName(path)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    rootNodes = nodes,
                    selectedDirectory = path,
                    selectedDirectoryName = dirName
                )
                Logger.d("SidebarViewModel", "Loaded file tree with ${nodes.size} root nodes")
            } catch (e: Exception) {
                Logger.e("SidebarViewModel", "Error loading file tree: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载文件失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 展开/折叠目录节点
     */
    fun toggleNodeExpand(node: FileTreeNode) {
        viewModelScope.launch {
            val updatedNode = withContext(Dispatchers.IO) {
                if (node.isExpanded) {
                    FileTreeLoader.collapseNode(node)
                } else {
                    FileTreeLoader.expandNode(node, context)
                }
            }

            // 更新文件树中的节点
            val updatedNodes = updateNodeInTree(_uiState.value.rootNodes, node.path, updatedNode)
            _uiState.value = _uiState.value.copy(rootNodes = updatedNodes)
        }
    }

    /**
     * 点击节点
     * 如果是目录则展开/折叠，如果是文件则选中
     */
    fun onNodeClick(node: FileTreeNode) {
        if (node.isDirectory) {
            toggleNodeExpand(node)
        } else {
            // 文件节点被点击，在MainScreen中处理文件加载
            Logger.d("SidebarViewModel", "File selected: ${node.name}")
        }
    }

    /**
     * 递归更新树中的节点
     */
    private fun updateNodeInTree(
        nodes: List<FileTreeNode>,
        targetPath: String,
        updatedNode: FileTreeNode
    ): List<FileTreeNode> {
        return nodes.map { node ->
            if (node.path == targetPath) {
                updatedNode
            } else if (node.isDirectory && node.children.isNotEmpty()) {
                node.copy(children = updateNodeInTree(node.children, targetPath, updatedNode))
            } else {
                node
            }
        }
    }

    /**
     * 获取目录名称
     */
    fun getDirectoryName(path: String): String {
        val directories = notePreferences.getNoteDirectories()
        return directories.find { it.path == path }?.name ?: "未知目录"
    }
}
