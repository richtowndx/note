package com.videonote.data.local.database.dao

import androidx.room.*
import com.videonote.data.local.database.entity.NoteEntity
import com.videonote.data.local.database.entity.NoteSizeInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    /**
     * 分析所有笔记记录的大小信息
     * 用于数据库优化和性能监控
     */
    @Query("SELECT id, LENGTH(markdownContent) as markdown_size, LENGTH(originalContent) as original_size, LENGTH(transcriptSegments) as transcript_size FROM notes ORDER BY updatedAt DESC")
    suspend fun analyzeNotesSize(): List<NoteSizeInfo>

    /**
     * 获取所有笔记记录，对大内容进行截断优化
     * 1MB以内完整显示，1MB以上截断为100KB以提升性能
     */
    @Query("SELECT id, videoUrl, platform, title, status, " +
           "markdownContent, originalContent, audioTitle, modelName, providerId, style, format, createdAt, updatedAt, " +
           "ttsProgress, ttsSpeechRate, ttsPitch, ttsVoiceId, noteType, filePath, fileName, " +
           "CASE " +
           "WHEN transcriptSegments IS NULL THEN NULL " +
           "WHEN LENGTH(transcriptSegments) <= 1048576 THEN transcriptSegments " +
           "ELSE SUBSTR(transcriptSegments, 1, 102400) || '\\n\\n...[内容过大，显示前100KB，共' || LENGTH(transcriptSegments)/1024 || 'KB，点击查看完整内容]' " +
           "END as transcriptSegments " +
           "FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): NoteEntity?

    /**
     * 获取活跃笔记记录（非SUCCESS状态），对大内容进行截断优化
     * 用于实时监控处理中的任务状态
     */
    @Query("SELECT id, videoUrl, platform, title, status, " +
           "markdownContent, originalContent, audioTitle, modelName, providerId, style, format, createdAt, updatedAt, " +
           "ttsProgress, ttsSpeechRate, ttsPitch, ttsVoiceId, noteType, filePath, fileName, " +
           "CASE " +
           "WHEN transcriptSegments IS NULL THEN NULL " +
           "WHEN LENGTH(transcriptSegments) <= 1048576 THEN transcriptSegments " +
           "ELSE SUBSTR(transcriptSegments, 1, 102400) || '\\n\\n...[内容过大，显示前100KB，共' || LENGTH(transcriptSegments)/1024 || 'KB，点击查看完整内容]' " +
           "END as transcriptSegments " +
           "FROM notes WHERE status != 'SUCCESS' ORDER BY createdAt DESC")
    fun getActiveNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    /**
     * 删除笔记及其关联的所有数据
     * 级联删除内容包括：
     * - 笔记基本信息
     * - TTS播放进度 (ttsProgress)
     * - TTS播放配置 (ttsSpeechRate, ttsPitch, ttsVoiceId)
     * - 笔记内容 (markdownContent, originalContent, transcriptSegments)
     */
    @Delete
    suspend fun deleteNote(note: NoteEntity)

    /**
     * 根据ID删除笔记及其关联的所有数据
     * 级联删除内容包括：
     * - 笔记基本信息
     * - TTS播放进度 (ttsProgress)
     * - TTS播放配置 (ttsSpeechRate, ttsPitch, ttsVoiceId)
     * - 笔记内容 (markdownContent, originalContent, transcriptSegments)
     */
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    /**
     * 删除所有笔记及其关联的所有数据
     * 级联删除内容包括：
     * - 所有笔记的基本信息
     * - 所有笔记的TTS播放进度和配置
     * - 所有笔记的内容数据
     */
    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    /**
     * 更新笔记内容和状态信息
     * 用于任务状态变更和内容更新
     */
    @Query("UPDATE notes SET status = :status, markdownContent = :markdownContent, originalContent = :originalContent, audioTitle = :audioTitle, transcriptSegments = :transcriptSegments, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNoteContent(
        id: String,
        status: String,
        markdownContent: String? = null,
        originalContent: String? = null,
        audioTitle: String? = null,
        transcriptSegments: String? = null,
        updatedAt: Long
    )

    /**
     * 更新TTS阅读进度
     * 用于保存当前TTS播放位置
     */
    @Query("UPDATE notes SET ttsProgress = :ttsProgress WHERE id = :id")
    suspend fun updateTtsProgress(id: String, ttsProgress: Int)

    /**
     * 更新TTS设置
     * 用于保存语速、音调、语音选择等配置
     */
    @Query("UPDATE notes SET ttsSpeechRate = :ttsSpeechRate, ttsPitch = :ttsPitch, ttsVoiceId = :ttsVoiceId WHERE id = :id")
    suspend fun updateTtsSettings(
        id: String,
        ttsSpeechRate: Float?,
        ttsPitch: Float?,
        ttsVoiceId: String?
    )

    /**
     * 根据文件名获取笔记
     * 用于本地文件导入时的文件名冲突检测
     *
     * @param fileName 文件名（不含路径）
     * @return 找到的笔记实体，未找到返回null
     */
    @Query("SELECT * FROM notes WHERE fileName = :fileName LIMIT 1")
    suspend fun getNoteByFileName(fileName: String): NoteEntity?

    /**
     * 根据笔记类型获取笔记列表
     * 用于历史页面的Tab过滤功能
     *
     * @param noteType 笔记类型（VIDEO 或 LOCAL_FILE）
     * @return Flow<List<NoteEntity>> 响应式数据流，发射指定类型的笔记列表
     */
    @Query("SELECT * FROM notes WHERE noteType = :noteType ORDER BY updatedAt DESC")
    fun getNotesByType(noteType: String): Flow<List<NoteEntity>>
}