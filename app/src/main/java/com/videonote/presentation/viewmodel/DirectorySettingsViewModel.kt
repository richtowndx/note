package com.videonote.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonote.data.preferences.NotePreferences
import com.videonote.domain.model.NoteDirectory
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 目录设置页面的UI状态
 */
data class DirectorySettingsUiState(
    val directories: List<NoteDirectory> = emptyList(),
    val selectedDirectoryPath: String? = null,
    val error: String? = null
)

/**
 * 目录设置ViewModel
 * 管理笔记目录的添加和删除
 */
@HiltViewModel
class DirectorySettingsViewModel @Inject constructor(
    private val notePreferences: NotePreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DirectorySettingsUiState())
    val uiState: StateFlow<DirectorySettingsUiState> = _uiState.asStateFlow()

    init {
        loadDirectories()
    }

    /**
     * 加载目录列表
     */
    private fun loadDirectories() {
        val directories = notePreferences.getNoteDirectories()
        val selectedDir = notePreferences.getSelectedDirectory()
        _uiState.value = DirectorySettingsUiState(
            directories = directories,
            selectedDirectoryPath = selectedDir
        )
    }

    /**
     * 选择目录并设为当前激活目录
     */
    fun selectDirectory(path: String) {
        notePreferences.setSelectedDirectory(path)
        loadDirectories()
    }

    /**
     * 添加笔记目录
     * @param uriStr 目录的URI字符串
     */
    fun addDirectory(uriStr: String) {
        viewModelScope.launch {
            try {
                val uri = Uri.parse(uriStr)
                val documentFile = DocumentFile.fromTreeUri(context, uri)

                if (documentFile == null || !documentFile.exists()) {
                    _uiState.value = _uiState.value.copy(error = "无法访问选择的目录")
                    return@launch
                }

                val name = documentFile.name ?: "未知目录"
                val directory = NoteDirectory(
                    path = uriStr,
                    name = name
                )

                val success = notePreferences.addNoteDirectory(directory)
                if (success) {
                    loadDirectories()
                } else {
                    _uiState.value = _uiState.value.copy(error = "该目录已添加")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "添加目录失败: ${e.message}")
            }
        }
    }

    /**
     * 删除笔记目录
     * @param path 目录路径
     */
    fun removeDirectory(path: String) {
        viewModelScope.launch {
            notePreferences.removeNoteDirectory(path)

            // 如果删除的是当前选中的目录，清除选中状态
            if (notePreferences.getSelectedDirectory() == path) {
                notePreferences.setSelectedDirectory(null)
            }

            loadDirectories()
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
