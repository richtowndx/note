package com.videonote.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.videonote.util.Logger
import androidx.lifecycle.viewModelScope
import com.videonote.data.tts.ContextProvider
import com.videonote.data.tts.TTSEngine
import com.videonote.data.tts.AndroidTTSEngine
import com.videonote.data.local.database.dao.NoteDao
import com.videonote.domain.model.*
import com.videonote.presentation.ui.markdown.splitMarkdownIntoTTSBlocks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * TTS ViewModel - 管理TTS功能的UI状态和业务逻辑
 * 参考TTS文档中的TTSController设计，适配Android平台
 *
 * 功能：
 * - 播放控制（播放、暂停、停止、前进、后退）
 * - 语速、音调调节
 * - 语音切换
 * - 定时停止
 * - 文本分段朗读
 */
@HiltViewModel
class TTSViewModel @Inject constructor(
    private val ttsEngine: AndroidTTSEngine,
    private val contextProvider: ContextProvider,
    private val noteDao: NoteDao
) : ViewModel() {

    /** UI状态的私有可变StateFlow */
    private val _uiState = MutableStateFlow(TTSUiState())
    val uiState: StateFlow<TTSUiState> = _uiState.asStateFlow()

    /** 定时器任务 */
    private var timerJob: Job? = null

    /** 进度保存防抖任务 */
    private var saveProgressJob: Job? = null

    /** 初始化状态 */
    private var isEngineInitialized = false

    /** 初始化重试次数 */
    private var initRetryCount = 0

    /** 当前页面显示的笔记ID */
    private var displayNoteId: String? = null

    /** 是否正在初始化 */
    private var isInitializing = false

    private var voicesLoaded = false

    init {
        Logger.d(TAG, "[VideoNote] TTSViewModel init块被调用")

        // 设置全部播放完成回调
        ttsEngine.setOnAllSpeakCompleteCallback {
            Logger.d(TAG, "[VideoNote] TTS全部播放完成，重置进度")
            viewModelScope.launch {
                // 重置数据库中的进度为0
                _uiState.value.playingNoteId?.let { noteId ->
                    try {
                        noteDao.updateTtsProgress(noteId, 0)
                        Logger.d(TAG, "[VideoNote] 已重置笔记进度: noteId=$noteId, progress=0")
                    } catch (e: Exception) {
                        Logger.e(TAG, "[VideoNote] 重置笔记进度失败: ${e.message}", e)
                    }
                }
                // 重置UI状态
                _uiState.value = _uiState.value.copy(
                    currentPosition = 0,
                    displayNoteProgress = 0,
                    playingNoteId = null
                )
            }
        }

        // 监听TTS状态变化
        viewModelScope.launch {
            ttsEngine.stateFlow.collect { state ->
                Logger.d(TAG, "[VideoNote] TTS状态变化: $state")
                _uiState.value = _uiState.value.copy(
                    ttsState = state,
                    isPlaying = state == TTSState.PLAYING,
                    isPaused = state == TTSState.PAUSED,
                    error = if (state == TTSState.ERROR && _uiState.value.error == null) {
                        "TTS服务错误，请稍后重试"
                    } else {
                        _uiState.value.error
                    }
                )
            }
        }

        // 监听当前位置变化
        viewModelScope.launch {
            ttsEngine.currentPositionFlow.collect { position ->
                Logger.d(TAG, "[VideoNote] TTS位置变化: $position")
                val textBlocks = _uiState.value.textBlocks
                val currentBlock = if (position >= 0 && position < textBlocks.size) {
                    textBlocks[position]
                } else {
                    null
                }
                _uiState.value = _uiState.value.copy(
                    currentPosition = position,
                    currentTextBlock = currentBlock
                )
                // P1-4 修复：添加防抖机制，500ms 后才保存，减少频繁数据库写入
                saveProgressJob?.cancel()
                saveProgressJob = viewModelScope.launch {
                    delay(500)
                    saveTtsProgress()
                }
            }
        }

        // 延迟检查并加载语音列表
        viewModelScope.launch {
            // 等待TTS初始化完成（最多3秒）
            var checkCount = 0
            while (!voicesLoaded && checkCount < 30) {
                kotlinx.coroutines.delay(100)
                checkCount++

                // 检查TTS是否已初始化（通过检查state不是ERROR且不是初始化中）
                val currentState = _uiState.value.ttsState
                if (currentState == TTSState.STOPPED && checkCount > 5) {
                    // 已经至少等待了500ms，假设TTS已初始化
                    voicesLoaded = true
                    isEngineInitialized = true
                    Logger.d(TAG, "[VideoNote] 检测到TTS已就绪，开始加载语音列表")
                    loadAvailableVoices()
                    break
                }
            }

            if (!voicesLoaded) {
                Logger.w(TAG, "[VideoNote] 等待TTS初始化超时，尝试加载语音列表")
                voicesLoaded = true
                loadAvailableVoices()
            }
        }
    }

    /**
     * 手动初始化TTS引擎
     * 应该在MainActivity设置Context后调用
     */
    fun initializeTTS() {
        Logger.d(TAG, "[VideoNote] initializeTTS() 被手动调用")
        initializeEngine()
    }

    /**
     * 加载笔记的TTS设置（不开始播放，不改变正在播放的笔记）
     * 用于切换笔记时预先加载该笔记的TTS配置到显示状态
     *
     * @param noteId 笔记ID
     */
    fun loadNoteTtsSettings(noteId: String) {
        displayNoteId = noteId
        viewModelScope.launch {
            try {
                val noteEntity = noteDao.getNoteById(noteId)
                if (noteEntity != null) {
                    var savedProgress = noteEntity.ttsProgress ?: 0
                    Logger.d(TAG, "[VideoNote] 加载显示笔记TTS设置: noteId=$noteId, progress=$savedProgress, speechRate=${noteEntity.ttsSpeechRate}, pitch=${noteEntity.ttsPitch}, voiceId=${noteEntity.ttsVoiceId}")

                    // P3-1 修复：验证并修正进度，防止笔记内容更新后进度超出范围
                    val markdownContent = noteEntity.markdownContent ?: ""
                    if (markdownContent.isNotEmpty()) {
                        val textBlocks = splitMarkdownIntoTTSBlocks(markdownContent)
                        val totalBlocks = textBlocks.size

                        if (savedProgress >= totalBlocks && totalBlocks > 0) {
                            Logger.w(TAG, "[VideoNote] 保存的进度($savedProgress) 超出范围(0-$totalBlocks)，重置为0")
                            savedProgress = 0
                        }
                    }

                    // 检查是否有正在播放的笔记（且不是当前笔记）
                    val hasDifferentPlayingNote = (_uiState.value.playingNoteId != null &&
                                                   _uiState.value.playingNoteId != noteId &&
                                                   (_uiState.value.isPlaying || _uiState.value.isPaused))

                    if (hasDifferentPlayingNote) {
                        // 有其他笔记正在播放，保持播放状态，只记录显示笔记的进度
                        Logger.d(TAG, "[VideoNote] 有其他笔记正在播放，保持播放状态，记录显示笔记进度: $savedProgress")
                        // P0-2 修复：同步更新 currentPosition，避免 UI 显示不一致
                        _uiState.value = _uiState.value.copy(
                            displayNoteProgress = savedProgress,
                            currentPosition = savedProgress
                        )
                    } else {
                        // 没有其他笔记正在播放，或者正在播放的就是当前笔记
                        // P1-6 修复：不需要重置引擎位置，引擎已停止，只需更新 UI 状态
                        Logger.d(TAG, "[VideoNote] 没有其他笔记正在播放，记录显示笔记进度: $savedProgress")
                        _uiState.value = _uiState.value.copy(
                            currentPosition = 0,
                            displayNoteProgress = savedProgress
                        )
                    }

                    // 应用保存的TTS设置（语速、音调、语音）- 这些设置对所有笔记通用
                    noteEntity.ttsSpeechRate?.let { ttsEngine.setSpeechRate(it) }
                    noteEntity.ttsPitch?.let { ttsEngine.setPitch(it) }
                    noteEntity.ttsVoiceId?.let { voiceId ->
                        _uiState.value.availableVoices.find { it.id == voiceId }?.let { voice ->
                            ttsEngine.setVoice(voice)
                        }
                    }
                } else {
                    Logger.d(TAG, "[VideoNote] 笔记无TTS记录，使用默认设置")
                }
            } catch (e: Exception) {
                Logger.e(TAG, "[VideoNote] 加载笔记TTS设置失败: ${e.message}", e)
            }
        }
    }

    /**
     * 初始化TTS引擎
     * P2-2, P2-3, P2-4 修复：删除重复的监听器，只在 init 块中设置一次
     */
    private fun initializeEngine() {
        Logger.d(TAG, "[VideoNote] initializeEngine() 被调用，isEngineInitialized: $isEngineInitialized, isInitializing: $isInitializing")

        if (isEngineInitialized) {
            Logger.d(TAG, "[VideoNote] TTS引擎已初始化，跳过")
            return
        }

        if (isInitializing) {
            Logger.d(TAG, "[VideoNote] TTS引擎正在初始化中，跳过重复初始化")
            return
        }

        isInitializing = true
        Logger.d(TAG, "[VideoNote] 开始初始化TTS引擎...")

        viewModelScope.launch {
            try {
                Logger.d(TAG, "[VideoNote] 调用ttsEngine.initialize()...")
                ttsEngine.initialize { success ->
                    isEngineInitialized = success
                    isInitializing = false
                    Logger.d(TAG, "[VideoNote] TTS引擎初始化回调: $success")
                    if (!success) {
                        val errorMsg = "TTS引擎初始化失败。请检查：\n1. 设备是否安装了TTS语音服务\n2. 设置中是否启用了文本转语音\n3. 是否下载了语音包"
                        Logger.e(TAG, "[VideoNote] $errorMsg")
                        _uiState.value = _uiState.value.copy(
                            error = errorMsg
                        )
                    } else {
                        Logger.d(TAG, "[VideoNote] TTS引擎初始化成功！")
                        // P2-4 修复：初始化成功后加载语音列表（只加载一次）
                        loadAvailableVoices()
                    }
                }
            } catch (e: Exception) {
                Logger.e(TAG, "[VideoNote] TTS初始化异常: ${e.message}", e)
                isInitializing = false
                _uiState.value = _uiState.value.copy(
                    error = "TTS初始化失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 加载可用语音列表
     */
    private fun loadAvailableVoices() {
        viewModelScope.launch {
            try {
                Logger.d(TAG, "[VideoNote] 开始加载可用语音列表...")
                val voices = ttsEngine.getAvailableVoices()
                _uiState.value = _uiState.value.copy(
                    availableVoices = voices
                )

                Logger.d(TAG, "[VideoNote] 加载到 ${voices.size} 个语音")
                // 记录前10个语音
                voices.take(10).forEach { voice ->
                    Logger.d(TAG, "[VideoNote] 语音: ${voice.name} (${voice.lang})")
                }

                // 设置默认语音（中文优先）
                val defaultVoice = voices.find { it.lang.startsWith("zh") }
                    ?: voices.firstOrNull()
                defaultVoice?.let {
                    Logger.d(TAG, "[VideoNote] 设置默认语音: ${it.name} (${it.lang})")
                    setVoice(it)
                }
            } catch (e: Exception) {
                Logger.e(TAG, "[VideoNote] 加载语音列表失败: ${e.message}", e)
            }
        }
    }

    /**
     * 开始播放文本
     *
     * @param text 要朗读的文本
     * @param blocks 文本块列表（可选），用于分段朗读
     * @param fromStart 是否从头开始播放，true=从头开始，false=从保存的进度继续（默认true）
     */
    fun play(text: String, blocks: List<String>? = null, fromStart: Boolean = true) {
        Logger.d(TAG, "[VideoNote] play() 调用，文本长度: ${text.length}, fromStart: $fromStart")
        viewModelScope.launch {
            try {
                // 如果正在播放或暂停，先停止并保存当前播放笔记的进度
                if (_uiState.value.isPlaying || _uiState.value.isPaused) {
                    Logger.d(TAG, "[VideoNote] 正在播放/暂停，先停止并保存当前播放笔记的进度")
                    ttsEngine.stop()
                    saveTtsProgress()
                    // 重置UI状态
                    _uiState.value = _uiState.value.copy(
                        currentPosition = 0,
                        isPlaying = false,
                        isPaused = false,
                        playingNoteId = null
                    )
                }

                // P1-5 修复：统一使用 playingNoteId，删除冗余的 currentNoteId 变量
                _uiState.value = _uiState.value.copy(
                    playingNoteId = displayNoteId
                )

                // 设置文本块（与 blockIndex 一一对应）
                val blocksToUse: List<String>
                if (blocks != null) {
                    Logger.d(TAG, "[VideoNote] 使用预设文本块，数量: ${blocks.size}")
                    blocksToUse = blocks
                } else {
                    // 使用与 parseMarkdownWithIds 相同的分割逻辑
                    blocksToUse = splitMarkdownIntoTTSBlocks(text)
                    Logger.d(TAG, "[VideoNote] 从 Markdown 提取文本块，数量: ${blocksToUse.size}")
                }

                val totalBlocks = blocksToUse.size
                _uiState.value = _uiState.value.copy(
                    totalBlocks = totalBlocks,
                    textBlocks = blocksToUse
                )

                // 计算实际播放的起始位置
                val displayProgress = _uiState.value.displayNoteProgress
                Logger.d(TAG, "[VideoNote] 显示笔记进度: $displayProgress, 总块数: $totalBlocks")

                val startIndex: Int = when {
                    // 明确要求从头开始
                    fromStart -> {
                        Logger.d(TAG, "[VideoNote] fromStart=true，从0开始")
                        0
                    }
                    // 保存的进度超出范围或无效，从头开始
                    displayProgress >= totalBlocks || displayProgress < 0 -> {
                        Logger.d(TAG, "[VideoNote] 显示笔记进度($displayProgress)超出总块数($totalBlocks)或无效，从0开始")
                        0
                    }
                    // 从保存的进度继续
                    else -> {
                        Logger.d(TAG, "[VideoNote] 从保存的进度继续: $displayProgress")
                        displayProgress
                    }
                }

                // 开始播放（直接传递文本块列表）
                Logger.d(TAG, "[VideoNote] 调用TTS引擎播放，startIndex: $startIndex, blocks: ${blocksToUse.size}, playingNoteId=${_uiState.value.playingNoteId}")
                ttsEngine.speak(blocksToUse, startIndex)
            } catch (e: Exception) {
                Logger.e(TAG, "[VideoNote] 播放失败: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "播放失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 暂停播放
     * 1. 保存正在播放笔记的进度
     * 2. 停止播放
     * 3. 更新 UI 状态为当前实际位置
     */
    fun pause() {
        Logger.d(TAG, "[VideoNote] pause() 调用")
        viewModelScope.launch {
            try {
                // P0-3 修复：获取实际播放位置并保存
                val actualPosition = ttsEngine.getCurrentPosition()
                Logger.d(TAG, "[VideoNote] 当前播放位置: $actualPosition")

                // P1-5 修复：使用 playingNoteId 保存进度
                _uiState.value.playingNoteId?.let { noteId ->
                    try {
                        noteDao.updateTtsProgress(noteId, actualPosition)
                        Logger.d(TAG, "[VideoNote] TTS进度已保存: noteId=$noteId, progress=$actualPosition")
                    } catch (e: Exception) {
                        Logger.e(TAG, "[VideoNote] 保存TTS进度失败: ${e.message}", e)
                    }
                }

                // 暂停播放
                ttsEngine.pause()
                stopTimer()

                // P2-1 修复：删除未使用的 let 表达式，直接更新 UI 状态
                _uiState.value = _uiState.value.copy(
                    displayNoteProgress = actualPosition,
                    currentPosition = actualPosition
                )

                Logger.d(TAG, "[VideoNote] 暂停成功")
            } catch (e: Exception) {
                Logger.e(TAG, "[VideoNote] 暂停失败: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "暂停失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 保存TTS播放进度到数据库
     */
    private suspend fun saveTtsProgress() {
        // P1-5 修复：使用 playingNoteId 而非 currentNoteId
        val noteId = _uiState.value.playingNoteId ?: return
        val currentPosition = ttsEngine.getCurrentPosition()
        try {
            noteDao.updateTtsProgress(noteId, currentPosition)
            Logger.d(TAG, "[VideoNote] TTS进度已保存: noteId=$noteId, progress=$currentPosition")
        } catch (e: Exception) {
            Logger.e(TAG, "[VideoNote] 保存TTS进度失败: ${e.message}", e)
        }
    }

    /**
     * 公共方法：保存当前播放进度
     * 用于在页面切换或笔记切换时保存进度
     */
    fun saveCurrentProgress() {
        viewModelScope.launch {
            saveTtsProgress()
        }
    }

    /**
     * 恢复播放
     */
    fun resume() {
        Logger.d(TAG, "[VideoNote] resume() 调用")
        viewModelScope.launch {
            try {
                ttsEngine.resume()
                // 如果设置了定时器，恢复播放时也需要恢复定时
                if (_uiState.value.timer.enabled) {
                    Logger.d(TAG, "[VideoNote] 恢复定时器: ${_uiState.value.timer.minutes}分钟")
                    startTimer(_uiState.value.timer.minutes)
                }
                Logger.d(TAG, "[VideoNote] 恢复成功")
            } catch (e: Exception) {
                Logger.e(TAG, "[VideoNote] 恢复失败: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "恢复失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 停止播放
     */
    fun stop() {
        Logger.d(TAG, "[VideoNote] stop() 调用")
        viewModelScope.launch {
            try {
                // 保存当前播放进度
                saveTtsProgress()

                ttsEngine.stop()
                stopTimer()
                _uiState.value = _uiState.value.copy(
                    currentPosition = 0,
                    playingNoteId = null
                )
                Logger.d(TAG, "[VideoNote] 停止成功")
            } catch (e: Exception) {
                Logger.e(TAG, "[VideoNote] 停止失败: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "停止失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 前进到下一段
     */
    fun forward() {
        Logger.d(TAG, "[VideoNote] forward() 调用，isPlaying: ${_uiState.value.isPlaying}")
        viewModelScope.launch {
            try {
                ttsEngine.forward(autoPlay = _uiState.value.isPlaying)
            } catch (e: Exception) {
                Logger.e(TAG, "[VideoNote] 前进失败: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "前进失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 后退到上一段
     */
    fun backward() {
        Logger.d(TAG, "[VideoNote] backward() 调用，isPlaying: ${_uiState.value.isPlaying}")
        viewModelScope.launch {
            try {
                ttsEngine.backward(autoPlay = _uiState.value.isPlaying)
            } catch (e: Exception) {
                Logger.e(TAG, "[VideoNote] 后退失败: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "后退失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 设置语速
     *
     * @param rate 语速值，范围0.2f - 3.0f
     */
    fun setSpeechRate(rate: Float) {
        viewModelScope.launch {
            try {
                ttsEngine.setSpeechRate(rate)
                _uiState.value = _uiState.value.copy(
                    settings = _uiState.value.settings.withRate(rate)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "设置语速失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 设置音调
     *
     * @param pitch 音调值，范围0.5f - 2.0f
     */
    fun setPitch(pitch: Float) {
        viewModelScope.launch {
            try {
                ttsEngine.setPitch(pitch)
                _uiState.value = _uiState.value.copy(
                    settings = _uiState.value.settings.withPitch(pitch)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "设置音调失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 设置语音
     *
     * @param voice 要使用的语音对象
     */
    fun setVoice(voice: TTSVoice) {
        viewModelScope.launch {
            try {
                ttsEngine.setVoice(voice)
                _uiState.value = _uiState.value.copy(
                    settings = _uiState.value.settings.withVoice(voice.id),
                    selectedVoice = voice
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "设置语音失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 设置定时停止
     *
     * @param minutes 定时分钟数，0表示关闭定时
     */
    fun setTimer(minutes: Int) {
        stopTimer()

        val newTimer = TTSTimer(enabled = minutes > 0, minutes = minutes)
        _uiState.value = _uiState.value.copy(timer = newTimer)

        if (minutes > 0) {
            startTimer(minutes)
        }
    }

    /**
     * 启动定时器
     */
    private fun startTimer(minutes: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            delay(minutes * 60L * 1000)
            stop()
        }
    }

    /**
     * 停止定时器
     */
    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * ViewModel 清理资源
     * P3-3 修复：取消所有正在进行的任务，并立即保存当前进度
     */
    override fun onCleared() {
        super.onCleared()
        Logger.d(TAG, "[VideoNote] TTSViewModel onCleared")

        // 清除全部播放完成回调
        ttsEngine.setOnAllSpeakCompleteCallback(null)

        // 取消所有正在进行的任务
        saveProgressJob?.cancel()
        timerJob?.cancel()

        // 立即保存当前进度（不使用防抖）
        if (_uiState.value.playingNoteId != null) {
            viewModelScope.launch {
                try {
                    val noteId = _uiState.value.playingNoteId!!
                    val position = ttsEngine.getCurrentPosition()
                    noteDao.updateTtsProgress(noteId, position)
                    Logger.d(TAG, "[VideoNote] onCleared 保存进度: noteId=$noteId, progress=$position")
                } catch (e: Exception) {
                    Logger.e(TAG, "[VideoNote] onCleared 保存进度失败", e)
                }
            }
        }
    }

    companion object {
        private const val TAG = "VideoNote.TTSViewModel"
    }
}

/**
 * TTS UI状态数据类
 */
data class TTSUiState(
    /** TTS播放状态 */
    val ttsState: TTSState = TTSState.STOPPED,

    /** 是否正在播放 */
    val isPlaying: Boolean = false,

    /** 是否已暂停 */
    val isPaused: Boolean = false,

    /** 当前朗读位置（文本块索引） */
    val currentPosition: Int = 0,

    /** 总文本块数 */
    val totalBlocks: Int = 0,

    /** 文本块列表 */
    val textBlocks: List<String> = emptyList(),

    /** 当前朗读的文本块（用于UI高亮显示） */
    val currentTextBlock: String? = null,

    /** TTS设置 */
    val settings: TTSSettings = TTSSettings(),

    /** 当前选中的语音 */
    val selectedVoice: TTSVoice? = null,

    /** 可用语音列表 */
    val availableVoices: List<TTSVoice> = emptyList(),

    /** 定时器设置 */
    val timer: TTSTimer = TTSTimer(),

    /** 错误信息 */
    val error: String? = null,

    /** 当前正在播放的笔记ID（可能与显示的笔记不同） */
    val playingNoteId: String? = null,

    /** 显示笔记的播放进度（用于在UI上显示当前笔记的进度，即使正在播放其他笔记） */
    val displayNoteProgress: Int = 0
)
