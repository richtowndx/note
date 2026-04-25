package com.videonote.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API响应模型 - 业务层使用的通用API响应结构
 * 与数据传输层的ApiResponse不同，这是在业务逻辑层使用的响应模型
 *
 * @param <T> 响应数据的泛型类型，具体数据根据不同的API端点而变化
 */
data class ApiResponse<T>(
    /**
     * 响应状态码 - API调用的结果状态码
     * 标准HTTP状态码，用于判断API调用是否成功
     */
    val code: Int,

    /**
     * 响应消息 - API调用的结果描述信息
     * 包含成功或失败的详细描述信息
     */
    val msg: String,

    /**
     * 响应数据 - API返回的具体业务数据
     * 泛型类型，根据不同的API端点返回不同的数据结构
     */
    val data: T?
)

/**
 * AI服务提供商模型 - 业务层的供应商数据结构
 * 表示AI服务提供商的完整信息，用于业务逻辑处理
 */
data class Provider(
    /**
     * 提供商唯一标识符
     * 用于关联模型和笔记记录的系统ID
     */
    val id: String,

    /**
     * 提供商显示名称
     * 例如: OpenAI, Anthropic, 百度文心等
     */
    val name: String,

    /**
     * 提供商Logo标识
     * 用于UI显示的图标或图片路径
     */
    val logo: String,

    /**
     * 提供商类型
     * built-in: 内置提供商, custom: 自定义提供商
     */
    val type: String,

    /**
     * API访问密钥
     * 用于API认证的敏感信息
     */
    val apiKey: String,

    /**
     * API基础地址
     * AI服务的API端点基础URL
     */
    val baseUrl: String,

    /**
     * 启用状态
     * 0: 禁用, 1: 启用
     */
    val enabled: Int,

    /**
     * 创建时间
     * ISO格式的时间戳字符串
     */
    val createdAt: String
)

/**
 * 视频笔记生成请求模型 - 发送给后端API的完整请求结构
 * 包含生成视频笔记所需的所有参数和配置信息
 */
@Serializable
data class VideoRequest(
    /**
     * 视频URL - 要处理的视频链接
     * 支持各大视频平台的链接格式
     */
    @SerialName("video_url") val videoUrl: String,

    /**
     * 视频平台 - 视频来源平台标识
     * 例如: bilibili, youtube, douyin等
     */
    @SerialName("platform") val platform: String,

    /**
     * 视频质量 - 视频下载质量设置
     * 例如: 720p, 1080p, highest等
     */
    @SerialName("quality") val quality: String,

    /**
     * 是否截图 - 是否在处理过程中生成视频截图
     * 默认为false，不生成截图
     */
    @SerialName("screenshot") val screenshot: Boolean = false,

    /**
     * 是否删除截图 - 处理完成后是否删除截图文件
     * 默认为false，保留截图文件
     */
    @SerialName("deleteScreenshot") val deleteScreenshot: Boolean = false,

    /**
     * 是否生成链接 - 是否在笔记中包含视频链接
     * 默认为false，不包含链接
     */
    @SerialName("link") val link: Boolean = false,

    /**
     * AI模型名称 - 用于生成笔记的AI模型
     * 例如: gpt-4, claude-3等
     */
    @SerialName("model_name") val modelName: String,

    /**
     * 服务提供商ID - AI服务提供商标识
     * 用于选择使用哪个AI服务
     */
    @SerialName("provider_id") val providerId: String,

    /**
     * 任务ID - 可选的预分配任务标识符
     * 用于跟踪和查询特定任务
     */
    @SerialName("task_id") val taskId: String? = null,

    /**
     * 输出格式 - 笔记的输出格式列表
     * 例如: ["markdown", "json"]等
     */
    @SerialName("format") val format: List<String>? = null,

    /**
     * 笔记风格 - 内容生成的风格偏好
     * 例如: academic, casual, detailed等
     */
    @SerialName("style") val style: String? = null,

    /**
     * 额外参数 - 自定义的额外处理参数
     * JSON格式的字符串，包含特殊配置
     */
    @SerialName("extras") val extras: String? = null,

    /**
     * 视频理解 - 是否启用视频内容理解功能
     * 默认为false，仅处理音频
     */
    @SerialName("video_understanding") val videoUnderstanding: Boolean = false,

    /**
     * 视频间隔 - 视频截图的时间间隔(秒)
     * 默认为0，不进行定时截图
     */
    @SerialName("video_interval") val videoInterval: Int = 0,

    /**
     * 网格大小 - 视频截图的网格布局尺寸
     * 例如: [3, 3]表示3x3的网格布局
     */
    @SerialName("grid_size") val gridSize: List<Int>? = null
)

