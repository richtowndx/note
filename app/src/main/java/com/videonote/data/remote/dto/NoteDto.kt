package com.videonote.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName


// // 专门用于API请求的结构体，严格按照接口文档定义
// @Serializable
// data class GenerateNoteRequest(
//     val video_url: String,
//     val platform: String,
//     val quality: String,
//     val screenshot: Boolean = false,
//     val link: Boolean = false,
//     val model_name: String,
//     val provider_id: String,
//     val task_id: String? = null,
//     val format: List<String>? = null,
//     val style: String? = null,
//     val extras: String? = null,
//     val video_understanding: Boolean = false,
//     val video_interval: Int = 0,
//     val grid_size: List<Int>? = null
// )

@Serializable
data class TaskStatusResponse(
    val status: String,
    val message: String?,
    val task_id: String,
    val result: TaskResult? = null
)

@Serializable
data class TaskResult(
    val markdown: String,
    @SerialName("original_text") val originalText: String? = null,
    @SerialName("audio_meta") val audioMeta: AudioMeta? = null,
    val transcript: Transcript? = null
)

@Serializable
data class AudioMeta(
    val title: String? = null
)

@Serializable
data class Transcript(
    val segments: List<TranscriptSegment>? = null
)

@Serializable
data class TranscriptSegment(
    val text: String? = null,
    val start: Float? = null,
    val end: Float? = null
)

@Serializable
data class RecordRequest(
    val video_id: String,
    val platform: String
)

@Serializable
data class CookieResponse(
    val platform: String,
    val cookie: String
)

// 专门用于生成笔记API的请求DTO，严格按照接口文档定义
@Serializable
data class GenerateNoteRequest(
    @SerialName("video_url") val videoUrl: String,
    @SerialName("platform") val platform: String,
    @SerialName("quality") val quality: String,
    @SerialName("screenshot") val screenshot: Boolean = false,
    @SerialName("link") val link: Boolean = false,
    @SerialName("model_name") val modelName: String,
    @SerialName("provider_id") val providerId: String,
    @SerialName("format") val format: List<String>? = null,
    @SerialName("style") val style: String? = null,
    @SerialName("video_understanding") val videoUnderstanding: Boolean = false,
    @SerialName("video_interval") val videoInterval: Int = 0
)

@Serializable
data class CookieUpdateRequest(
    val platform: String,
    val cookie: String
)