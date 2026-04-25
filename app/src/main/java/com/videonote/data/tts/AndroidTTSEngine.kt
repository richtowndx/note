package com.videonote.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.videonote.domain.model.TTSState
import com.videonote.util.Logger
import com.videonote.domain.model.TTSVoice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android系统TTS引擎实现
 * 使用Android系统自带的TextToSpeech API实现TTS功能
 * 参考TTS文档中的NativeTTSClient设计，适配Android平台
 *
 * 特点：
 * - 完全离线，无需网络连接
 * - 使用系统预装或用户安装的TTS语音
 * - 支持语速、音调调节
 * - 支持多语言语音切换
 *
 * @property context 应用上下文，用于创建TextToSpeech实例
 * @property contextProvider Context提供者，用于获取Activity Context
 */
@Singleton
class AndroidTTSEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contextProvider: ContextProvider
) : TTSEngine, TextToSpeech.OnInitListener {

    /** Android TextToSpeech 实例 */
    private var tts: TextToSpeech? = null

    /** 初始化状态 */
    private var isInitialized = false

    /** 当前状态流 */
    private val _stateFlow: MutableStateFlow<TTSState> = MutableStateFlow(TTSState.STOPPED)
    override val stateFlow: Flow<TTSState> = _stateFlow.asStateFlow()

    /** 当前朗读位置流 */
    private val _currentPositionFlow = MutableStateFlow<Int>(0)
    override val currentPositionFlow: Flow<Int> = _currentPositionFlow.asStateFlow()

    /** 文本块列表 */
    private var textBlocks: List<String> = emptyList()

    /** 当前朗读位置 */
    private var currentPosition: Int = 0

    /** 当前语速 */
    private var currentSpeechRate: Float = 2.0f

    /** 当前音调 */
    private var currentPitch: Float = 1.0f

    /** 当前语音 */
    private var currentVoice: TTSVoice? = null

    /** 播放完成回调 */
    private var onSpeakComplete: (() -> Unit)? = null

    /** 当前文本块的子句总数 */
    private var currentBlockTotalUtterances: Int = 0

    /** 当前文本块已完成的子句数 */
    private var currentBlockCompletedUtterances: Int = 0

    /** 初始化完成的Deferred */
    private var initDeferred: CompletableDeferred<Boolean>? = null

    /** 缓存的语音列表 */
    private var cachedVoices: List<TTSVoice> = emptyList()

    /** 全部播放完成回调 */
    private var onAllSpeakComplete: (() -> Unit)? = null

    /**
     * 初始化TTS引擎
     * 创建TextToSpeech实例并等待初始化完成
     */
    override suspend fun initialize(onInitComplete: (Boolean) -> Unit) {
        if (isInitialized) {
            Logger.d(TAG, "[VideoNote] TTS引擎已初始化，跳过")
            onInitComplete(true)
            return
        }

        // 如果正在初始化，等待完成
        initDeferred?.let { deferred ->
            Logger.d(TAG, "[VideoNote] TTS引擎正在初始化中，等待完成...")
            val success = deferred.await()
            onInitComplete(success)
            return
        }

        Logger.d(TAG, "[VideoNote] 开始初始化TTS引擎...")
        _stateFlow.value = TTSState.INITIALIZING

        // 创建新的Deferred来等待初始化完成
        val deferred = CompletableDeferred<Boolean>()
        initDeferred = deferred

        // 优先使用Activity Context，如果不可用则使用ApplicationContext
        val ttsContext = contextProvider.getActivityContext() ?: context.applicationContext
        Logger.d(TAG, "[VideoNote] 使用Context类型: ${if (contextProvider.getActivityContext() != null) "Activity Context" else "Application Context"}")

        // 延迟一段时间让TTS服务完全启动
        Logger.d(TAG, "[VideoNote] 等待TTS服务启动...")
        kotlinx.coroutines.delay(500)

        try {
            // 尝试使用Google TTS引擎
            Logger.d(TAG, "[VideoNote] 尝试创建Google TTS引擎实例...")
            try {
                // 使用三参数构造函数，明确指定使用Google TTS
                tts = TextToSpeech(ttsContext, this, "com.google.android.tts")
                Logger.d(TAG, "[VideoNote] Google TTS引擎实例已创建")
            } catch (e: Exception) {
                // 如果指定引擎失败，回退到系统默认
                Logger.w(TAG, "[VideoNote] 指定Google TTS失败，尝试系统默认: ${e.message}")
                tts = TextToSpeech(ttsContext, this)
                Logger.d(TAG, "[VideoNote] 系统默认TTS引擎实例已创建")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "[VideoNote] 创建TextToSpeech失败: ${e.message}", e)
            _stateFlow.value = TTSState.ERROR
            initDeferred = null
            deferred.complete(false)
            onInitComplete(false)
            return
        }

        // 等待初始化完成
        val success = deferred.await()
        initDeferred = null

        Logger.d(TAG, "[VideoNote] TTS引擎初始化${if (success) "成功" else "失败"}")
        onInitComplete(success)
    }

    /**
     * TextToSpeech初始化回调
     * 在TTS引擎初始化完成时调用
     */
    override fun onInit(status: Int) {
        Logger.d(TAG, "[VideoNote] onInit回调触发，状态: $status (SUCCESS=${TextToSpeech.SUCCESS}, ERROR=${TextToSpeech.ERROR})")

        // 尝试获取TTS引擎信息（无论成功或失败都记录）
        try {
            val availableEngines = tts?.engines
            Logger.d(TAG, "[VideoNote] 可用TTS引擎数量: ${availableEngines?.size ?: 0}")
            availableEngines?.forEach { engine ->
                Logger.d(TAG, "[VideoNote]   引擎: $engine")
            }

            val currentEngine = tts?.defaultEngine
            Logger.d(TAG, "[VideoNote] 当前TTS引擎: $currentEngine")
        } catch (e: Exception) {
            Logger.e(TAG, "[VideoNote] 获取TTS引擎信息失败: ${e.message}")
        }

        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            Logger.d(TAG, "[VideoNote] TTS初始化成功，设置播放监听器...")

            // 设置播放监听器
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Logger.d(TAG, "[VideoNote] TTS开始播放: $utteranceId")
                    _stateFlow.value = TTSState.PLAYING
                }

                override fun onDone(utteranceId: String?) {
                    Logger.d(TAG, "[VideoNote] TTS播放完成: $utteranceId, 已完成=${currentBlockCompletedUtterances}/${currentBlockTotalUtterances}")

                    // 增加已完成计数
                    currentBlockCompletedUtterances++

                    // 只有当当前块的所有子句都播放完成后，才播放下一块
                    if (currentBlockCompletedUtterances >= currentBlockTotalUtterances) {
                        Logger.d(TAG, "[VideoNote] 当前块所有子句播放完成，准备播放下一块")
                        onPlaybackComplete()
                    } else {
                        Logger.d(TAG, "[VideoNote] 等待更多子句完成... (${currentBlockCompletedUtterances}/${currentBlockTotalUtterances})")
                    }
                }

                @Suppress("OVERRIDE_DEPRECATION")
                override fun onError(utteranceId: String?) {
                    Logger.e(TAG, "[VideoNote] TTS播放错误: $utteranceId")
                    _stateFlow.value = TTSState.ERROR
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    Logger.d(TAG, "[VideoNote] TTS停止播放: $utteranceId, interrupted: $interrupted")
                    _stateFlow.value = TTSState.STOPPED
                }
            })

            // 设置语言 - 尝试多种语言
            var languageSet = false
            val localesToTry = listOf(
                Locale.CHINA,           // zh-CN
                Locale("zh", "CN"),     // 明确指定中文
                Locale.SIMPLIFIED_CHINESE,
                Locale.TRADITIONAL_CHINESE,
                Locale.ENGLISH,         // 回退到英文
                Locale.getDefault()     // 系统默认
            )

            for (locale in localesToTry) {
                try {
                    val result = tts?.setLanguage(locale)
                    Logger.d(TAG, "[VideoNote] 尝试设置语言 $locale，结果: $result")

                    if (result == TextToSpeech.LANG_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                        Logger.d(TAG, "[VideoNote] 成功设置语言: $locale")
                        languageSet = true
                        break
                    } else if (result == TextToSpeech.LANG_MISSING_DATA) {
                        Logger.w(TAG, "[VideoNote] 语言 $locale 缺少数据")
                    } else if (result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Logger.w(TAG, "[VideoNote] 语言 $locale 不支持")
                    }
                } catch (e: Exception) {
                    Logger.w(TAG, "[VideoNote] 设置语言 $locale 失败: ${e.message}")
                }
            }

            if (!languageSet) {
                Logger.w(TAG, "[VideoNote] 无法设置任何语言，TTS可能无法正常工作")
            }

            // 设置默认语速
            Logger.d(TAG, "[VideoNote] 设置默认语速: $currentSpeechRate")
            tts?.setSpeechRate(currentSpeechRate)

            _stateFlow.value = TTSState.STOPPED

            // 完成初始化Deferred
            initDeferred?.complete(true)
            Logger.d(TAG, "[VideoNote] TTS引擎初始化完成")

            // 缓存语音列表
            try {
                val availableVoices = tts?.voices
                cachedVoices = availableVoices?.map { voice ->
                    TTSVoice(
                        id = voice.name,
                        name = voice.name,
                        lang = voice.locale.toString(),
                        engineName = "android-tts",
                        isInstalled = true
                    )
                } ?: emptyList()

                // 分离中文语音和其他语音
                val chineseVoices = cachedVoices.filter {
                    it.lang.startsWith("zh") && it.lang.contains("CN")
                }

                // 中文语音优先排序
                cachedVoices = chineseVoices.sortedBy { it.lang }

                Logger.d(TAG, "[VideoNote] 已缓存 ${cachedVoices.size} 个语音，中文语音 ${chineseVoices.size} 个")
            } catch (e: Exception) {
                Logger.e(TAG, "[VideoNote] 缓存语音列表失败: ${e.message}")
            }

            // 输出可用语音信息
            try {
                val availableVoices = tts?.voices
                Logger.d(TAG, "[VideoNote] 可用语音数量: ${availableVoices?.size ?: 0}")
                availableVoices?.take(5)?.forEach { voice ->
                    Logger.d(TAG, "[VideoNote]   语音: ${voice.name} (${voice.locale})")
                }
            } catch (e: Exception) {
                Logger.e(TAG, "[VideoNote] 获取语音列表失败: ${e.message}")
            }
        } else {
            Logger.e(TAG, "[VideoNote] TTS初始化失败，状态码: $status")

            // 尝试获取更多诊断信息
            try {
                val availableVoices = tts?.voices
                Logger.d(TAG, "[VideoNote] 失败时可用语音数量: ${availableVoices?.size ?: 0}")
                availableVoices?.take(3)?.forEach { voice ->
                    Logger.d(TAG, "[VideoNote]   语音: ${voice.name} (${voice.locale})")
                }

                // 尝试获取当前语言设置
                @Suppress("DEPRECATION")
                val currentLanguage = tts?.language
                Logger.d(TAG, "[VideoNote] 当前语言: $currentLanguage")
            } catch (e: Exception) {
                Logger.e(TAG, "[VideoNote] 获取TTS信息失败: ${e.message}")
            }

            _stateFlow.value = TTSState.ERROR
            isInitialized = false

            // 初始化失败
            initDeferred?.complete(false)
        }
    }

    /**
     * 朗读文本块列表
     * @param blocks 文本块列表（与 blockIndex 一一对应）
     * @param startIndex 开始位置索引
     */
    override suspend fun speak(blocks: List<String>, startIndex: Int) {
        Logger.d(TAG, "[VideoNote] speak() 调用，isInitialized: $isInitialized, tts != null: ${tts != null}, blocks.size: ${blocks.size}, startIndex: $startIndex")
        if (tts == null) {
            Logger.e(TAG, "[VideoNote] TTS引擎未初始化，无法播放")
            throw IllegalStateException("TTS引擎未初始化")
        }

        // 设置文本块列表
        textBlocks = blocks

        // 停止当前播放（但不重置currentPosition）
        tts?.stop()

        // 如果明确指定了startIndex >= 0，则设置位置（用于从头开始播放）
        // 如果startIndex < 0，则保持当前位置（用于恢复播放）
        if (startIndex >= 0) {
            currentPosition = startIndex.coerceIn(0, textBlocks.lastIndex)
            _currentPositionFlow.value = currentPosition
        }
        // 如果startIndex < 0，保持currentPosition不变

        Logger.d(TAG, "[VideoNote] 开始播放第 $currentPosition 段，共 ${textBlocks.size} 段")
        speakCurrentBlock()
    }

    /**
     * 暂停播放
     */
    override suspend fun pause() {
        Logger.d(TAG, "[VideoNote] pause() 调用")
        if (!isInitialized) return

        tts?.stop()
        _stateFlow.value = TTSState.PAUSED
        // 重置子句计数器（恢复时会重新从当前位置开始播放所有子句）
        currentBlockCompletedUtterances = 0
        currentBlockTotalUtterances = 0
        Logger.d(TAG, "[VideoNote] TTS已暂停，计数器已重置")
    }

    /**
     * 恢复播放
     */
    override suspend fun resume() {
        Logger.d(TAG, "[VideoNote] resume() 调用，当前位置: $currentPosition")
        if (!isInitialized) return

        speakCurrentBlock()
    }

    /**
     * 停止播放
     */
    override suspend fun stop() {
        Logger.d(TAG, "[VideoNote] stop() 调用")
        if (!isInitialized) return

        tts?.stop()
        currentPosition = 0
        _currentPositionFlow.value = 0
        _stateFlow.value = TTSState.STOPPED
        // 重置子句计数器
        currentBlockCompletedUtterances = 0
        currentBlockTotalUtterances = 0
        Logger.d(TAG, "[VideoNote] TTS已停止，计数器已重置")
    }

    /**
     * 前进到下一段
     */
    override suspend fun forward(autoPlay: Boolean) {
        Logger.d(TAG, "[VideoNote] forward() 调用，autoPlay: $autoPlay")
        if (textBlocks.isEmpty()) return

        val nextPosition = (currentPosition + 1).coerceAtMost(textBlocks.lastIndex)
        currentPosition = nextPosition
        _currentPositionFlow.value = nextPosition

        Logger.d(TAG, "[VideoNote] 前进到第 $currentPosition 段")
        if (autoPlay) {
            speakCurrentBlock()
        }
    }

    /**
     * 后退到上一段
     */
    override suspend fun backward(autoPlay: Boolean) {
        Logger.d(TAG, "[VideoNote] backward() 调用，autoPlay: $autoPlay")
        if (textBlocks.isEmpty()) return

        val prevPosition = (currentPosition - 1).coerceAtLeast(0)
        currentPosition = prevPosition
        _currentPositionFlow.value = prevPosition

        Logger.d(TAG, "[VideoNote] 后退到第 $currentPosition 段")
        if (autoPlay) {
            speakCurrentBlock()
        }
    }

    /**
     * 设置语速
     */
    override suspend fun setSpeechRate(rate: Float) {
        Logger.d(TAG, "[VideoNote] setSpeechRate() 调用: $rate")
        if (!isInitialized) return

        val validRate = rate.coerceIn(0.2f, 3.0f)
        currentSpeechRate = validRate
        tts?.setSpeechRate(validRate)
        Logger.d(TAG, "[VideoNote] 语速已设置为: $validRate")
    }

    /**
     * 设置音调
     */
    override suspend fun setPitch(pitch: Float) {
        Logger.d(TAG, "[VideoNote] setPitch() 调用: $pitch")
        if (!isInitialized) return

        val validPitch = pitch.coerceIn(0.5f, 2.0f)
        currentPitch = validPitch
        tts?.setPitch(validPitch)
        Logger.d(TAG, "[VideoNote] 配置已设置为: $validPitch")
    }

    /**
     * 设置语音
     */
    override suspend fun setVoice(voice: TTSVoice) {
        Logger.d(TAG, "[VideoNote] setVoice() 调用: ${voice.name} (${voice.lang})")
        if (!isInitialized) return

        currentVoice = voice

        // 查找并设置对应的系统语音
        val voices = tts?.voices ?: return
        val systemVoice = voices.find {
            it.locale.toString() == voice.lang || it.name == voice.name
        }

        systemVoice?.let {
            tts?.setVoice(it)
            tts?.language = it.locale
            Logger.d(TAG, "[VideoNote] 语音已设置为: ${it.name}")
        } ?: Logger.w(TAG, "[VideoNote] 未找到匹配的系统语音")
    }

    /**
     * 获取所有可用语音
     * 优先返回缓存的语音列表，中文优先
     */
    override suspend fun getAvailableVoices(): List<TTSVoice> {
        Logger.d(TAG, "[VideoNote] getAvailableVoices() 调用，缓存语音数: ${cachedVoices.size}, tts != null: ${tts != null}")

        // 优先返回缓存的语音列表
        if (cachedVoices.isNotEmpty()) {
            Logger.d(TAG, "[VideoNote] 返回缓存的 ${cachedVoices.size} 个语音")
            // 记录前5个中文语音
            val chineseVoices = cachedVoices.filter { it.lang.startsWith("zh") }
            chineseVoices.take(5).forEach { voice ->
                Logger.d(TAG, "[VideoNote]   中文语音: ${voice.name} (${voice.lang})")
            }
            return cachedVoices
        }

        // 如果缓存为空，尝试从TTS获取
        Logger.d(TAG, "[VideoNote] 缓存为空，尝试从TTS获取语音")
        val ttsInstance = tts
        if (ttsInstance == null) {
            Logger.w(TAG, "[VideoNote] TTS对象为null，无法获取语音")
            return emptyList()
        }

        val allVoices = ttsInstance.voices?.map { voice ->
            TTSVoice(
                id = voice.name,
                name = voice.name,
                lang = voice.locale.toString(),
                engineName = "android-tts",
                isInstalled = true
            )
        } ?: emptyList()

        // 缓存结果
        val chineseVoices = allVoices.filter {
            it.lang.startsWith("zh") || it.lang.contains("CN") || it.lang.contains("TW") || it.lang.contains("HK")
        }
        cachedVoices = chineseVoices.sortedBy { it.lang } + allVoices.filter {
            !it.lang.startsWith("zh") && !it.lang.contains("CN") && !it.lang.contains("TW") && !it.lang.contains("HK")
        }.sortedBy { it.lang }

        Logger.d(TAG, "[VideoNote] 从TTS获取并缓存了 ${cachedVoices.size} 个语音，中文语音 ${chineseVoices.size} 个")
        return cachedVoices
    }

    /**
     * 设置文本块列表
     * 注意：不再重置currentPosition，以支持从暂停位置继续播放
     */
    override fun setTextBlocks(blocks: List<String>) {
        Logger.d(TAG, "[VideoNote] setTextBlocks() 调用，块数: ${blocks.size}, 当前位置: $currentPosition")
        textBlocks = blocks
    }

    /**
     * 设置当前播放位置
     * 用于从保存的进度恢复播放
     */
    fun setCurrentPosition(position: Int) {
        currentPosition = position.coerceIn(0, if (textBlocks.isNotEmpty()) textBlocks.lastIndex else 0)
        _currentPositionFlow.value = currentPosition
        Logger.d(TAG, "[VideoNote] setCurrentPosition: $currentPosition")
    }

    /**
     * 获取当前播放位置
     */
    fun getCurrentPosition(): Int = currentPosition

    /**
     * 释放资源
     */
    override fun shutdown() {
        Logger.d(TAG, "[VideoNote] shutdown() 调用")
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        _stateFlow.value = TTSState.STOPPED
        Logger.d(TAG, "[VideoNote] TTS引擎已释放资源")
    }

    /**
     * 朗读当前文本块
     *
     * 设计说明：
     * 1. 将文本块按标点符号分割成多个子句
     * 2. 使用 QUEUE_ADD 将所有子句添加到播放队列
     * 3. 通过 UtteranceProgressListener.onDone 追踪每个子句的完成状态
     * 4. 只有当所有子句都完成后，才播放下一块
     *
     * 注意：tts.speak() 是异步操作，立即返回，实际播放在后台进行
     */
    private fun speakCurrentBlock() {
        if (!isInitialized || textBlocks.isEmpty()) return

        val text = textBlocks[currentPosition]
        if (text.isEmpty()) return

        Logger.d(TAG, "[VideoNote] speakCurrentBlock: 位置=$currentPosition, 文本长度=${text.length}")

        // 确保语速设置生效
        Logger.d(TAG, "[VideoNote] 确认语速设置: $currentSpeechRate")
        tts?.setSpeechRate(currentSpeechRate)

        // 重置子句计数器
        currentBlockCompletedUtterances = 0

        // 按标点符号分割： , ; \n  ! ? 。 ， ；：、 "
        // 过滤 # | -- 等符号和空字符串
        val liststr = text.split(Regex("(?<=[,;\\n\\!\\?\"。，；：、“])"))
            .map { it.replace(Regex("[#|]"), "").replace("--", "").replace("**", "").trim() }
            .filter { it.isNotEmpty()  }

        // 记录子句总数
        currentBlockTotalUtterances = liststr.size

        Logger.d(TAG, "[VideoNote] 分割后的句子数量: $currentBlockTotalUtterances")

        // 将所有子句添加到播放队列
        for (i in liststr.indices) {
            val part = liststr[i]
            val utteranceId = "utterance_${currentPosition}_$i"

            // 第一个句子用 QUEUE_FLUSH 清空队列，后续用 QUEUE_ADD 追加
            val queueMode = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD

            val result = tts?.speak(
                part,
                queueMode,
                null,
                utteranceId
            )

            Logger.d(TAG, "[VideoNote] 添加句子[$i/${liststr.size}]: ${part.take(30)}..., queueMode=$queueMode, result=$result")

            // 子句间添加微小延迟（让队列添加有时间间隔）
            if (i < liststr.size - 1) {
                try {
                    Thread.sleep(50)
                } catch (e: InterruptedException) {
                    Logger.w(TAG, "[VideoNote] 子句间延迟被中断: ${e.message}")
                }
            }
        }

        Logger.d(TAG, "[VideoNote] 所有子句已添加到队列，等待播放完成...")
    }

    /**
     * 播放完成处理
     */
    private fun onPlaybackComplete() {
        Logger.d(TAG, "[VideoNote] onPlaybackComplete: 当前位置=$currentPosition, 总块数=${textBlocks.size}")
        // 检查是否还有下一段
        if (currentPosition < textBlocks.lastIndex) {
            // 自动播放下一段
            currentPosition++
            _currentPositionFlow.value = currentPosition
            Logger.d(TAG, "[VideoNote] 自动播放下一段: $currentPosition")
            speakCurrentBlock()
        } else {
            // 全部播放完成
            Logger.d(TAG, "[VideoNote] 所有段落播放完成，重置位置为0")
            _stateFlow.value = TTSState.STOPPED

            // 重置当前位置为0，方便下次从头开始播放
            currentPosition = 0
            _currentPositionFlow.value = 0

            // 触发全部完成回调
            onSpeakComplete?.invoke()
            onAllSpeakComplete?.invoke()
        }
    }

    /**
     * 设置全部播放完成回调
     * 当所有文本块播放完成时调用此回调
     */
    override fun setOnAllSpeakCompleteCallback(callback: (() -> Unit)?) {
        onAllSpeakComplete = callback
        Logger.d(TAG, "[VideoNote] setOnAllSpeakCompleteCallback: ${if (callback != null) "已设置" else "已清除"}")
    }

    companion object {
        private const val TAG = "VideoNote"
    }
}
