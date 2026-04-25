package com.videonote

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * VideoNote应用程序类 - 应用的入口点和全局配置
 * 使用Hilt进行依赖注入，提供整个应用的基础设施支持
 * 继承自Application类，管理应用级别的初始化和资源
 */
@HiltAndroidApp
class VideoNoteApplication : Application() {

    /**
     * 应用程序创建 - 在应用启动时调用
     * 执行应用级别的初始化操作，如设置依赖注入、配置全局参数等
     * Hilt会在此方法中设置依赖注入容器
     */
    override fun onCreate() {
        super.onCreate()
    }

    /**
     * 应用程序终止 - 在应用进程终止前调用
     * 执行清理操作，释放全局资源
     * 注意：在实际应用中，此方法不一定会被调用，因为系统可能会直接杀死进程
     */
    override fun onTerminate() {
        super.onTerminate()
    }
}