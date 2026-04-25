package com.videonote.domain.model

/**
 * TTS播放状态枚举 - 定义TTS引擎的所有可能状态
 * 参考TTS文档中的状态机设计，确保状态转换的正确性
 */
enum class TTSState {
    /** 停止状态 - 未播放或已完全停止 */
    STOPPED,

    /** 播放中 - 正在朗读文本 */
    PLAYING,

    /** 已暂停 - 暂停播放，可恢复 */
    PAUSED,

    /** 初始化中 - 正在初始化TTS引擎 */
    INITIALIZING,

    /** 错误状态 - 播放过程中发生错误 */
    ERROR
}

/**
 * TTS语音模型 - 表示单个TTS语音的信息
 * 对应TTS文档中的TTSVoice类型定义
 *
 * @property id 语音的唯一标识符，用于选择特定语音
 * @property name 语音的显示名称，例如"Google 中文普通话"
 * @property lang 语言代码，例如"zh-CN"、"en-US"
 * @property engineName 引擎名称，例如"android-tts"
 * @property isInstalled 是否已安装该语音（某些语音可能需要下载）
 */
data class TTSVoice(
    val id: String,
    val name: String,
    val lang: String,
    val engineName: String = "android-tts",
    val isInstalled: Boolean = true
)

/**
 * TTS设置数据类 - 存储TTS播放的所有配置参数
 * 对应TTS文档中的语速、音调等设置
 *
 * @property voiceId 当前选中的语音ID
 * @property speechRate 语速，范围0.2f - 3.0f，默认2.0f
 * @property pitch 音调，范围0.5f - 2.0f，默认1.0f
 * @property autoAdvance 是否自动播放下一段，默认true
 * @property highlightEnabled 是否启用文本高亮，默认true
 */
data class TTSSettings(
    val voiceId: String = "",
    val speechRate: Float = 2.0f,
    val pitch: Float = 1.0f,
    val autoAdvance: Boolean = true,
    val highlightEnabled: Boolean = true
) {
    companion object {
        /** 默认语速 */
        const val DEFAULT_RATE = 2.0f

        /** 最小语速 */
        const val MIN_RATE = 0.2f

        /** 最大语速 */
        const val MAX_RATE = 3.0f

        /** 默认音调 */
        const val DEFAULT_PITCH = 1.0f

        /** 最小音调 */
        const val MIN_PITCH = 0.5f

        /** 最大音调 */
        const val MAX_PITCH = 2.0f
    }

    /**
     * 验证语速是否在有效范围内
     */
    fun isValidRate(rate: Float): Boolean {
        return rate in MIN_RATE..MAX_RATE
    }

    /**
     * 验证音调是否在有效范围内
     */
    fun isValidPitch(pitch: Float): Boolean {
        return pitch in MIN_PITCH..MAX_PITCH
    }

    /**
     * 创建新的设置副本并更新语速
     */
    fun withRate(newRate: Float): TTSSettings {
        return copy(speechRate = newRate.coerceIn(MIN_RATE, MAX_RATE))
    }

    /**
     * 创建新的设置副本并更新音调
     */
    fun withPitch(newPitch: Float): TTSSettings {
        return copy(pitch = newPitch.coerceIn(MIN_PITCH, MAX_PITCH))
    }

    /**
     * 创建新的设置副本并更新语音
     */
    fun withVoice(newVoiceId: String): TTSSettings {
        return copy(voiceId = newVoiceId)
    }
}

/**
 * TTS文本块 - 表示要朗读的一个文本段落
 * 支持按段落/句子分割并分别朗读
 *
 * @property text 要朗读的文本内容
 * @property index 在全文中的索引位置
 * @property isCurrent 是否是当前正在朗读的段落
 */
data class TTSTextBlock(
    val text: String,
    val index: Int,
    val isCurrent: Boolean = false
)

/**
 * TTS定时选项 - 定时停止的配置
 * 对应TTS文档中的定时停止功能
 *
 * @property enabled 是否启用定时停止
 * @property minutes 定时分钟数，0表示无限制
 */
data class TTSTimer(
    val enabled: Boolean = false,
    val minutes: Int = 0
) {
    companion object {
        /** 预设的定时选项（分钟） */
        val PRESET_OPTIONS = listOf(0, 5, 10, 15, 30, 45, 60, 90, 120, 180, 240)
    }

    /**
     * 获取定时停止的显示文本
     */
    fun getDisplayText(): String {
        return when {
            !enabled -> "关闭"
            minutes == 0 -> "无限制"
            minutes < 60 -> "${minutes}分钟"
            else -> {
                val hours = minutes / 60
                val mins = minutes % 60
                if (mins > 0) "${hours}小时${mins}分钟"
                else "${hours}小时"
            }
        }
    }
}
