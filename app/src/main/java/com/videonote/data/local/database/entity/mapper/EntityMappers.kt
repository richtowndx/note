package com.videonote.data.local.database.entity.mapper

import com.videonote.data.local.database.entity.*
import com.videonote.data.remote.dto.*
import com.videonote.domain.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Provider mappings
fun ProviderDto.asDomainModel(): Provider {
    return Provider(
        id = id,
        name = name,
        logo = logo,
        type = type,
        apiKey = api_key,
        baseUrl = base_url,
        enabled = enabled,
        createdAt = created_at
    )
}

fun ProviderDto.asEntity(): ProviderEntity {
    return ProviderEntity(
        id = id,
        name = name,
        logo = logo,
        type = type,
        apiKey = api_key,
        baseUrl = base_url,
        enabled = enabled,
        createdAt = created_at
    )
}

fun ProviderEntity.asDomainModel(): Provider {
    return Provider(
        id = id,
        name = name,
        logo = logo,
        type = type,
        apiKey = apiKey,
        baseUrl = baseUrl,
        enabled = enabled,
        createdAt = createdAt
    )
}

fun Provider.asEntity(): ProviderEntity {
    return ProviderEntity(
        id = id,
        name = name,
        logo = logo,
        type = type,
        apiKey = apiKey,
        baseUrl = baseUrl,
        enabled = enabled,
        createdAt = createdAt
    )
}

// Model mappings
fun ModelDto.asDomainModel(): ModelListItem {
    return ModelListItem(
        id = id.toString(),
        providerId = provider_id,
        modelName = model_name,
        createdAt = created_at
    )
}

fun ModelInfoDto.asDomainModel(): ModelInfo {
    return ModelInfo(
        id = id,
        created = created,
        `object` = `object`,
        ownedBy = owned_by,
        permission = permission,
        root = root
    )
}

fun ModelListItem.asEntity(): ModelEntity {
    return ModelEntity(
        id = id.toIntOrNull() ?: 0,
        providerId = providerId,
        modelName = modelName,
        createdAt = createdAt
    )
}

// Note mappings
fun Note.asEntity(): NoteEntity {
    return NoteEntity(
        id = id,
        videoUrl = videoUrl,
        platform = platform,
        title = title,
        status = status.name,
        markdownContent = markdownContent,
        originalContent = originalContent,
        audioTitle = audioTitle,
        transcriptSegments = transcriptSegments?.let { Json.encodeToString(it) },
        modelName = "default", // 提供默认值
        providerId = "default", // 提供默认值
        style = null,
        format = null,
        createdAt = createdAt,
        updatedAt = updatedAt,
        ttsProgress = ttsProgress,
        ttsSpeechRate = ttsSpeechRate,
        ttsPitch = ttsPitch,
        ttsVoiceId = ttsVoiceId,
        noteType = noteType.name,
        filePath = filePath,
        fileName = fileName
    )
}

fun NoteEntity.asDomainModel(): Note {
    return Note(
        id = id,
        videoUrl = videoUrl,
        platform = platform,
        title = title,
        status = NoteStatus.valueOf(status),
        markdownContent = markdownContent,
        originalContent = originalContent,
        audioTitle = audioTitle,
        transcriptSegments = transcriptSegments?.let {
            try {
                Json.decodeFromString<List<com.videonote.domain.model.TranscriptSegment>>(it)
            } catch (e: Exception) {
                null
            }
        },
        modelName = modelName,
        providerId = providerId,
        style = style,
        format = format,
        createdAt = createdAt,
        updatedAt = updatedAt,
        ttsProgress = ttsProgress,
        ttsSpeechRate = ttsSpeechRate,
        ttsPitch = ttsPitch,
        ttsVoiceId = ttsVoiceId,
        noteType = try {
            NoteType.valueOf(noteType)
        } catch (e: Exception) {
            NoteType.VIDEO // 默认为视频笔记
        },
        filePath = filePath,
        fileName = fileName
    )
}

// TaskStatus mappings
fun TaskStatusResponse.asDomainModel(): TaskStatus {
    return TaskStatus(
        status = status,
        message = message,
        taskId = task_id,
        result = result?.let {
            com.videonote.domain.model.TaskResult(
                markdown = it.markdown,
                originalText = it.originalText,
                audioMeta = it.audioMeta?.let { audioMeta ->
                    com.videonote.domain.model.AudioMeta(title = audioMeta.title)
                },
                transcript = it.transcript?.let { transcript ->
                    com.videonote.domain.model.Transcript(
                        segments = transcript.segments?.map { segment ->
                            com.videonote.domain.model.TranscriptSegment(
                                text = segment.text,
                                start = segment.start,
                                end = segment.end
                            )
                        }
                    )
                }
            )
        }
    )
}

// DTO to domain model mappings for API responses
fun com.videonote.data.remote.dto.CookieResponse.asDomainModel(): com.videonote.domain.model.CookieResponse {
    return com.videonote.domain.model.CookieResponse(
        cookie = cookie
    )
}