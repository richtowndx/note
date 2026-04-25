package com.videonote.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.videonote.data.tts.AndroidTTSEngine
import com.videonote.data.tts.ContextProvider
import com.videonote.data.tts.TTSInitializer
import com.videonote.presentation.navigation.NavigationState
import com.videonote.presentation.navigation.VideoNoteNavigation
import com.videonote.presentation.ui.theme.VideoNoteTheme
import com.videonote.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 主活动类 - 应用的主界面和入口点
 * 使用Jetpack Compose构建现代Android UI
 * 使用Hilt进行依赖注入，支持MVVM架构模式
 * 管理应用的主要导航和UI主题设置
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var contextProvider: ContextProvider

    @Inject
    lateinit var ttsInitializer: TTSInitializer

    @Inject
    lateinit var ttsEngine: AndroidTTSEngine

    /**
     * 活动创建 - 在主界面创建时调用
     * 设置Jetpack Compose内容和应用主题
     * 包含异常处理机制，确保应用在出错时能优雅降级
     *
     * @param savedInstanceState 保存的活动状态，用于恢复之前的界面状态
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置Activity Context供TTS引擎使用
        contextProvider.setActivityContext(this)

        // 处理打开文件的Intent
        handleFileIntent(intent)

        // 初始化TTS引擎（在协程中执行）
        Logger.d("MainActivity", "[VideoNote] 开始初始化TTS...")
        lifecycleScope.launch {
            val success = ttsInitializer.initialize()
            Logger.d("MainActivity", "[VideoNote] TTS初始化${if (success) "成功" else "失败"}")
        }

        try {
            setContent {
                VideoNoteTheme {
                    // 设置根Surface，应用主题背景色
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colors.background
                    ) {
                        // 设置主导航组件，管理应用内的页面跳转
                        VideoNoteNavigation()
                    }
                }
            }
        } catch (e: Exception) {
            // 记录异常但不让应用崩溃，提供降级UI
            try {
                // 显示一个简单的错误信息界面
                setContent {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colors.background
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "应用启动失败\n${e.message}",
                                style = MaterialTheme.typography.body1,
                                color = MaterialTheme.colors.error
                            )
                        }
                    }
                }
            } catch (e2: Exception) {
                // 如果连错误界面都无法显示，则关闭应用
                finish()
            }
        }
    }

    /**
     * 活动启动 - 在活动变为可见时调用
     * 在onCreate之后或从后台返回前台时调用
     */
    override fun onStart() {
        super.onStart()
    }

    /**
     * 处理新的Intent - 当Activity已经运行时收到新的Intent
     * 用于处理从外部打开文件的情况
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleFileIntent(intent)
    }

    /**
     * 处理文件打开Intent
     *
     * @param intent 收到的Intent
     */
    private fun handleFileIntent(intent: Intent?) {
        val hasPendingImport = IntentHandler.handleIntent(intent)
        if (hasPendingImport) {
            Logger.d(TAG, "[VideoNote] 检测到文件导入Intent")
        }
    }

    /**
     * 活动恢复 - 在活动开始与用户交互时调用
     * 此时活动位于活动栈顶部，用户可以进行交互
     */
    override fun onResume() {
        super.onResume()
    }

    /**
     * 活动暂停 - 在活动即将失去焦点时调用
     * 通常当用户切换到其他应用或弹出对话框时调用
     */
    override fun onPause() {
        super.onPause()
    }

    /**
     * 活动停止 - 在活动不再对用户可见时调用
     * 可能是因为活动被销毁或新的活动完全覆盖了当前活动
     */
    override fun onStop() {
        super.onStop()
    }

    /**
     * 活动销毁 - 在活动被系统销毁前调用
     * 执行最终的清理操作，释放所有资源
     * 注意：横屏等配置变更不会触发完全销毁，此时不应停止TTS播放
     */
    override fun onDestroy() {
        super.onDestroy()

        // 只有在Activity真正结束时（isFinishing=true）才停止TTS
        // 横屏等配置变更时isFinishing=false，不应停止播放
        if (isFinishing) {
            Logger.d(TAG, "[VideoNote] Activity正在结束，停止TTS播放")
            // 停止TTS播放（在协程中执行）
            lifecycleScope.launch {
                try {
                    ttsEngine.stop()
                } catch (e: Exception) {
                    Logger.e(TAG, "[VideoNote] 停止TTS失败: ${e.message}")
                }
            }
            // 清理Activity Context引用
            contextProvider.clearActivityContext()
            // 释放TTS引擎资源（同步操作）
            ttsEngine.shutdown()
        } else {
            Logger.d(TAG, "[VideoNote] Activity配置变更（如横屏），保持TTS播放状态")
            // 配置变更时，仍然需要清理Activity Context引用
            // 因为新的Activity实例会设置新的Context
            contextProvider.clearActivityContext()
        }
    }

    /**
     * 配置变更回调 - 处理屏幕方向等配置变更
     * 由于在AndroidManifest中配置了configChanges，此方法在横竖屏切换时被调用
     * 而不是重新创建Activity，这样可以保持TTS播放状态
     */
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        Logger.d(TAG, "[VideoNote] 配置变更: 方向=${if (newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) "横屏" else "竖屏"}")
        // 重新设置Activity Context，因为可能在onDestroy中被清除了
        contextProvider.setActivityContext(this)
    }

    companion object {
        /**
         * 日志标签 - 用于主活动级别的日志输出
         * 便于在日志中追踪主活动的生命周期和操作
         */
        private const val TAG = "VideoNote.MainActivity"
    }
}