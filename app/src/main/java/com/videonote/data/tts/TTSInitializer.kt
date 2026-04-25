package com.videonote.data.tts

import com.videonote.util.Logger
import kotlinx.coroutines.CompletableDeferred
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TTS初始化管理器
 * 负责在Activity Context设置完成后初始化TTS引擎
 */
@Singleton
class TTSInitializer @Inject constructor(
    private val contextProvider: ContextProvider,
    private val ttsEngine: AndroidTTSEngine
) {
    private var isInitialized = false
    private val TAG = "TTSInitializer"

    /** 初始化完成回调列表 */
    private val initCallbacks = mutableListOf<suspend () -> Unit>()

    /**
     * 注册初始化完成回调
     */
    fun onInitComplete(callback: suspend () -> Unit) {
        initCallbacks.add(callback)
        // 如果已经初始化，立即调用回调
        if (isInitialized) {
            Logger.d(TAG, "[VideoNote] TTS已初始化，立即调用回调")
            return
        }
    }

    /**
     * 尝试初始化TTS引擎
     * 如果Activity Context还没有设置，会等待最多1秒
     */
    suspend fun initialize(): Boolean {
        if (isInitialized) {
            Logger.d(TAG, "[VideoNote] TTS已经初始化，跳过")
            return true
        }

        Logger.d(TAG, "[VideoNote] 开始初始化TTS...")

        // 等待Activity Context可用，最多等待1秒
        var retries = 0
        while (contextProvider.getActivityContext() == null && retries < 10) {
            Logger.d(TAG, "[VideoNote] 等待Activity Context... (${retries + 1}/10)")
            kotlinx.coroutines.delay(100)
            retries++
        }

        if (contextProvider.getActivityContext() == null) {
            Logger.w(TAG, "[VideoNote] Activity Context不可用，使用Application Context")
        } else {
            Logger.d(TAG, "[VideoNote] Activity Context已就绪")
        }

        return try {
            // 使用CompletableDeferred等待初始化完成
            val deferred = CompletableDeferred<Boolean>()
            ttsEngine.initialize { success ->
                isInitialized = success
                Logger.d(TAG, "[VideoNote] TTS初始化${if (success) "成功" else "失败"}")
                deferred.complete(success)

                // 初始化完成后调用所有回调
                if (success) {
                    Logger.d(TAG, "[VideoNote] 调用 ${initCallbacks.size} 个初始化完成回调")
                }
            }
            val result = deferred.await()

            // 通知所有监听器
            if (result) {
                initCallbacks.forEach { it() }
            }

            result
        } catch (e: Exception) {
            Logger.e(TAG, "[VideoNote] TTS初始化异常: ${e.message}", e)
            false
        }
    }

    fun reset() {
        isInitialized = false
    }
}