/**
 * 任务状态响应模型 - 异步任务处理状态的完整信息
 * 用于查询笔记生成任务的当前状态和结果
 */
data class TaskStatus(
    /**
     * 任务状态 - 当前处理阶段
     * 可能的值: PENDING, PARSING, DOWNLOADING, TRANSCRIBING, SUMMARIZING, FORMATTING, SAVING, SUCCESS, FAILED
     */
    val status: String,

    /**
     * 状态消息 - 当前状态的详细描述
     * 可以为null，特别是在成功或简单状态变更时
     */
    val message: String?,

    /**
     * 任务标识符 - 任务的唯一ID
     * 用于跟踪和查询特定任务
     */
    val taskId: String,

    /**
     * 任务结果 - 处理完成后的结果数据
     * 仅在任务完成时才有值，否则为null
     */
    val result: TaskResult?
)

/**
 * 任务结果模型 - 笔记生成任务的完整输出数据
 * 包含生成的笔记内容和相关元数据信息
 */
data class TaskResult(
    /**
     * Markdown格式笔记 - 最终生成的结构化笔记内容
     * 包含视频摘要、关键点等格式化内容
     */
    val markdown: String,

    /**
     * 原始转录文本 - 音频转录的原始文字内容
     * 未经过总结和格式化的完整转录文本
     */
    val originalText: String? = null,

    /**
     * 音频元数据 - 从音频文件提取的元信息
     * 包含标题、时长等音频相关信息
     */
    val audioMeta: AudioMeta? = null,

    /**
     * 转录数据 - 带时间戳的转录片段信息
     * 包含每个文字片段的时间位置信息
     */
    val transcript: Transcript? = null
)

/**
 * 音频元数据模型 - 音频文件的元信息结构
 * 包含从音频文件中提取的基本信息
 */
@Serializable
data class AudioMeta(
    /**
     * 音频标题 - 音频文件的标题信息
     * 通常从音频文件的元数据中提取
     */
    val title: String? = null
)

/**
 * 转录数据模型 - 音频转录的完整信息
 * 包含时间戳文字片段的详细数据
 */
@Serializable
data class Transcript(
    /**
     * 转录片段列表 - 带时间戳的文字片段数组
     * 每个片段包含文字内容和时间位置信息
     */
    val segments: List<TranscriptSegment>? = null
)

/**
 * 转录片段模型 - 单个文字片段的详细信息
 * 包含文字内容及其在音频中的时间位置
 */
@Serializable
data class TranscriptSegment(
    /**
     * 文字内容 - 该片段的转录文字
     * 可以为null，特别是在数据不完整时
     */
    val text: String? = null,

    /**
     * 开始时间 - 片段在音频中的开始时间(秒)
     * 浮点数精度，用于精确定位
     */
    val start: Float? = null,

    /**
     * 结束时间 - 片段在音频中的结束时间(秒)
     * 浮点数精度，用于精确定位
     */
    val end: Float? = null
)

/**
 * 任务ID响应模型 - 创建任务后返回的标识符
 * 用于异步任务处理的后续查询
 */
data class TaskIdResponse(
    /**
     * 任务标识符 - 新创建任务的唯一ID
     * 用于后续状态查询和结果获取
     */
    val taskId: String
)

/**
 * 笔记实体模型 - 业务层的笔记数据结构
 * 表示视频笔记的完整信息，用于业务逻辑处理和UI显示
 */
