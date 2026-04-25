package com.videonote.data.tts

import com.videonote.domain.model.TTSState
import com.videonote.domain.model.TTSVoice
import kotlinx.coroutines.flow.Flow

/**
 * TTS引擎接口 - 定义TTS引擎的通用操作接口
 * 参考TTS文档中的TTSClient接口设计，支持多引擎扩展
 *
 * 此接口定义了所有TTS引擎必须实现的核心功能：
 * - 播放控制（播放、暂停、停止、前进、后退）
 * - 语音和语速设置
 * - 状态监听
 * - 引擎生命周期管理
 */
interface TTSEngine {

    /**
     * TTS状态流 - 播放状态的响应式流
     * 用于UI层订阅和响应TTS状态变化
     */
    val stateFlow: Flow<TTSState>

    /**
     * 当前朗读位置流 - 当前朗读文本块的索引
     * 用于UI高亮显示当前朗读内容
     */
    val currentPositionFlow: Flow<Int>

    /**
     * 初始化TTS引擎
     * 必须在使用前调用，确保TTS引擎已准备就绪
     *
     * @param onInitComplete 初始化完成回调，返回是否成功
     */
    suspend fun initialize(onInitComplete: (Boolean) -> Unit = {})

    /**
     * 朗读文本块列表
     * 开始朗读指定的文本块列表
     *
     * @param blocks 要朗读的文本块列表（与 blockIndex 一一对应）
     * @param startIndex 开始索引，用于从指定位置开始朗读
     */
    suspend fun speak(blocks: List<String>, startIndex: Int = 0)

    /**
     * 暂停播放
     * 暂停当前朗读，可后续恢复
     */
    suspend fun pause()

    /**
     * 恢复播放
     * 从暂停位置继续朗读
     */
    suspend fun resume()

    /**
     * 停止播放
     * 完全停止朗读并重置位置
     */
    suspend fun stop()

    /**
     * 前进到下一段
     * 跳到下一个文本块并朗读
     *
     * @param autoPlay 是否自动开始播放，默认true
     */
    suspend fun forward(autoPlay: Boolean = true)

    /**
     * 后退到上一段
     * 跳到上一个文本块并朗读
     *
     * @param autoPlay 是否自动开始播放，默认true
     */
    suspend fun backward(autoPlay: Boolean = true)

    /**
     * 设置语速
     *
     * @param rate 语速值，范围0.2f - 3.0f
     */
    suspend fun setSpeechRate(rate: Float)

    /**
     * 设置音调
     *
     * @param pitch 音调值，范围0.5f - 2.0f
     */
    suspend fun setPitch(pitch: Float)

    /**
     * 设置语音
     *
     * @param voice 要使用的语音对象
     */
    suspend fun setVoice(voice: TTSVoice)

    /**
     * 获取所有可用语音列表
     *
     * @return 可用语音列表
     */
    suspend fun getAvailableVoices(): List<TTSVoice>

    /**
     * 设置文本块列表
     * 用于支持分段朗读和导航
     *
     * @param blocks 文本块列表
     */
    fun setTextBlocks(blocks: List<String>)

    /**
     * 设置全部播放完成回调
     * 当所有文本块播放完成时调用此回调
     *
     * @param callback 播放完成回调函数
     */
    fun setOnAllSpeakCompleteCallback(callback: (() -> Unit)?)

    /**
     * 释放资源
     * 在不再使用TTS引擎时调用，释放系统资源
     */
    fun shutdown()
}
