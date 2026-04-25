package com.videonote.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonote.domain.model.Note
import com.videonote.domain.model.NoteStatus
import com.videonote.domain.model.NoteType
import com.videonote.domain.repository.VideoNoteRepository
import com.videonote.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 主界面ViewModel - 管理主界面的UI状态和业务逻辑
 * 负责显示当前选中的笔记、处理笔记选择和基本的数据加载
 * 使用Hilt进行依赖注入，遵循MVVM架构模式
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: VideoNoteRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    /**
     * UI状态的私有可变StateFlow - 内部状态管理
     */
    private val _uiState = MutableStateFlow(MainUiState())

    /**
     * UI状态的公开只读StateFlow - 外部状态观察
     */
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /**
     * ViewModel初始化
     */
    fun initialize() {
        if (_uiState.value.isLoading) {
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        // 从数据库加载最新笔记
        loadLatestNote()
    }

    /**
     * 加载最新笔记 - 从仓库获取笔记列表并选择最新的一个
     */
    private fun loadLatestNote() {
        viewModelScope.launch {
            try {
                val notes = repository.getAllNotes().firstOrNull()
                val latestNote = notes?.firstOrNull()

                _uiState.value = _uiState.value.copy(
                    currentNote = latestNote,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载数据失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 加载指定笔记 - 根据笔记ID查找并加载特定的笔记
     */
    fun loadSpecificNote(noteId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val notes = repository.getAllNotes().firstOrNull()
                val targetNote = notes?.find { it.id == noteId }
                _uiState.value = _uiState.value.copy(
                    currentNote = targetNote,
                    isLoading = false,
                    error = if (targetNote == null) "找不到指定的笔记" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载数据失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 加载文件 - 从URI加载文件内容并创建笔记
     * 侧边栏点击文件时调用
     *
     * @param uri 文件的URI
     * @param fileName 文件名
     */
    fun loadFile(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val content = withContext(Dispatchers.IO) {
                    readFileContent(uri, fileName)
                }

                if (content != null) {
                    // 检查文件名是否已存在
                    val existingNote = repository.getNoteByFileName(fileName)
                    if (existingNote != null) {
                        // 已存在，直接加载（使用最新内容更新）
                        val updatedNote = existingNote.copy(
                            markdownContent = content,
                            status = NoteStatus.SUCCESS,
                            updatedAt = System.currentTimeMillis()
                        )
                        _uiState.value = _uiState.value.copy(
                            currentNote = updatedNote,
                            isLoading = false
                        )
                        Logger.d(TAG, "Loaded existing note: $fileName")
                    } else {
                        // 不存在，创建新笔记并直接展示
                        val noteId = "LOCAL_${System.currentTimeMillis()}"
                        val note = Note(
                            id = noteId,
                            videoUrl = "",
                            platform = "LOCAL_FILE",
                            modelName = "local",
                            providerId = "local",
                            status = NoteStatus.SUCCESS,
                            markdownContent = content,
                            noteType = NoteType.LOCAL_FILE,
                            fileName = fileName,
                            filePath = uri.toString(),
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )

                        // 先立即展示内容
                        _uiState.value = _uiState.value.copy(
                            currentNote = note,
                            isLoading = false
                        )

                        // 后台保存到数据库
                        withContext(Dispatchers.IO) {
                            try {
                                repository.importLocalFile(fileName, uri.toString(), content)
                            } catch (e: Exception) {
                                Logger.e(TAG, "Error saving note to db: ${e.message}")
                            }
                        }
                        Logger.d(TAG, "Created and loaded new note: $fileName")
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "无法读取文件内容"
                    )
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error loading file: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载文件失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 从URI读取文件内容
     */
    private suspend fun readFileContent(uri: Uri, fileName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    reader.readText()
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error reading file: ${e.message}")
                null
            }
        }
    }

    /**
     * 选择笔记 - 手动设置当前显示的笔记
     */
    fun selectNote(note: Note) {
        _uiState.value = _uiState.value.copy(currentNote = note)
    }

    /**
     * 导入本地文件笔记（兼容旧功能）
     */
    suspend fun importLocalFile(fileName: String, filePath: String, content: String): Result<String> {
        return try {
            val result = repository.importLocalFile(fileName, filePath, content)
            if (result.isSuccess) {
                loadLatestNote()
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
    }
}

/**
 * 主界面UI状态数据类
 */
data class MainUiState(
    val currentNote: Note? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

private const val TAG = "VideoNote.MainViewModel"
