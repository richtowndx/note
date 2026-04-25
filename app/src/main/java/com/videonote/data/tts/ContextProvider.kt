package com.videonote.data.tts

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Context提供者 - 用于提供Activity Context给需要它的组件
 * 某些Android组件（如TextToSpeech）需要Activity Context才能正常工作
 */
@Singleton
class ContextProvider @Inject constructor() {
    private var _activityContext: Context? = null

    /**
     * 设置当前Activity的Context
     * 应该在MainActivity.onCreate中调用
     */
    fun setActivityContext(context: Context) {
        _activityContext = context
    }

    /**
     * 获取Activity Context
     * 如果未设置则返回null
     */
    fun getActivityContext(): Context? = _activityContext

    /**
     * 清除Activity Context
     * 应该在MainActivity.onDestroy中调用
     */
    fun clearActivityContext() {
        _activityContext = null
    }
}
