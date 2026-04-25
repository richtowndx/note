package com.videonote.util

import android.util.Log
import com.videonote.BuildConfig

/**
 * 统一日志工具类
 *
 * 在debug模式下正常输出日志，在release模式下不输出日志（空操作）
 */
object Logger {

    /**
     * 输出DEBUG级别日志
     *
     * @param tag 日志标签
     * @param message 日志消息
     */
    @JvmStatic
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    /**
     * 输出DEBUG级别日志（支持懒加载消息）
     *
     * @param tag 日志标签
     * @param message 懒加载的日志消息（仅在debug模式下才会执行）
     */
    @JvmStatic
    fun d(tag: String, message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message())
        }
    }

    /**
     * 输出INFO级别日志
     *
     * @param tag 日志标签
     * @param message 日志消息
     */
    @JvmStatic
    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message)
        }
    }

    /**
     * 输出INFO级别日志（支持懒加载消息）
     *
     * @param tag 日志标签
     * @param message 懒加载的日志消息（仅在debug模式下才会执行）
     */
    @JvmStatic
    fun i(tag: String, message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message())
        }
    }

    /**
     * 输出WARN级别日志
     *
     * @param tag 日志标签
     * @param message 日志消息
     */
    @JvmStatic
    fun w(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, message)
        }
    }

    /**
     * 输出WARN级别日志（支持懒加载消息）
     *
     * @param tag 日志标签
     * @param message 懒加载的日志消息（仅在debug模式下才会执行）
     */
    @JvmStatic
    fun w(tag: String, message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, message())
        }
    }

    /**
     * 输出WARN级别日志（带异常）
     *
     * @param tag 日志标签
     * @param message 日志消息
     * @param throwable 异常对象
     */
    @JvmStatic
    fun w(tag: String, message: String, throwable: Throwable?) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, message, throwable)
        }
    }

    /**
     * 输出ERROR级别日志
     *
     * @param tag 日志标签
     * @param message 日志消息
     */
    @JvmStatic
    fun e(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message)
        }
    }

    /**
     * 输出ERROR级别日志（支持懒加载消息）
     *
     * @param tag 日志标签
     * @param message 懒加载的日志消息（仅在debug模式下才会执行）
     */
    @JvmStatic
    fun e(tag: String, message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message())
        }
    }

    /**
     * 输出ERROR级别日志（带异常）
     *
     * @param tag 日志标签
     * @param message 日志消息
     * @param throwable 异常对象
     */
    @JvmStatic
    fun e(tag: String, message: String, throwable: Throwable?) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message, throwable)
        }
    }

    /**
     * 输出ERROR级别日志（仅异常）
     *
     * @param tag 日志标签
     * @param throwable 异常对象
     */
    @JvmStatic
    fun e(tag: String, throwable: Throwable?) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, "", throwable)
        }
    }

    /**
     * 输出VERBOSE级别日志
     *
     * @param tag 日志标签
     * @param message 日志消息
     */
    @JvmStatic
    fun v(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, message)
        }
    }

    /**
     * 输出VERBOSE级别日志（支持懒加载消息）
     *
     * @param tag 日志标签
     * @param message 懒加载的日志消息（仅在debug模式下才会执行）
     */
    @JvmStatic
    fun v(tag: String, message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, message())
        }
    }

    /**
     * 检查是否启用日志
     *
     * @return true表示启用日志，false表示禁用日志
     */
    @JvmStatic
    fun isLogEnabled(): Boolean = BuildConfig.DEBUG
}