data class Note(
    /**
     * 笔记唯一标识符
     * 通常使用UUID或任务ID作为唯一标识
     */
    val id: String,

    /**
     * 视频链接 - 原始视频URL
     * 支持各大视频平台的链接格式
     */
    val videoUrl: String,

    /**
     * 视频平台标识
     * 例如: bilibili, youtube, douyin等
     */
    val platform: String,

    /**
     * 视频标题 - 可选的视频标题信息
     * 可能为空，某些情况下从视频元数据中提取
     */
    val title: String? = null,

    /**
     * 处理状态 - 笔记生成任务的当前状态
     * 使用枚举类型确保状态值的有效性
     */
    val status: NoteStatus,

    /**
     * Markdown格式内容 - 最终生成的结构化笔记
     * 包含视频摘要、关键点等格式化内容
     */
    val markdownContent: String? = null,

    /**
     * 原始文本内容 - 音频转录的原始文字
     * 未经过总结和格式化的转录文本
     */
    val originalContent: String? = null,

    /**
     * 音频元数据标题 - 从音频文件提取的视频标题
     * 有时与title字段不同，这是从音频文件本身提取的
     */
    val audioTitle: String? = null,

    /**
     * 转录片段列表 - 带时间戳的文字片段数组
     * 包含每个文字片段的时间位置信息
     */
    val transcriptSegments: List<TranscriptSegment>? = null,

    /**
     * AI模型名称 - 生成笔记时使用的AI模型
     * 默认为"default"，实际使用时应该指定具体模型
     */
    val modelName: String = "default",

    /**
     * 服务提供商ID - AI服务提供商标识
     * 默认为"default"，实际使用时应该指定具体提供商
     */
    val providerId: String = "default",

    /**
     * 笔记风格 - 内容生成和格式化的风格偏好
     * 例如: academic, casual, detailed等
     */
    val style: String? = null,

    /**
     * 输出格式列表 - 笔记内容的输出格式要求
     * 例如: ["markdown", "json", "txt"]等
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
     * 使用枚举确保类型值的有效性
     */
    val noteType: NoteType = NoteType.VIDEO,

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

/**
 * 笔记状态枚举 - 笔记生成任务的可能状态
 * 使用枚举确保状态值的一致性和有效性
 */
enum class NoteStatus {
    /**
     * 等待处理 - 任务已创建但尚未开始处理
     */
    PENDING,

    /**
     * 解析视频链接 - 正在解析视频URL和获取视频信息
     */
    PARSING,

    /**
     * 下载视频/音频 - 正在下载视频或音频文件
     */
    DOWNLOADING,

    /**
     * 音频转文字 - 正在进行语音识别和转录
     */
    TRANSCRIBING,

    /**
     * 内容总结 - 正在使用AI进行内容分析和总结
     */
    SUMMARIZING,

    /**
     * 格式化输出 - 正在整理和格式化笔记内容
     */
    FORMATTING,

    /**
     * 保存结果 - 正在保存最终生成的笔记
     */
    SAVING,

    /**
     * 处理成功 - 任务完成，笔记已成功生成
     */
    SUCCESS,

    /**
     * 处理失败 - 任务过程中出现错误，无法完成
     */
    FAILED
}

/**
 * 供应商创建请求模型 - 添加新AI服务提供商的请求数据
 * 用于向系统添加新的AI服务提供商配置
 */
data class ProviderRequest(
    /**
     * 提供商名称 - 服务提供商的显示名称
     * 例如: OpenAI, Anthropic等
     */
    val name: String,

    /**
     * API访问密钥 - 用于API认证的密钥
     * 敏感信息，用于访问AI服务
     */
    val apiKey: String,

    /**
     * API基础地址 - AI服务的API端点URL
     * 例如: https://api.openai.com/v1
     */
    val baseUrl: String,

    /**
     * 提供商Logo - 可选的图标或标识路径
     * 用于UI显示，可以为空
     */
    val logo: String? = null,

    /**
     * 提供商类型 - 区分内置和自定义提供商
     * 例如: built-in, custom
     */
    val type: String
)

/**
 * 供应商更新请求模型 - 修改现有AI服务提供商的请求数据
 * 用于更新已存在的提供商配置信息
 */
data class ProviderUpdateRequest(
    /**
     * 提供商ID - 要更新的提供商唯一标识符
     * 必填字段，用于指定要更新的提供商
     */
    val id: String,

    /**
     * 新的提供商名称 - 可选的更新名称
     * 为null时表示不更新此字段
     */
    val name: String? = null,

    /**
     * 新的API密钥 - 可选的更新密钥
     * 为null时表示不更新此字段
     */
    val apiKey: String? = null,

    /**
     * 新的API基础地址 - 可选的更新URL
     * 为null时表示不更新此字段
     */
    val baseUrl: String? = null,

    /**
     * 新的Logo - 可选的更新图标
     * 为null时表示不更新此字段
     */
    val logo: String? = null,

    /**
     * 新的提供商类型 - 可选的更新类型
     * 为null时表示不更新此字段
     */
    val type: String? = null,

    /**
     * 新的启用状态 - 可选的更新状态
     * 为null时表示不更新此字段
     */
    val enabled: Int? = null
)

/**
 * 提供商测试请求模型 - 测试AI服务提供商连接的请求数据
 * 用于验证提供商配置是否正确可用
 */
data class TestRequest(
    /**
     * 提供商ID - 要测试的提供商唯一标识符
     * 用于指定要测试连接的AI服务提供商
     */
    val id: String
)

/**
 * 记录删除请求模型 - 删除视频记录的请求数据
 * 用于删除特定的视频笔记记录
 */
data class RecordRequest(
    /**
     * 视频ID - 要删除的视频唯一标识符
     * 用于精确定位要删除的视频记录
     */
    val videoId: String,

    /**
     * 视频平台 - 视频来源平台标识
     * 用于进一步确认视频记录的准确性
     */
    val platform: String
)

/**
 * 模型列表项模型 - AI模型的基本信息
 * 用于显示和管理可用的AI模型列表
 */
data class ModelListItem(
    /**
     * 模型标识符 - 模型的唯一ID
     * 字符串格式，用于引用和选择模型
     */
    val id: String,

    /**
     * 提供商ID - 模型所属的服务提供商标识
     * 用于关联模型和提供商
     */
    val providerId: String,

    /**
     * 模型名称 - AI模型的显示名称
     * 例如: GPT-4, Claude-3等
     */
    val modelName: String,

    /**
     * 创建时间 - 模型添加到系统的时间
     * ISO格式的时间戳字符串，可选字段
     */
    val createdAt: String? = null
)

/**
 * 模型列表响应模型 - API返回的模型列表响应结构
 * 用于包装从API获取的完整模型信息
 */
data class ModelListResponse(
    /**
     * 模型信息列表 - 从API获取的模型详细信息数组
     * 包含每个模型的完整元数据信息
     */
    val models: List<ModelInfo>
)

/**
 * 模型信息模型 - AI模型的详细信息
 * 包含模型在AI服务提供商系统中的完整元数据
 */
data class ModelInfo(
    /**
     * 模型标识符 - 在AI服务系统中的唯一ID
     * 通常是字符串格式的模型名称
     */
    val id: String,

    /**
     * 创建时间戳 - 模型在AI服务系统中的创建时间
     * Unix时间戳格式，表示模型创建的精确时间
     */
    val created: Long,

    /**
     * 对象类型 - API响应中的对象类型标识
     * 通常固定为"model"，表示这是一个模型对象
     */
    val `object`: String,

    /**
     * 所有者 - 模型的所有者标识
     * 表示模型属于哪个组织或用户
     */
    val ownedBy: String,

    /**
     * 权限信息 - 模型的访问权限描述
     * 描述模型的使用权限和访问限制
     */
    val permission: String,

    /**
     * 根模型 - 模型的根标识符
     * 表示模型的基础版本或父模型
     */
    val root: String
)

/**
 * Cookie响应模型 - 视频平台Cookie信息的响应结构
 * 用于存储和传递视频平台的认证Cookie
 */
data class CookieResponse(
    /**
     * Cookie数据 - 视频平台的认证Cookie字符串
     * 用于访问需要登录的视频平台内容
     */
    val cookie: String
)

/**
 * 创建模型请求模型 - 向系统添加新AI模型的请求数据
 * 用于在指定提供商下添加新的AI模型
 */
data class CreateModelRequest(
    /**
     * 提供商ID - 新模型所属的服务提供商标识
     * 指定新模型应该属于哪个AI服务提供商
     */
    val providerId: String,

    /**
     * 模型名称 - 新添加的AI模型名称
     * 指定要添加的具体AI模型标识符
     */
    val modelName: String
)