package com.videonote.domain.repository

import com.videonote.domain.model.Note
import com.videonote.domain.model.NoteType
import kotlinx.coroutines.flow.Flow

/**
 * VideoNote仓库接口 - 定义数据访问和业务操作的抽象接口
 * 遵循Repository模式，为业务层提供统一的数据访问接口
 * 隐藏数据来源的复杂性，支持本地数据库访问
 */
interface VideoNoteRepository {

    /**
     * 获取所有笔记 - 获取本地数据库中存储的所有笔记记录
     * 使用Flow提供响应式数据流，当数据变更时自动通知观察者
     *
     * @return Flow<List<Note>> 响应式数据流，发射笔记列表
     */
    fun getAllNotes(): Flow<List<Note>>

    /**
     * 根据ID获取笔记 - 获取指定ID的笔记详细信息
     * 用于显示笔记详情或编辑笔记内容
     *
     * @param id 笔记的唯一标识符
     * @return Note? 找到返回笔记对象，未找到返回null
     */
    suspend fun getNoteById(id: String): Note?

    /**
     * 删除笔记 - 从本地数据库移除指定的笔记记录
     * 用于清理用户不需要的笔记
     *
     * @param id 要删除的笔记唯一标识符
     */
    suspend fun deleteNote(id: String)

    /**
     * 导入本地文件笔记 - 将本地Markdown/TXT文件作为笔记导入
     * 支持用户直接打开本地文件作为笔记使用
     *
     * @param fileName 文件名（不含路径）
     * @param filePath 文件完整路径
     * @param content 文件内容
     * @return Result<String> 成功返回笔记ID，失败返回异常信息
     */
    suspend fun importLocalFile(fileName: String, filePath: String, content: String): Result<String>

    /**
     * 根据笔记类型获取笔记 - 获取指定类型的所有笔记
     * 用于历史页面的Tab过滤功能
     *
     * @param noteType 笔记类型（VIDEO 或 LOCAL_FILE）
     * @return Flow<List<Note>> 响应式数据流，发射指定类型的笔记列表
     */
    fun getNotesByType(noteType: NoteType): Flow<List<Note>>

    /**
     * 根据文件名获取笔记 - 用于文件名冲突检测
     *
     * @param fileName 文件名（不含路径）
     * @return Note? 找到返回笔记对象，未找到返回null
     */
    suspend fun getNoteByFileName(fileName: String): Note?
}
