package com.videonote.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 笔记实体类 - 存储视频笔记信息的数据库表
 * 对应数据库表名: notes
 *
 * 注意：删除笔记记录时会级联删除以下关联数据：
 * - TTS播放进度 (ttsProgress)
 * - TTS播放配置 (ttsSpeechRate, ttsPitch, ttsVoiceId)
 * - 笔记内容 (markdownContent, originalContent, transcriptSegments)
 */
@Entity(tableName = "notes")
data class NoteEntity(
    /**
     * 主键 - 笔记唯一标识符
     * 通常使用UUID或任务ID作为标识
     */
    @PrimaryKey val id: String,

    /**
     * 视频链接 - 用户输入的原始视频URL
     * 支持哔哩哔哩、YouTube、抖音、快手等平台
     */
    val videoUrl: String,

    /**
     * 视频平台标识 - 视频来源平台名称
     * 例如: bilibili, youtube, douyin, kuaishou 等
     */
    val platform: String,

    /**
     * 视频标题 - 可选的视频标题信息
     * 可能为空，某些情况下从视频元数据中提取
     */
    val title: String? = null,

    /**
     * 处理状态 - 笔记生成任务的当前状态
     * 可能的值:
     * - PENDING: 等待处理
     * - PARSING: 解析视频链接
     * - DOWNLOADING: 下载视频/音频
     * - TRANSCRIBING: 音频转文字
     * - SUMMARIZING: 内容总结
     * - FORMATTING: 格式化输出
     * - SAVING: 保存结果
     * - SUCCESS: 处理成功
     * - FAILED: 处理失败
     */
    val status: String,

    /**
     * Markdown格式内容 - 最终生成的结构化笔记
     * 包含视频摘要、关键点等格式化内容
     */
    val markdownContent: String? = null,

    /**
     * 原始文本内容 - 音频转录的原始文字内容
     * 未经过总结和格式化的转录文本
     */
    val originalContent: String? = null,

    /**
     * 音频元数据中的标题 - 从音频文件元数据提取的视频标题
     * 有时与title字段不同，这是从音频文件本身提取的
     */
    val audioTitle: String? = null,

    /**
     * 转录片段数据 - JSON格式存储的时间戳文字片段
     * 包含每个文字片段的时间信息，格式为JSON字符串
     * 用于生成带时间戳的转录内容
     */
    val transcriptSegments: String? = null,

    /**
     * 使用的AI模型名称 - 生成笔记时使用的具体AI模型
     * 例如: gpt-4, claude-3 等
     */
    val modelName: String,

    /**
     * AI服务提供商ID - 提供AI模型的服务商标识
     * 关联到providers表中的提供商
     */
    val providerId: String,

    /**
     * 笔记风格 - 内容生成和格式化的风格偏好
     * 例如: academic, casual, detailed, summary 等
     */
    val style: String? = null,

    /**
     * 输出格式列表 - 笔记内容的输出格式要求
     * 例如: ["markdown", "json", "txt"] 等
     */
    val format: List<String>? = null,

    /**
     * 创建时间戳 - 记录创建的Unix时间戳(毫秒)
     * 用于排序和时间追踪
     */
    val createdAt: Long,

    /**
     * 更新时间戳 - 记录最后更新的Unix时间戳(毫秒)
     * 用于状态变更追踪和排序
     */
    val updatedAt: Long,

    /**
     * TTS阅读进度 - 记录TTS播放到的文本块索引
     * 用于下次打开笔记时继续阅读
     */
    val ttsProgress: Int? = null,

    /**
     * TTS语速设置 - 记录用户设置的TTS语速
     * 范围: 0.2f - 3.0f, 默认 1.0f
     */
    val ttsSpeechRate: Float? = null,

    /**
     * TTS音调设置 - 记录用户设置的TTS音调
     * 范围: 0.5f - 2.0f, 默认 1.0f
     */
    val ttsPitch: Float? = null,

    /**
     * TTS语音ID - 记录用户选择的TTS语音标识
     * 用于恢复用户的语音选择
     */
    val ttsVoiceId: String? = null,

    /**
     * 笔记类型 - 标识笔记的来源类型
     * 可能的值: VIDEO (视频笔记), LOCAL_FILE (本地文件笔记)
     * 默认为 VIDEO，保持向后兼容
     */
    val noteType: String = "VIDEO",

    /**
     * 本地文件路径 - 本地文件笔记的完整文件路径
     * 仅对 LOCAL_FILE 类型的笔记有效，VIDEO 类型为 null
     */
    val filePath: String? = null,

    /**
     * 文件名 - 本地文件的名称（不含路径）
     * 仅对 LOCAL_FILE 类型的笔记有效，VIDEO 类型为 null
     * 用于文件名冲突检测和显示
     */
    val fileName: String? = null
)