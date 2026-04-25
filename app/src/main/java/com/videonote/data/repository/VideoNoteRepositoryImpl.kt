package com.videonote.data.repository

import com.videonote.data.local.database.VideoNoteDatabase
import com.videonote.data.local.database.entity.mapper.asDomainModel
import com.videonote.data.local.database.entity.mapper.asEntity
import com.videonote.domain.model.*
import com.videonote.domain.repository.VideoNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoNoteRepositoryImpl @Inject constructor(
    private val database: VideoNoteDatabase
) : VideoNoteRepository {

    // 本地数据
    override fun getAllNotes(): Flow<List<Note>> {
        return database.noteDao().getAllNotes().map { entities ->
            entities.map { it.asDomainModel() }
        }
    }

    override suspend fun getNoteById(id: String): Note? {
        return database.noteDao().getNoteById(id)?.asDomainModel()
    }

    override suspend fun deleteNote(id: String) {
        database.noteDao().deleteNoteById(id)
    }

    // 本地文件笔记管理
    override suspend fun importLocalFile(fileName: String, filePath: String, content: String): Result<String> {
        return try {
            // 检查文件名是否已存在
            val existingNote = database.noteDao().getNoteByFileName(fileName)
            if (existingNote != null) {
                return Result.failure(Exception("文件名已存在: $fileName"))
            }

            // 创建本地文件笔记
            val noteId = "LOCAL_${System.currentTimeMillis()}"
            val note = Note(
                id = noteId,
                videoUrl = "", // 本地文件没有视频URL
                platform = "LOCAL_FILE",
                modelName = "local",
                providerId = "local",
                status = NoteStatus.SUCCESS, // 本地文件直接标记为成功
                markdownContent = content,
                originalContent = null,
                noteType = NoteType.LOCAL_FILE,
                fileName = fileName,
                filePath = filePath,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            database.noteDao().insertNote(note.asEntity())

            Result.success(noteId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getNotesByType(noteType: NoteType): Flow<List<Note>> {
        return database.noteDao().getNotesByType(noteType.name).map { entities ->
            entities.map { it.asDomainModel() }
        }
    }

    override suspend fun getNoteByFileName(fileName: String): Note? {
        return database.noteDao().getNoteByFileName(fileName)?.asDomainModel()
    }
}
